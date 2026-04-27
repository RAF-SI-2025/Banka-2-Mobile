package rs.raf.banka2.mobile.feature.supervisor.loanrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationResponseDto
import rs.raf.banka2.mobile.data.repository.LoanRepository
import javax.inject.Inject

@HiltViewModel
class LoanRequestsViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoanRequestsState())
    val state: StateFlow<LoanRequestsState> = _state.asStateFlow()

    private val _events = Channel<LoanRequestsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.listAllRequests()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, requests = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun approve(id: Long) = viewModelScope.launch {
        when (val result = repository.approveRequest(id)) {
            is ApiResult.Success -> {
                _events.send(LoanRequestsEvent.Toast("Kredit odobren."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun reject(id: Long) = viewModelScope.launch {
        when (val result = repository.rejectRequest(id)) {
            is ApiResult.Success -> {
                _events.send(LoanRequestsEvent.Toast("Zahtev odbijen."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class LoanRequestsState(
    val loading: Boolean = false,
    val requests: List<LoanApplicationResponseDto> = emptyList(),
    val error: String? = null
)

sealed interface LoanRequestsEvent {
    data class Toast(val message: String) : LoanRequestsEvent
}
