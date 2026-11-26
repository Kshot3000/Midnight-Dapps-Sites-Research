package tech.kshot.midnightprover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HealthKind { UNKNOWN, CHECKING, ONLINE, BUSY, NOT_READY, WRONG_VERSION, TIMEOUT, UNENCRYPTED, OFFLINE, ERROR }

/** What the server card shows. Derived purely from a [ServerStatus]. */
data class ServerHealth(
    val kind: HealthKind = HealthKind.UNKNOWN,
    val status: ServerStatus? = null,
    val attempt: Int = 0,
    val maxAttempts: Int = 0
) {
    val headline: String get() = when (kind) {
        HealthKind.UNKNOWN -> "Not checked yet"
        HealthKind.CHECKING -> if (attempt <= 1) "Checking proof server…" else "Retrying ($attempt of $maxAttempts)…"
        HealthKind.ONLINE -> "Proof server online"
        HealthKind.BUSY -> "Proof server busy"
        HealthKind.NOT_READY -> "Server not ready"
        HealthKind.WRONG_VERSION -> "Unexpected server version"
        HealthKind.TIMEOUT -> "The proof server timed out"
        HealthKind.UNENCRYPTED -> "Server is not using https"
        HealthKind.OFFLINE -> "Proof server offline"
        HealthKind.ERROR -> status?.failure?.title ?: "Server error"
    }

    val canProve: Boolean get() = kind == HealthKind.ONLINE || kind == HealthKind.WRONG_VERSION ||
        kind == HealthKind.NOT_READY || kind == HealthKind.BUSY

    companion object {
        fun from(s: ServerStatus, baseUrl: String = ""): ServerHealth = ServerHealth(
            kind = when {
                s.failure is ProveException.Timeout -> HealthKind.TIMEOUT
                !s.reachable -> HealthKind.OFFLINE
                s.failure != null -> HealthKind.ERROR
                s.readyStatus == "busy" -> HealthKind.BUSY
                !s.ready -> HealthKind.NOT_READY
                !s.versionMatches -> HealthKind.WRONG_VERSION
                baseUrl.isNotEmpty() && !ProofJobInput.isLocalhost(baseUrl) &&
                    (runCatching { java.net.URI(baseUrl).scheme }.getOrNull()?.lowercase() != "https") ->
                    HealthKind.UNENCRYPTED
                else -> HealthKind.ONLINE
            },
            status = s
        )
    }
}

data class ProverUiState(
    val settings: ProverSettings = ProverSettings(),
    val health: ServerHealth = ServerHealth(),
    val draft: String = "",
    val urlDraft: String = ProofJobInput.DEFAULT_BASE_URL,
    val session: ProveSession = ProveSession(),
    val history: List<JobRecord> = emptyList(),
    val importMessage: String = ""
) {
    val draftError: String? get() = if (draft.isBlank()) null else ProofJobInput.validationError(draft)
    val draftInfo: PayloadInfo? get() = PayloadInspector.inspectHex(draft)
    val urlError: String? get() = ProofJobInput.baseUrlError(urlDraft, settings.allowOffPhone)
    val offPhone: Boolean get() = !ProofJobInput.isLocalhost(settings.baseUrl)
    val urlDirty: Boolean get() = ProofJobInput.normaliseBaseUrl(urlDraft) != settings.baseUrl
}

/**
 * Screen state. Proof jobs live in the app-scoped [ProveEngine]; this holds
 * the editor draft (in [SavedStateHandle], so it survives process death),
 * server health with retry/backoff, and settings.
 */
class ProverViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val graph = app as ProverApplication
    private val store = graph.store
    private val engine = graph.engine

    private val local = MutableStateFlow(
        LocalState(
            settings = store.loadSettings(),
            draft = saved.get<String>(KEY_DRAFT) ?: "",
            urlDraft = saved.get<String>(KEY_URL_DRAFT) ?: store.loadSettings().baseUrl
        )
    )

    val state: StateFlow<ProverUiState> = combine(local, engine.session, engine.history) { l, s, h ->
        ProverUiState(l.settings, l.health, l.draft, l.urlDraft, s, h, l.importMessage)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProverUiState())

    private var healthJob: Job? = null

    init {
        if (local.value.settings.onboardingDone) checkServer()
    }

    private data class LocalState(
        val settings: ProverSettings,
        val health: ServerHealth = ServerHealth(),
        val draft: String = "",
        val urlDraft: String = "",
        val importMessage: String = ""
    )

    private fun timeouts() = ClientTimeouts(proveReadMs = local.value.settings.proveTimeoutMinutes * 60_000)

    // ---- Draft / import ----
    fun onDraftChange(text: String) {
        if (engine.session.value.isRunning) return
        local.update { it.copy(draft = text, importMessage = "") }
        // Bundles are size-limited; very large payloads are kept only in memory/engine files.
        saved[KEY_DRAFT] = if (text.length <= MAX_SAVED_DRAFT) text else null
    }

    /** Imports bytes from a file/share: raw proving payloads become hex; hex text is kept. */
    fun importPayload(bytes: ByteArray, source: String) {
        val asText = runCatching { String(bytes, Charsets.US_ASCII) }.getOrDefault("")
        val hex = if (ProofJobInput.validationError(asText) == null) ProofJobInput.cleanHex(asText) else HexCodec.bytesToHex(bytes)
        onDraftChange(hex)
        val info = PayloadInspector.inspectHex(hex)
        local.update {
            it.copy(importMessage = when {
                info == null -> "Imported from $source, but it isn't a usable payload."
                info.headerOk -> "Imported ${JobHistoryFormat.formatBytes(info.sizeBytes)} from $source."
                else -> "Imported from $source — it doesn't start with a Midnight proving-payload header, so the server may reject it."
            })
        }
    }

    fun loadSample() {
        val bytes = getApplication<Application>().assets.open(SAMPLE_ASSET).use { it.readBytes() }
        importPayload(bytes, "the bundled sample")
        local.update { it.copy(importMessage = "Loaded the sample DUST-spend payload (captured on a local devnet, not testnet or mainnet). Proving it tests your server end to end.") }
    }

    fun clearDraft() = onDraftChange("")

    // ---- Proving ----
    /** Proves the draft; on a retry with an empty draft, re-proves the last job's payload. */
    fun prove() {
        val last = engine.session.value.payloadHex
        if (local.value.draft.isBlank() && last.isNotEmpty()) onDraftChange(last)
        val started = engine.start(local.value.draft, local.value.settings.baseUrl, timeouts())
        if (started) ProveService.start(getApplication())
    }

    fun cancelProve() = engine.cancel()
    fun dismissResult() = engine.dismiss()
    fun clearHistory() = engine.clearHistory()

    // ---- Server ----
    fun checkServer() {
        healthJob?.cancel()
        val client = ProofServerClient(local.value.settings.baseUrl, timeouts())
        healthJob = viewModelScope.launch {
            val policy = RetryPolicy()
            val status = client.statusWithRetry(policy) { attempt, _ ->
                local.update { it.copy(health = ServerHealth(HealthKind.CHECKING, it.health.status, attempt, policy.maxAttempts)) }
            }
            local.update { it.copy(health = ServerHealth.from(status, local.value.settings.baseUrl)) }
        }
    }

    fun onUrlDraftChange(text: String) {
        if (engine.session.value.isRunning) return
        local.update { it.copy(urlDraft = text) }
        saved[KEY_URL_DRAFT] = text
    }

    /** Commits the URL draft if valid. Returns false (and keeps the draft) when invalid. */
    fun saveUrl(): Boolean {
        val draft = local.value.urlDraft
        if (ProofJobInput.baseUrlError(draft, local.value.settings.allowOffPhone) != null) return false
        if (engine.session.value.isRunning) return false
        val url = ProofJobInput.normaliseBaseUrl(draft)
        updateSettings { it.copy(baseUrl = url) }
        local.update { it.copy(urlDraft = url, health = ServerHealth()) }
        saved[KEY_URL_DRAFT] = url
        checkServer()
        return true
    }

    fun resetUrl() {
        onUrlDraftChange(ProofJobInput.DEFAULT_BASE_URL)
        saveUrl()
    }

    // ---- Settings ----
    fun updateSettings(f: (ProverSettings) -> ProverSettings) {
        val next = f(local.value.settings)
        store.saveSettings(next)
        local.update { it.copy(settings = next) }
    }

    fun setProveTimeout(minutes: Int) = updateSettings { it.copy(proveTimeoutMinutes = minutes.coerceIn(1, 120)) }
    fun setReduceMotion(on: Boolean) = updateSettings { it.copy(reduceMotion = on) }
    fun setKeepScreenOn(on: Boolean) = updateSettings { it.copy(keepScreenOn = on) }

    /** P0-5 opt-in. Turning it off snaps the server back to this phone. */
    fun setAllowOffPhone(on: Boolean) {
        if (engine.session.value.isRunning) return
        updateSettings { it.copy(allowOffPhone = on) }
        if (!on && !ProofJobInput.isLocalhost(local.value.settings.baseUrl)) resetUrl()
    }

    /** One quiet status read (no "checking" flicker) for periodic refresh. */
    fun refreshQuietly() {
        if (healthJob?.isActive == true || engine.session.value.isRunning) return
        val client = ProofServerClient(local.value.settings.baseUrl, timeouts())
        healthJob = viewModelScope.launch {
            val status = client.status()
            local.update { it.copy(health = ServerHealth.from(status, local.value.settings.baseUrl)) }
        }
    }

    fun finishOnboarding() {
        updateSettings { it.copy(onboardingDone = true) }
        checkServer()
    }

    fun reopenOnboarding() = updateSettings { it.copy(onboardingDone = false) }

    companion object {
        const val KEY_DRAFT = "draft"
        const val KEY_URL_DRAFT = "urlDraft"
        const val MAX_SAVED_DRAFT = 200_000
        const val SAMPLE_ASSET = "sample-dust-spend-payload.hex"
    }
}
