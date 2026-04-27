package rs.raf.banka2.mobile.feature.supervisor.accountrequests

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
import rs.raf.banka2.mobile.data.dto.account.AccountRequestResponseDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class AccountRequestsViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountRequestsState())
    val state: StateFlow<AccountRequestsState> = _state.asStateFlow()

    private val _events = Channel<AccountRequestsEvent>(Channel.BUFFERED)
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
        when (val result = repository.approveAccountRequest(id)) {
            is ApiResult.Success -> {
                _events.send(AccountRequestsEvent.Toast("Zahtev odobren."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun reject(id: Long, reason: String) = viewModelScope.launch {
        when (val result = repository.rejectAccountRequest(id, reason)) {
            is ApiResult.Success -> {
                _events.send(AccountRequestsEvent.Toast("Zahtev odbijen."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class AccountRequestsState(
    val loading: Boolean = false,
    val requests: List<AccountRequestResponseDto> = emptyList(),
    val error: String? = null
)

sealed interface AccountRequestsEvent {
    data class Toast(val message: String) : AccountRequestsEvent
}
