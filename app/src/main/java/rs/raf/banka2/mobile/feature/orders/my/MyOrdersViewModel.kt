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
import rs.raf.banka2.mobile.core.format.DateFormatter
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

    fun setStatus(value: String?) {
        _state.update { it.copy(filterStatus = value) }
        refresh()
    }

    fun setListingType(value: String?) {
        _state.update { it.copy(filterListingType = value) }
        refresh()
    }

    fun setDateFrom(value: String) = _state.update { it.copy(filterDateFrom = value) }
    fun setDateTo(value: String) = _state.update { it.copy(filterDateTo = value) }

    /**
     * R1-597: validiraj datumske filtere PRE BE poziva. Slobodan unos ("abc",
     * "2026-13-40") je ranije isao direktno BE-u koji radi `LocalDate.parse` →
     * 400/500. Ako je bilo koji datum nevalidan, postavi gresku i NE pozivaj refresh.
     */
    fun applyFilters() {
        val s = _state.value
        if (!DateFormatter.isValidIsoDate(s.filterDateFrom) || !DateFormatter.isValidIsoDate(s.filterDateTo)) {
            _state.update { it.copy(error = "Datum mora biti u formatu YYYY-MM-DD.") }
            return
        }
        refresh()
    }

    fun resetFilters() {
        _state.update {
            it.copy(
                filterStatus = null,
                filterListingType = null,
                filterDateFrom = "",
                filterDateTo = ""
            )
        }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val s = _state.value
        when (val result = repository.myOrdersFiltered(
            status = s.filterStatus,
            listingType = s.filterListingType,
            dateFrom = s.filterDateFrom,
            dateTo = s.filterDateTo
        )) {
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
    val error: String? = null,
    /** C3 #7 / Spec C3 §7 — filteri istorije ordera. */
    val filterStatus: String? = null,           // PENDING/APPROVED/DECLINED/DONE/PARTIAL_FILLED
    val filterListingType: String? = null,      // STOCK/FUTURES/FOREX/OPTION
    val filterDateFrom: String = "",
    val filterDateTo: String = ""
)

/**
 * Lista podrzanih statusa za filter chip.
 *
 * R1-592: BE `OrderStatus` enum ima SAMO 4 vrednosti (PENDING/APPROVED/DECLINED/DONE).
 * Ranije su tu bili i "PARTIAL_FILLED" i "CANCELLED" koji NE postoje u BE enumu —
 * BE servis ih ignorise (`status` koji ne matchuje enum → filter se ne primeni →
 * tiho vraca SVE ordere). To je obmanjivalo korisnika: chip "Otkazan"/"Parcijalan"
 * je vracao kompletnu listu. Drzimo SAMO realne BE statuse.
 */
val MY_ORDERS_STATUS_OPTIONS: List<String> = listOf("ALL", "PENDING", "APPROVED", "DONE", "DECLINED")
val MY_ORDERS_STATUS_LABEL_SR: Map<String, String> = mapOf(
    "ALL" to "Sve",
    "PENDING" to "Ceka",
    "APPROVED" to "Aktivan",
    "DONE" to "Zavrsen",
    "DECLINED" to "Odbijen"
)

val MY_ORDERS_LISTING_TYPE_OPTIONS: List<String> = listOf("ALL", "STOCK", "FUTURES", "FOREX", "OPTION")
val MY_ORDERS_LISTING_TYPE_LABEL_SR: Map<String, String> = mapOf(
    "ALL" to "Sve",
    "STOCK" to "Akcije",
    "FUTURES" to "Futures",
    "FOREX" to "Forex",
    "OPTION" to "Opcije"
)

sealed interface MyOrdersEvent {
    data class Toast(val message: String) : MyOrdersEvent
}
