package rs.raf.banka2.mobile.feature.recurringorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.listing.ListingDto
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringCadence
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringDirection
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringMode
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringOrderDto
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringOrderLabels
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.ListingRepository
import rs.raf.banka2.mobile.data.repository.RecurringOrderRepository
import java.math.BigDecimal
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 350L

/**
 * [FE3 Mobile port — DCA / RecurringOrder] VM za RecurringOrdersScreen.
 *
 * Holduje 2 stanja:
 *  1) listu trajnih naloga (sa 3-tab filterom Aktivni/Pauzirani/Svi)
 *  2) "nov nalog" formu sa listing autocomplete-om (debounce 350ms),
 *     direction toggle BUY/SELL, mode toggle BYAMOUNT/BYQUANTITY, value input,
 *     account picker, cadence picker.
 *
 * Pause/Resume/Cancel se rade inline sa per-row "submittingId" indikatorom da
 * vise akcija ne moze biti otvoreno paralelno.
 */
@HiltViewModel
class RecurringOrdersViewModel @Inject constructor(
    private val recurringRepository: RecurringOrderRepository,
    private val listingRepository: ListingRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecurringOrdersState())
    val state: StateFlow<RecurringOrdersState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        refresh()
        loadAccounts()
    }

    // ---- Lista naloga ----

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = recurringRepository.listMy()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, orders = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun setFilter(tab: RecurringOrderLabels.FilterTab) {
        if (_state.value.filter == tab) return
        _state.update { it.copy(filter = tab) }
    }

    fun pause(id: Long) = updateStatus(id) { recurringRepository.pause(it) }
    fun resume(id: Long) = updateStatus(id) { recurringRepository.resume(it) }

    private fun updateStatus(
        id: Long,
        action: suspend (Long) -> ApiResult<RecurringOrderDto>,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(submittingId = id, error = null) }
            when (val result = action(id)) {
                is ApiResult.Success -> _state.update { st ->
                    st.copy(
                        submittingId = null,
                        orders = st.orders.map { if (it.id == id) result.data else it },
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submittingId = null, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun openCancelConfirm(order: RecurringOrderDto) =
        _state.update { it.copy(cancelTarget = order) }

    fun dismissCancelConfirm() = _state.update { it.copy(cancelTarget = null) }

    fun confirmCancel() {
        val target = _state.value.cancelTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(submittingId = target.id, error = null) }
            when (val result = recurringRepository.cancel(target.id)) {
                is ApiResult.Success -> _state.update { st ->
                    st.copy(
                        submittingId = null,
                        cancelTarget = null,
                        orders = st.orders.filterNot { it.id == target.id },
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submittingId = null, cancelTarget = null, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    // ---- Forma za novi nalog ----

    fun setSearchQuery(query: String) {
        _state.update { it.copy(form = it.form.copy(searchQuery = query)) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(form = it.form.copy(searchResults = emptyList())) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            when (val result = listingRepository.list(type = null, search = query)) {
                is ApiResult.Success -> _state.update {
                    it.copy(form = it.form.copy(searchResults = result.data.take(20)))
                }
                else -> Unit
            }
        }
    }

    fun selectListing(listing: ListingDto) {
        _state.update {
            it.copy(
                form = it.form.copy(
                    selectedListing = listing,
                    searchQuery = listing.ticker,
                    searchResults = emptyList(),
                )
            )
        }
    }

    fun clearListing() = _state.update {
        it.copy(form = it.form.copy(selectedListing = null, searchQuery = "", searchResults = emptyList()))
    }

    fun setDirection(direction: RecurringDirection) =
        _state.update { it.copy(form = it.form.copy(direction = direction)) }

    fun setMode(mode: RecurringMode) =
        _state.update { it.copy(form = it.form.copy(mode = mode)) }

    fun setCadence(cadence: RecurringCadence) =
        _state.update { it.copy(form = it.form.copy(cadence = cadence)) }

    fun setValueText(text: String) {
        val parsed = text.replace(',', '.').toBigDecimalOrNull()
        _state.update { it.copy(form = it.form.copy(valueText = text, value = parsed)) }
    }

    fun selectAccount(id: Long) =
        _state.update { it.copy(form = it.form.copy(accountId = id)) }

    fun submitNewOrder() {
        val form = _state.value.form
        val validation = validateForm(form)
        if (validation != null) {
            _state.update { it.copy(formError = validation) }
            return
        }
        val listing = form.selectedListing!!
        val value = form.value!!
        val accountId = form.accountId!!
        viewModelScope.launch {
            _state.update { it.copy(submittingForm = true, formError = null) }
            val result = recurringRepository.create(
                listingId = listing.id,
                direction = form.direction,
                mode = form.mode,
                value = value,
                accountId = accountId,
                cadence = form.cadence,
            )
            when (result) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        submittingForm = false,
                        orders = listOf(result.data) + it.orders,
                        form = RecurringOrderForm(),
                        formSuccess = "Trajni nalog kreiran.",
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submittingForm = false, formError = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun clearMessages() = _state.update {
        it.copy(error = null, formError = null, formSuccess = null)
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            when (val result = accountRepository.getMyAccounts()) {
                is ApiResult.Success -> _state.update {
                    val defaultAccount = result.data.firstOrNull { acc -> acc.currency.equals("RSD", true) }
                        ?: result.data.firstOrNull()
                    it.copy(
                        accounts = result.data,
                        form = it.form.copy(
                            accountId = it.form.accountId ?: defaultAccount?.id,
                        ),
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    companion object {
        /**
         * Pure forma validacija — vraca user-facing poruku ili null.
         * Testirano u `RecurringOrderFormValidationTest`.
         */
        fun validateForm(form: RecurringOrderForm): String? {
            if (form.selectedListing == null) return "Izaberi hartiju."
            if (form.value == null || form.value <= BigDecimal.ZERO) {
                return "Vrednost mora biti pozitivan broj."
            }
            if (form.mode == RecurringMode.BYQUANTITY) {
                // BYQUANTITY mora biti ceo broj akcija
                if (form.value.stripTrailingZeros().scale() > 0) {
                    return "Po kolicini: vrednost mora biti ceo broj akcija."
                }
            }
            if (form.accountId == null) return "Izaberi racun."
            return null
        }
    }
}

data class RecurringOrderForm(
    val searchQuery: String = "",
    val searchResults: List<ListingDto> = emptyList(),
    val selectedListing: ListingDto? = null,
    val direction: RecurringDirection = RecurringDirection.BUY,
    val mode: RecurringMode = RecurringMode.BYAMOUNT,
    val valueText: String = "",
    val value: BigDecimal? = null,
    val accountId: Long? = null,
    val cadence: RecurringCadence = RecurringCadence.MONTHLY,
)

data class RecurringOrdersState(
    val orders: List<RecurringOrderDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val filter: RecurringOrderLabels.FilterTab = RecurringOrderLabels.FilterTab.ACTIVE,
    val loading: Boolean = false,
    val submittingId: Long? = null,
    val submittingForm: Boolean = false,
    val form: RecurringOrderForm = RecurringOrderForm(),
    val formError: String? = null,
    val formSuccess: String? = null,
    val error: String? = null,
    val cancelTarget: RecurringOrderDto? = null,
) {
    val activeCount: Int get() = orders.count { it.active }
    val pausedCount: Int get() = orders.count { !it.active }
    val totalCount: Int get() = orders.size

    val filteredOrders: List<RecurringOrderDto>
        get() = when (filter) {
            RecurringOrderLabels.FilterTab.ACTIVE -> orders.filter { it.active }
            RecurringOrderLabels.FilterTab.PAUSED -> orders.filterNot { it.active }
            RecurringOrderLabels.FilterTab.ALL -> orders
        }
}
