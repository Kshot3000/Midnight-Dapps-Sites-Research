package tech.kshot.midnightprover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** One failed job, kept as a kind so each failure class gets its own message. */
data class FailureInfo(val kind: String, val title: String, val message: String) {
    companion object {
        fun from(e: Throwable): FailureInfo = when (e) {
            is ProveException -> FailureInfo(e.javaClass.simpleName, e.title, e.message ?: "")
            else -> FailureInfo("Unexpected", "Something went wrong", e.message ?: e.javaClass.simpleName)
        }
    }
}

enum class HealthKind { UNKNOWN, CHECKING, ONLINE, BUSY, NOT_READY, WRONG_VERSION, OFFLINE, ERROR }

/** What the server card shows, derived purely from a [ServerStatus] (P0-1: busy is not unreachable). */
data class ServerHealth(val kind: HealthKind = HealthKind.UNKNOWN, val status: ServerStatus? = null) {
    val headline: String get() = when (kind) {
        HealthKind.UNKNOWN -> "Proof server not checked yet"
        HealthKind.CHECKING -> Copy.STATUS_CHECKING
        HealthKind.ONLINE -> Copy.STATUS_ONLINE
        HealthKind.BUSY -> "Proof server busy"
        HealthKind.NOT_READY -> "Proof server not ready"
        HealthKind.WRONG_VERSION -> "Unexpected server version"
        HealthKind.OFFLINE -> "Proof server unreachable"
        HealthKind.ERROR -> status?.failure?.title ?: "Server error"
    }

    companion object {
        fun from(s: ServerStatus): ServerHealth = ServerHealth(
            kind = when {
                !s.reachable -> HealthKind.OFFLINE
                s.failure != null -> HealthKind.ERROR
                s.readyStatus == "busy" -> HealthKind.BUSY
                !s.ready -> HealthKind.NOT_READY
                !s.versionMatches -> HealthKind.WRONG_VERSION
                else -> HealthKind.ONLINE
            },
            status = s
        )
    }
}

data class ProverUiState(
    /** The saved, validated server URL the app actually uses. */
    val baseUrl: String = ProofJobInput.DEFAULT_BASE_URL,
    /** What the user is typing in the URL field; applied only on Save (P0-3). */
    val urlDraft: String = ProofJobInput.DEFAULT_BASE_URL,
    val allowOffPhone: Boolean = false,
    val health: ServerHealth = ServerHealth(),
    val txHex: String = "",
    val jobState: JobState = JobState.IDLE,
    /** The payload captured when the job started (the field is locked while proving). */
    val jobPayloadHex: String = "",
    val elapsedSeconds: Double = 0.0,
    val proofHex: String = "",
    val proofSummary: String = "",
    val warning: String = "",
    val inputError: String = "",
    val failure: FailureInfo? = null,
    val history: List<JobRecord> = emptyList()
) {
    val isProving: Boolean get() = jobState == JobState.PROVING
    val urlError: String? get() = ProofJobInput.baseUrlError(urlDraft, allowOffPhone)
    val urlDirty: Boolean get() = ProofJobInput.normaliseBaseUrl(urlDraft) != baseUrl
    val offPhone: Boolean get() = !ProofJobInput.isLocalhost(baseUrl)
}

/**
 * Drives the prover screens. The server URL, the off-phone opt-in and the
 * job history persist in SharedPreferences across launches. Cancelling a
 * prove drops the HTTP connection at once.
 */
class ProverViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("prover", Application.MODE_PRIVATE)
    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<ProverUiState> = _state.asStateFlow()

    private var proveJob: Job? = null
    private var checkJob: Job? = null
    /** Bumped per job, so a cancelled job winding down can't overwrite a newer one. */
    private var generation = 0
    private var jobStartedAt = 0L

    private fun loadInitialState(): ProverUiState {
        val allowOffPhone = prefs.getBoolean("allowOffPhone", false)
        val stored = prefs.getString("baseUrl", null) ?: ProofJobInput.DEFAULT_BASE_URL
        // P0-5: a saved off-phone URL is only honoured while the opt-in is on and it is valid.
        val url = if (ProofJobInput.baseUrlError(stored, allowOffPhone) == null) ProofJobInput.normaliseBaseUrl(stored)
        else ProofJobInput.DEFAULT_BASE_URL
        val history = mutableListOf<JobRecord>()
        runCatching {
            val arr = JSONArray(prefs.getString("history", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                history += JobRecord(
                    finishedAtMillis = o.optLong("at"),
                    serverUrl = o.optString("server"),
                    txPreview = o.optString("tx"),
                    success = o.optBoolean("ok"),
                    durationSeconds = o.optDouble("secs"),
                    detail = o.optString("detail"),
                    circuit = o.optString("circuit"),
                    cancelled = o.optBoolean("cancelled")
                )
            }
        }
        return ProverUiState(baseUrl = url, urlDraft = url, allowOffPhone = allowOffPhone, history = history)
    }

    private fun persist() {
        val arr = JSONArray()
        _state.value.history.take(MAX_HISTORY).forEach { r ->
            arr.put(JSONObject().apply {
                put("at", r.finishedAtMillis); put("server", r.serverUrl)
                put("tx", r.txPreview); put("ok", r.success)
                put("secs", r.durationSeconds); put("detail", r.detail)
                put("circuit", r.circuit); put("cancelled", r.cancelled)
            })
        }
        prefs.edit()
            .putString("baseUrl", _state.value.baseUrl)
            .putBoolean("allowOffPhone", _state.value.allowOffPhone)
            .putString("history", arr.toString())
            .apply()
    }

    private fun record(success: Boolean, cancelled: Boolean, startedAt: Long, payloadHex: String, detail: String) {
        val now = System.currentTimeMillis()
        val record = JobRecord(
            finishedAtMillis = now,
            serverUrl = _state.value.baseUrl,
            txPreview = JobHistoryFormat.summariseTx(payloadHex),
            success = success,
            durationSeconds = (now - startedAt) / 1000.0,
            detail = detail,
            circuit = PayloadInspector.inspectHex(payloadHex)?.circuitName.orEmpty(),
            cancelled = cancelled
        )
        _state.update { it.copy(history = (listOf(record) + it.history).take(MAX_HISTORY)) }
        persist()
    }

    private fun client() = ProofServerClient(_state.value.baseUrl)

    // ---- Server URL (P0-3, P0-5) ----

    /** Keystrokes only update the draft: nothing is normalised, saved or cancelled here. */
    fun onUrlDraftChange(raw: String) {
        if (_state.value.isProving) return
        _state.update { it.copy(urlDraft = raw) }
    }

    /** Validates, normalises and saves the draft, then checks the server. False if refused. */
    fun saveUrl(): Boolean {
        val s = _state.value
        if (s.isProving || s.urlError != null) return false
        val url = ProofJobInput.normaliseBaseUrl(s.urlDraft)
        _state.update { it.copy(baseUrl = url, urlDraft = url, health = ServerHealth()) }
        persist()
        checkServer()
        return true
    }

    fun useThisPhone() {
        if (_state.value.isProving) return
        _state.update { it.copy(urlDraft = ProofJobInput.DEFAULT_BASE_URL) }
        saveUrl()
    }

    /** Off-phone servers are opt-in; turning the opt-in off moves the app back to this phone. */
    fun setAllowOffPhone(allow: Boolean) {
        if (_state.value.isProving) return
        _state.update { it.copy(allowOffPhone = allow) }
        if (!allow && _state.value.offPhone) {
            _state.update { it.copy(baseUrl = ProofJobInput.DEFAULT_BASE_URL, urlDraft = ProofJobInput.DEFAULT_BASE_URL, health = ServerHealth()) }
        }
        persist()
    }

    // ---- Payload ----

    fun onTxHexChange(hex: String) {
        if (_state.value.isProving) return // P2-3: the payload is locked while proving
        _state.update { it.copy(txHex = hex, inputError = "") }
    }

    // ---- Server status ----

    fun checkServer() {
        if (_state.value.isProving) return
        checkJob?.cancel()
        val c = client()
        checkJob = viewModelScope.launch {
            _state.update { it.copy(health = ServerHealth(HealthKind.CHECKING, it.health.status)) }
            val status = c.status()
            _state.update { it.copy(health = ServerHealth.from(status)) }
        }
    }

    // ---- Proving ----

    /**
     * P0-2: Cancel answers at once, whatever stage the job is in (pre-flight
     * check or prove). The UI moves to CANCELLED immediately and the HTTP
     * connection is dropped; the proof server itself may still finish the job.
     */
    fun cancelProve() {
        val s = _state.value
        if (!s.isProving) return
        generation++
        proveJob?.cancel()
        proveJob = null
        _state.update {
            it.copy(
                jobState = JobState.CANCELLED,
                elapsedSeconds = (System.currentTimeMillis() - jobStartedAt) / 1000.0,
                failure = FailureInfo("Cancelled", "Stopped waiting", Copy.ERR_CANCELLED),
                warning = ""
            )
        }
        record(false, true, jobStartedAt, s.jobPayloadHex, "Stopped waiting")
    }

    fun dismissResult() {
        if (_state.value.isProving) return
        _state.update { it.copy(jobState = JobState.IDLE, failure = null, proofHex = "", proofSummary = "", warning = "") }
    }

    fun prove() {
        val s = _state.value
        if (s.isProving) return
        val validation = ProofJobInput.validationError(s.txHex)
        if (validation != null) {
            _state.update { it.copy(inputError = validation) }
            return
        }
        val payloadHex = ProofJobInput.cleanHex(s.txHex)
        val c = client()
        val startedAt = System.currentTimeMillis()
        val gen = ++generation
        jobStartedAt = startedAt
        _state.update {
            it.copy(
                jobState = JobState.PROVING, jobPayloadHex = payloadHex, failure = null, warning = "", inputError = "",
                proofHex = "", proofSummary = "", elapsedSeconds = 0.0
            )
        }
        proveJob = viewModelScope.launch {
            val clock = launch {
                while (isActive) {
                    _state.update { it.copy(elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000.0) }
                    delay(200)
                }
            }
            // P0-2: everything, including the pre-flight check, is inside try, so a
            // cancel at any point lands in the CancellationException branch.
            try {
                val pre = c.status()
                if (gen != generation) return@launch
                if (pre.reachable && pre.queueFull) _state.update { it.copy(warning = Copy.QUEUE_FULL_PRECHECK) }
                val proof = c.prove(HexCodec.hexToBytes(payloadHex))
                if (gen != generation) return@launch
                val summary = ProofFormat.summarise(proof)
                _state.update {
                    it.copy(
                        jobState = JobState.DONE,
                        elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000.0,
                        proofSummary = summary,
                        proofHex = HexCodec.bytesToHex(proof)
                    )
                }
                record(true, false, startedAt, payloadHex, summary)
            } catch (e: CancellationException) {
                throw e // cancelProve() already moved the UI to CANCELLED and recorded it
            } catch (e: Exception) {
                if (gen != generation) return@launch
                val f = FailureInfo.from(e)
                _state.update {
                    it.copy(
                        jobState = JobState.FAILED,
                        elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000.0,
                        failure = f
                    )
                }
                record(false, false, startedAt, payloadHex, f.title)
            } finally {
                clock.cancel()
            }
        }
    }

    fun clearHistory() {
        _state.update { it.copy(history = emptyList()) }
        persist()
    }

    companion object {
        const val MAX_HISTORY = 50
    }
}
