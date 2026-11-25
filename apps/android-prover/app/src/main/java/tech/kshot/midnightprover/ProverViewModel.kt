package tech.kshot.midnightprover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProverUiState(
    val baseUrl: String = ProofJobInput.DEFAULT_BASE_URL,
    val server: ServerStatus? = null,
    val txHex: String = "",
    val jobState: JobState = JobState.IDLE,
    val result: String = "",
    val error: String = ""
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
        _state.value = current.copy(jobState = JobState.PROVING, error = "", result = "")
        viewModelScope.launch {
            try {
                val response = ProofServerClient(url).proveTx(current.txHex)
                _state.value = _state.value.copy(
                    jobState = JobState.DONE,
                    result = response.take(4_000)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    jobState = JobState.FAILED,
                    error = "Proving failed: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }
}
