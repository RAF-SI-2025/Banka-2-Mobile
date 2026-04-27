package rs.raf.banka2.mobile.feature.orders.supervisor

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
import rs.raf.banka2.mobile.data.dto.order.OrderDto
import rs.raf.banka2.mobile.data.repository.OrderRepository
import javax.inject.Inject

@HiltViewModel
class OrdersSupervisorViewModel @Inject constructor(
    private val repository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersSupervisorState())
    val state: StateFlow<OrdersSupervisorState> = _state.asStateFlow()

    private val _events = Channel<OrdersSupervisorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun setStatusFilter(status: String?) {
        _state.update { it.copy(statusFilter = status) }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.listAll(status = _state.value.statusFilter)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, orders = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun approve(orderId: Long) = viewModelScope.launch {
        when (val result = repository.approve(orderId)) {
            is ApiResult.Success -> {
                _events.send(OrdersSupervisorEvent.Toast("Nalog odobren."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun decline(orderId: Long, partialQuantity: Int? = null) = viewModelScope.launch {
        when (val result = repository.decline(orderId, partialQuantity)) {
            is ApiResult.Success -> {
                val msg = if (partialQuantity != null) "Otkazano ${partialQuantity} jedinica." else "Nalog odbijen."
                _events.send(OrdersSupervisorEvent.Toast(msg))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class OrdersSupervisorState(
    val loading: Boolean = false,
    val orders: List<OrderDto> = emptyList(),
    val statusFilter: String? = null,
    val error: String? = null
)

sealed interface OrdersSupervisorEvent {
    data class Toast(val message: String) : OrdersSupervisorEvent
}
