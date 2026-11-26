package tech.kshot.midnightprover

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** A failure, flattened so it survives process death. */
data class FailureInfo(val kind: String, val title: String, val message: String) {
    companion object {
        fun from(e: Throwable): FailureInfo = when (e) {
            is ProveException -> FailureInfo(e.javaClass.simpleName, e.title, e.message ?: "")
            else -> FailureInfo("Unexpected", "Something went wrong", e.message ?: e.javaClass.simpleName)
        }
    }
}

/** The current (or most recent) proof job. */
data class ProveSession(
    val state: JobState = JobState.IDLE,
    val payloadHex: String = "",
    val serverUrl: String = "",
    val startedAtMillis: Long = 0,
    val finishedAtMillis: Long = 0,
    val proofHex: String = "",
    val failure: FailureInfo? = null
) {
    val info: PayloadInfo? by lazy { if (payloadHex.isEmpty()) null else PayloadInspector.inspectHex(payloadHex) }
    val proofBytes: Int get() = proofHex.length / 2
    val elapsedSeconds: Double get() =
        if (startedAtMillis == 0L) 0.0
        else ((if (finishedAtMillis > 0) finishedAtMillis else System.currentTimeMillis()) - startedAtMillis) / 1000.0
    val isRunning: Boolean get() = state == JobState.PROVING
}

/** User settings, persisted. */
data class ProverSettings(
    val baseUrl: String = ProofJobInput.DEFAULT_BASE_URL,
    val proveTimeoutMinutes: Int = DEFAULT_TIMEOUT_MINUTES,
    val reduceMotion: Boolean = false,
    val keepScreenOn: Boolean = true,
    val onboardingDone: Boolean = false,
    val allowOffPhone: Boolean = false,
    val privacyCloak: Boolean = false
) {
    companion object {
        /** Just above proof server 9.0.2's own 600 s job timeout. */
        const val DEFAULT_TIMEOUT_MINUTES = 11
    }
}

/** Persistence for settings, history and the live job. Files + SharedPreferences. */
class ProverStore(context: Context) {
    private val prefs = context.getSharedPreferences("prover", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "job").apply { mkdirs() }
    private val payloadFile = File(dir, "payload.hex")
    private val proofFile = File(dir, "proof.hex")

    fun loadSettings(): ProverSettings = ProverSettings(
        baseUrl = prefs.getString("baseUrl", null) ?: ProofJobInput.DEFAULT_BASE_URL,
        proveTimeoutMinutes = prefs.getInt("proveTimeoutMinutes", ProverSettings.DEFAULT_TIMEOUT_MINUTES),
        reduceMotion = prefs.getBoolean("reduceMotion", false),
        keepScreenOn = prefs.getBoolean("keepScreenOn", true),
        onboardingDone = prefs.getBoolean("onboardingDone", false),
        allowOffPhone = prefs.getBoolean("allowOffPhone", false),
        privacyCloak = prefs.getBoolean("privacyCloak", false)
    ).let { s ->
        // A stored off-phone URL is only honoured while the opt-in is on (P0-5).
        if (ProofJobInput.baseUrlError(s.baseUrl, s.allowOffPhone) != null) s.copy(baseUrl = ProofJobInput.DEFAULT_BASE_URL) else s
    }

    fun saveSettings(s: ProverSettings) {
        prefs.edit()
            .putString("baseUrl", s.baseUrl)
            .putInt("proveTimeoutMinutes", s.proveTimeoutMinutes)
            .putBoolean("reduceMotion", s.reduceMotion)
            .putBoolean("keepScreenOn", s.keepScreenOn)
            .putBoolean("onboardingDone", s.onboardingDone)
            .putBoolean("allowOffPhone", s.allowOffPhone)
            .putBoolean("privacyCloak", s.privacyCloak)
            .apply()
    }

    fun loadHistory(): List<JobRecord> = runCatching {
        val arr = JSONArray(prefs.getString("history", "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            JobRecord(
                finishedAtMillis = o.optLong("at"),
                serverUrl = o.optString("server"),
                txPreview = o.optString("tx"),
                success = o.optBoolean("ok"),
                durationSeconds = o.optDouble("secs", 0.0),
                detail = o.optString("detail"),
                circuit = o.optString("circuit"),
                cancelled = o.optBoolean("cancelled", false),
                interrupted = o.optBoolean("interrupted", false)
            )
        }
    }.getOrDefault(emptyList())

    fun saveHistory(history: List<JobRecord>) {
        val arr = JSONArray()
        history.take(MAX_HISTORY).forEach { r ->
            arr.put(JSONObject().apply {
                put("at", r.finishedAtMillis); put("server", r.serverUrl)
                put("tx", r.txPreview); put("ok", r.success)
                put("secs", r.durationSeconds); put("detail", r.detail); put("circuit", r.circuit); put("cancelled", r.cancelled); put("interrupted", r.interrupted)
            })
        }
        prefs.edit().putString("history", arr.toString()).apply()
    }

    /** Written synchronously (commit) so a kill right after start still finds the job. */
    @android.annotation.SuppressLint("ApplySharedPref") // commit(): the job must be on disk before Android can kill the process.
    fun saveSession(s: ProveSession) {
        if (s.payloadHex.isNotEmpty()) payloadFile.writeText(s.payloadHex) else payloadFile.delete()
        if (s.proofHex.isNotEmpty()) proofFile.writeText(s.proofHex) else proofFile.delete()
        val f = s.failure
        prefs.edit()
            .putString("job.state", s.state.name)
            .putString("job.server", s.serverUrl)
            .putLong("job.started", s.startedAtMillis)
            .putLong("job.finished", s.finishedAtMillis)
            .putString("job.failKind", f?.kind)
            .putString("job.failTitle", f?.title)
            .putString("job.failMessage", f?.message)
            .commit()
    }

    fun loadSession(): ProveSession {
        val state = runCatching { JobState.valueOf(prefs.getString("job.state", "IDLE")!!) }.getOrDefault(JobState.IDLE)
        val kind = prefs.getString("job.failKind", null)
        return ProveSession(
            state = state,
            payloadHex = if (payloadFile.exists()) payloadFile.readText() else "",
            serverUrl = prefs.getString("job.server", "") ?: "",
            startedAtMillis = prefs.getLong("job.started", 0),
            finishedAtMillis = prefs.getLong("job.finished", 0),
            proofHex = if (proofFile.exists()) proofFile.readText() else "",
            failure = kind?.let {
                FailureInfo(it, prefs.getString("job.failTitle", "") ?: "", prefs.getString("job.failMessage", "") ?: "")
            }
        )
    }

    companion object { const val MAX_HISTORY = 50 }
}

/**
 * Runs proof jobs on an application-scoped coroutine, independent of any
 * screen, so rotating, folding/unfolding or leaving the screen never drops
 * a prove. [ProveService] keeps the process in the foreground while a job
 * runs. Every state change is persisted; if the system kills the process
 * mid-prove, the next launch finds the job marked INTERRUPTED with its
 * payload intact, ready to retry with one tap.
 */
class ProveEngine(
    private val store: ProverStore,
    private val clientFactory: (String, ClientTimeouts) -> ProofServerClient = { url, t -> ProofServerClient(url, t) },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _history = MutableStateFlow(store.loadHistory())
    val history: StateFlow<List<JobRecord>> = _history.asStateFlow()

    private val _session = MutableStateFlow(recover(store.loadSession()))
    val session: StateFlow<ProveSession> = _session.asStateFlow()

    @Volatile private var job: Job? = null
    @Volatile private var client: ProofServerClient? = null

    private fun recover(s: ProveSession): ProveSession =
        if (s.state == JobState.PROVING) {
            val r = s.copy(
                state = JobState.INTERRUPTED,
                finishedAtMillis = System.currentTimeMillis(),
                failure = FailureInfo(
                    "Interrupted", "Proof interrupted", Copy.ERR_INTERRUPTED
                )
            )
            recordHistory(r, success = false, detail = r.failure?.title ?: "Proof interrupted")
            store.saveSession(r)
            r
        } else s

    /** Persist first, then publish: an observer never sees a state the disk doesn't have yet. */
    private fun set(s: ProveSession) {
        store.saveSession(s)
        _session.value = s
    }

    /** Starts proving [payloadHex] against [baseUrl]. Ignored if a job is already running. */
    fun start(payloadHex: String, baseUrl: String, timeouts: ClientTimeouts): Boolean {
        if (job?.isActive == true) return false
        val err = ProofJobInput.validationError(payloadHex)
        val clean = ProofJobInput.cleanHex(payloadHex)
        val startedAt = System.currentTimeMillis()
        if (err != null) {
            set(ProveSession(JobState.FAILED, clean, baseUrl, startedAt, startedAt,
                failure = FailureInfo("Invalid", "Payload isn't valid", err)))
            return false
        }
        val c = clientFactory(baseUrl, timeouts)
        client = c
        set(ProveSession(JobState.PROVING, clean, baseUrl, startedAt))
        job = scope.launch {
            val result = runCatching { c.prove(HexCodec.hexToBytes(clean)) }
            val now = System.currentTimeMillis()
            val current = _session.value
            if (current.state != JobState.PROVING) return@launch // cancelled meanwhile
            result.fold(
                onSuccess = { proof ->
                    finish(current.copy(state = JobState.DONE, finishedAtMillis = now, proofHex = HexCodec.bytesToHex(proof)),
                        true, ProofFormat.summarise(proof))
                },
                onFailure = { e ->
                    if (e is CancellationException) return@fold
                    finish(current.copy(state = JobState.FAILED, finishedAtMillis = now, failure = FailureInfo.from(e)),
                        false, FailureInfo.from(e).title)
                }
            )
        }
        return true
    }

    fun cancel() {
        val current = _session.value
        if (current.state != JobState.PROVING) return
        client?.cancelProve()
        job?.cancel()
        finish(current.copy(
            state = JobState.CANCELLED, finishedAtMillis = System.currentTimeMillis(),
            failure = FailureInfo("Cancelled", "Stopped waiting", Copy.ERR_CANCELLED)
        ), false, "Stopped waiting", cancelled = true)
    }

    /** Clears the result/failure but keeps nothing running. */
    fun dismiss() {
        if (_session.value.state == JobState.PROVING) return
        set(ProveSession())
    }

    fun clearHistory() {
        _history.value = emptyList()
        store.saveHistory(emptyList())
    }

    /** Records history first, then publishes the terminal state, so observers see both together. */
    @Synchronized
    private fun finish(s: ProveSession, success: Boolean, detail: String, cancelled: Boolean = false) {
        if (_session.value.state != JobState.PROVING) return // a cancel and a completion raced; first one wins
        recordHistory(s, success, detail, cancelled)
        set(s)
    }

    private fun recordHistory(s: ProveSession, success: Boolean, detail: String, cancelled: Boolean = false) {
        val rec = JobRecord(
            finishedAtMillis = s.finishedAtMillis,
            serverUrl = s.serverUrl,
            txPreview = JobHistoryFormat.summariseTx(s.payloadHex),
            success = success,
            durationSeconds = s.elapsedSeconds,
            detail = detail.take(160),
            circuit = s.info?.keyLocation ?: "",
            cancelled = cancelled,
            interrupted = s.state == JobState.INTERRUPTED
        )
        _history.update { (listOf(rec) + it).take(ProverStore.MAX_HISTORY) }
        store.saveHistory(_history.value)
    }
}
