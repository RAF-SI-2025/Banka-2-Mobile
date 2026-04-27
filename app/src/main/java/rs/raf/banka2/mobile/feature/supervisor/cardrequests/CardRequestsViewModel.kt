package rs.raf.banka2.mobile.feature.supervisor.cardrequests

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
import rs.raf.banka2.mobile.data.dto.card.CardRequestResponseDto
import rs.raf.banka2.mobile.data.repository.CardRepository
import javax.inject.Inject

@HiltViewModel
class CardRequestsViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CardRequestsState())
    val state: StateFlow<CardRequestsState> = _state.asStateFlow()

    private val _events = Channel<CardRequestsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun setStatusFilter(status: String?) {
        _state.update { it.copy(statusFilter = status) }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.listAllRequests(_state.value.statusFilter)) {
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
                _events.send(CardRequestsEvent.Toast("Zahtev odobren."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun reject(id: Long, reason: String) = viewModelScope.launch {
        when (val result = repository.rejectRequest(id, reason)) {
            is ApiResult.Success -> {
                _events.send(CardRequestsEvent.Toast("Zahtev odbijen."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class CardRequestsState(
    val loading: Boolean = false,
    val requests: List<CardRequestResponseDto> = emptyList(),
    val statusFilter: String? = null,
    val error: String? = null
)

sealed interface CardRequestsEvent {
    data class Toast(val message: String) : CardRequestsEvent
}
