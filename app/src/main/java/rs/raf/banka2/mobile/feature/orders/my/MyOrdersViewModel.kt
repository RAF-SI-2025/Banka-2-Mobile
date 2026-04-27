package rs.raf.banka2.mobile.feature.orders.my

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
class MyOrdersViewModel @Inject constructor(
    private val repository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MyOrdersState())
    val state: StateFlow<MyOrdersState> = _state.asStateFlow()

    private val _events = Channel<MyOrdersEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.myOrders()) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, orders = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun cancel(order: OrderDto, partialQuantity: Int? = null) = viewModelScope.launch {
        when (val result = repository.decline(order.id, partialQuantity)) {
            is ApiResult.Success -> {
                _events.send(MyOrdersEvent.Toast(if (partialQuantity == null) "Nalog otkazan." else "Parcijalno otkazano $partialQuantity komada."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

data class MyOrdersState(
    val loading: Boolean = false,
    val orders: List<OrderDto> = emptyList(),
    val error: String? = null
)

sealed interface MyOrdersEvent {
    data class Toast(val message: String) : MyOrdersEvent
}
