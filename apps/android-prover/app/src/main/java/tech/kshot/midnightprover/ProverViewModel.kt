package tech.kshot.midnightprover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProverUiState(
    val baseUrl: String = ProofJobInput.DEFAULT_BASE_URL,
    val server: ServerStatus? = null,
    val txHex: String = "",
    val jobState: JobState = JobState.IDLE,
    val result: String = "",
    val error: String = "",
    val elapsedSeconds: Double = 0.0,
    val history: List<JobRecord> = emptyList()
)

class ProverViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProverUiState())
    val state: StateFlow<ProverUiState> = _state

    fun onBaseUrlChange(value: String) {
        _state.value = _state.value.copy(baseUrl = value, server = null)
    }

    fun onTxHexChange(value: String) {
        _state.value = _state.value.copy(txHex = value, error = "")
    }

    fun checkServer() {
        val url = ProofJobInput.normaliseBaseUrl(_state.value.baseUrl)
        _state.value = _state.value.copy(jobState = JobState.CHECKING, error = "")
        viewModelScope.launch {
            val status = ProofServerClient(url).status()
            _state.value = _state.value.copy(
                server = status,
                jobState = JobState.IDLE,
                error = if (status.reachable) "" else "Server unreachable: ${status.detail}"
            )
        }
    }

    fun prove() {
        val current = _state.value
        val problem = ProofJobInput.validationError(current.txHex)
        if (problem != null) {
            _state.value = current.copy(error = problem)
            return
        }
        val url = ProofJobInput.normaliseBaseUrl(current.baseUrl)
        val cleaned = ProofJobInput.cleanHex(current.txHex)
        val startedAt = System.currentTimeMillis()
        _state.value = current.copy(
            jobState = JobState.PROVING, error = "", result = "", elapsedSeconds = 0.0
        )
        // Elapsed clock while the proof runs.
        viewModelScope.launch {
            while (_state.value.jobState == JobState.PROVING) {
                delay(200)
                if (_state.value.jobState == JobState.PROVING) {
                    _state.value = _state.value.copy(
                        elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000.0
                    )
                }
            }
        }
        viewModelScope.launch {
            try {
                val response = ProofServerClient(url).proveTx(current.txHex)
                val duration = (System.currentTimeMillis() - startedAt) / 1000.0
                _state.value = _state.value.copy(
                    jobState = JobState.DONE,
                    result = response.take(4_000),
                    elapsedSeconds = duration,
                    history = listOf(
                        JobRecord(
                            finishedAtMillis = System.currentTimeMillis(),
                            serverUrl = url,
                            txPreview = JobHistoryFormat.summariseTx(cleaned),
                            success = true,
                            durationSeconds = duration,
                            detail = "Proven by the server; response in the result panel."
                        )
                    ) + _state.value.history
                )
            } catch (e: Exception) {
                val duration = (System.currentTimeMillis() - startedAt) / 1000.0
                val message = e.message ?: e.javaClass.simpleName
                _state.value = _state.value.copy(
                    jobState = JobState.FAILED,
                    error = "Proving failed: $message",
                    elapsedSeconds = duration,
                    history = listOf(
                        JobRecord(
                            finishedAtMillis = System.currentTimeMillis(),
                            serverUrl = url,
                            txPreview = JobHistoryFormat.summariseTx(cleaned),
                            success = false,
                            durationSeconds = duration,
                            detail = message.take(200)
                        )
                    ) + _state.value.history
                )
            }
        }
    }
}
