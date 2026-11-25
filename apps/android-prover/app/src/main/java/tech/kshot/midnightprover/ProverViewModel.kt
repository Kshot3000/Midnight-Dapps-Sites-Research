package tech.kshot.midnightprover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

data class ProverUiState(
    val baseUrl: String = ProofJobInput.DEFAULT_BASE_URL,
    val server: ServerStatus? = null,
    val txHex: String = "",
    val jobState: JobState = JobState.IDLE,
    val elapsedSeconds: Double = 0.0,
    val result: String = "",
    val proofHex: String = "",
    val proofSummary: String = "",
    val warning: String = "",
    val error: String = "",
    val history: List<JobRecord> = emptyList()
)

/**
 * Drives the prover screens. The server URL and the job history persist in
 * SharedPreferences across launches; in-flight proves can be cancelled
 * (the HTTP connection is dropped, not just the coroutine).
 */
class ProverViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("prover", Application.MODE_PRIVATE)
    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<ProverUiState> = _state.asStateFlow()

    private var client = ProofServerClient(_state.value.baseUrl)
    private var proveJob: Job? = null

    private fun loadInitialState(): ProverUiState {
        val url = prefs.getString("baseUrl", null) ?: ProofJobInput.DEFAULT_BASE_URL
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
                    detail = o.optString("detail")
                )
            }
        }
        return ProverUiState(baseUrl = url, history = history)
    }

    private fun persist() {
        val arr = JSONArray()
        _state.value.history.take(50).forEach { r ->
            arr.put(JSONObject().apply {
                put("at", r.finishedAtMillis); put("server", r.serverUrl)
                put("tx", r.txPreview); put("ok", r.success)
                put("secs", r.durationSeconds); put("detail", r.detail)
            })
        }
        prefs.edit()
            .putString("baseUrl", _state.value.baseUrl)
            .putString("history", arr.toString())
            .apply()
    }

    private fun record(success: Boolean, startedAt: Long, detail: String) {
        val secs = (System.currentTimeMillis() - startedAt) / 1000.0
        val record = JobRecord(
            finishedAtMillis = System.currentTimeMillis(),
            serverUrl = _state.value.baseUrl,
            txPreview = JobHistoryFormat.summariseTx(ProofJobInput.cleanHex(_state.value.txHex)),
            success = success,
            durationSeconds = secs,
            detail = detail
        )
        _state.update { it.copy(history = (listOf(record) + it.history).take(50)) }
        persist()
    }

    fun onBaseUrlChange(raw: String) {
        val url = ProofJobInput.normaliseBaseUrl(raw)
        client.cancelActive()
        client = ProofServerClient(url)
        _state.update { it.copy(baseUrl = url, server = null) }
        persist()
    }

    fun onTxHexChange(hex: String) {
        _state.update { it.copy(txHex = hex) }
    }

    fun checkServer() {
        viewModelScope.launch {
            _state.update { it.copy(jobState = JobState.CHECKING, error = "") }
            val status = client.status()
            _state.update {
                it.copy(
                    server = status,
                    jobState = JobState.IDLE,
                    error = if (status.reachable) "" else "Server unreachable: ${status.detail}"
                )
            }
        }
    }

    fun cancelProve() {
        proveJob?.cancel()
        client.cancelActive()
    }

    fun prove() {
        val validation = ProofJobInput.validationError(_state.value.txHex)
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            return
        }
        proveJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            _state.update {
                it.copy(
                    jobState = JobState.PROVING, error = "", warning = "",
                    result = "", proofHex = "", proofSummary = "", elapsedSeconds = 0.0
                )
            }
            // Pre-flight: if the queue is already full, say so before waiting.
            val pre = client.status()
            if (pre.reachable && pre.jobCapacity != null &&
                pre.jobsProcessing + pre.jobsPending >= pre.jobCapacity
            ) {
                _state.update { it.copy(warning = "Server queue looks full — the request may be refused with 429.") }
            }
            val clock = launch {
                while (isActive) {
                    _state.update {
                        it.copy(elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000.0)
                    }
                    delay(200)
                }
            }
            try {
                val proof = client.prove(_state.value.txHex)
                clock.cancel()
                val secs = (System.currentTimeMillis() - startedAt) / 1000.0
                _state.update {
                    it.copy(
                        jobState = JobState.DONE,
                        elapsedSeconds = secs,
                        proofSummary = ProofFormat.summarise(proof),
                        proofHex = HexCodec.bytesToHex(proof),
                        result = "The server returned a proof. Copy it back to the wallet or tool that issued the payload."
                    )
                }
                record(true, startedAt, ProofFormat.summarise(proof))
            } catch (e: Exception) {
                clock.cancel()
                if (proveJob?.isCancelled == true) {
                    _state.update {
                        it.copy(jobState = JobState.IDLE, error = "Proof cancelled.")
                    }
                    record(false, startedAt, "Cancelled by user")
                } else {
                    val message = e.message ?: e.javaClass.simpleName
                    _state.update {
                        it.copy(
                            jobState = JobState.FAILED,
                            elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000.0,
                            error = message
                        )
                    }
                    record(false, startedAt, message.take(120))
                }
            }
        }
    }
}
