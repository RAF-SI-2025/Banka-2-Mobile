package rs.raf.banka2.mobile.feature.orders.create

import androidx.lifecycle.SavedStateHandle
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
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto
import rs.raf.banka2.mobile.data.dto.listing.ListingDto
import rs.raf.banka2.mobile.data.dto.order.CreateOrderDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.ExchangeManagementRepository
import rs.raf.banka2.mobile.data.repository.FundRepository
import rs.raf.banka2.mobile.data.repository.ListingRepository
import rs.raf.banka2.mobile.data.repository.OrderRepository
import javax.inject.Inject

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val listingRepository: ListingRepository,
    private val exchangeManagementRepository: ExchangeManagementRepository,
    private val orderRepository: OrderRepository,
    private val fundRepository: FundRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val listingId: Long = savedStateHandle["listingId"] ?: 0L
    private val initialDirection: String = savedStateHandle["direction"] ?: "BUY"

    private val _state = MutableStateFlow(
        CreateOrderState(
            direction = OrderDirection.from(initialDirection)
        )
    )
    val state: StateFlow<CreateOrderState> = _state.asStateFlow()

    private val _events = Channel<CreateOrderEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeRole()
        viewModelScope.launch { loadAccounts() }
        viewModelScope.launch { loadListing() }
    }

    private fun observeRole() {
        viewModelScope.launch {
            sessionManager.state.collect { session ->
                val profile = (session as? SessionState.LoggedIn)?.profile
                val role = profile?.role ?: UserRole.Unknown
                // ME-04: za CLIENT-a sa canTradeStocks=false UI je gated.
                // P1-fe-mobile-authz-1 (1753): FAIL-CLOSED kad profila nema
                // (process death, SessionManager je in-memory pa se sesija ne
                // restoruje pri deep-link/restored navigaciji). Ranije `?: true`
                // (ALLOW) → klijent bez TRADE_STOCKS bi dobio pun trade UI.
                val canTrade = profile?.canAccessTrading ?: false
                _state.update {
                    it.copy(
                        isEmployee = role.isEmployee,
                        canPickFund = role.isSupervisor,
                        canTrade = canTrade
                    )
                }
                // Supervizor moze da kupuje "u ime fonda" — fetchujemo listu fondova
                // da bi UI prikazao dropdown sa imenima umesto ID input polja.
                // Spec Celina 4 (Nova) §3905-3925.
                if (role.isSupervisor && _state.value.funds.isEmpty()) {
                    loadFunds()
                }
            }
        }
    }

    fun setDirection(value: OrderDirection) = _state.update { it.copy(direction = value) }
    fun setOrderType(value: OrderType) = _state.update { it.copy(orderType = value) }
    fun setQuantity(value: String) = _state.update { it.copy(quantity = value.filter { ch -> ch.isDigit() }, error = null) }
    fun setLimitPrice(value: String) = _state.update { it.copy(limitPrice = value, error = null) }
    fun setStopPrice(value: String) = _state.update { it.copy(stopPrice = value, error = null) }
    fun setAllOrNone(value: Boolean) = _state.update { it.copy(allOrNone = value) }
    // R2-1491: izbor racuna i fonda su MEDJUSOBNO ISKLJUCIVI (supervizor kupuje u
    // ime banke ILI u ime fonda). Selektovanje jednog ponisti drugi.
    fun selectAccount(account: AccountDto) = _state.update {
        it.copy(selectedAccount = account, selectedFund = null, onBehalfOfFundId = "")
    }
    fun selectFund(fund: FundSummaryDto?) = _state.update {
        it.copy(
            selectedFund = fund,
            onBehalfOfFundId = fund?.id?.toString().orEmpty(),
            selectedAccount = if (fund != null) null else it.selectedAccount
        )
    }
    fun setUseMargin(value: Boolean) = _state.update { it.copy(useMargin = value) }

    /**
     * Spec Celina 4 (Nova): supervizor moze da naznaci da kupuje
     *  - u ime banke (sa biranjem bankinog racuna), ili
     *  - u ime investicionog fonda kojim upravlja.
     *
     * BE prepoznaje "kupovina za banku" preko `accountId` koji pripada bankinom
     * vlasniku — UI samo nudi supervizoru da bira iz svojih (account) listi.
     * Fond rezim postavi `selectedFund` i prosledjuje `onBehalfOfFundId` u DTO-u.
     */
    private suspend fun loadFunds() {
        when (val result = fundRepository.list()) {
            is ApiResult.Success -> _state.update { it.copy(funds = result.data) }
            is ApiResult.Failure -> Unit // Tihi fail — supervizor moze i bez fond selektora
            ApiResult.Loading -> Unit
        }
    }

    fun openVerification() {
        val current = _state.value
        val qty = current.quantity.toIntOrNull()
        // R2-1491: supervizor kupuje u ime banke (account) ILI u ime fonda (fund),
        // ali NE i jedno i drugo, i NE nijedno. Klijent uvek mora dati racun.
        val supervisorBoth = current.canPickFund &&
            current.selectedFund != null && current.selectedAccount != null
        val supervisorNeither = current.canPickFund &&
            current.selectedFund == null && current.selectedAccount == null
        when {
            current.listing == null -> _state.update { it.copy(error = "Hartija nije ucitana.") }
            qty == null || qty <= 0 -> _state.update { it.copy(error = "Kolicina mora biti veca od 0.") }
            supervisorBoth ->
                _state.update { it.copy(error = "Izaberi ILI racun banke ILI fond — ne oba.") }
            supervisorNeither ->
                _state.update { it.copy(error = "Izaberi racun banke ili fond za podmirenje.") }
            current.selectedAccount == null && !current.isEmployee ->
                _state.update { it.copy(error = "Odaberi racun za podmirenje.") }
            current.orderType == OrderType.Limit && current.limitPrice.isBlank() ->
                _state.update { it.copy(error = "Limit cena je obavezna.") }
            current.orderType == OrderType.Stop && current.stopPrice.isBlank() ->
                _state.update { it.copy(error = "Stop cena je obavezna.") }
            current.orderType == OrderType.StopLimit &&
                (current.limitPrice.isBlank() || current.stopPrice.isBlank()) ->
                _state.update { it.copy(error = "Stop i limit cena su obavezne.") }
            else -> _state.update { it.copy(error = null, showVerification = true) }
        }
    }

    fun closeVerification() = _state.update { it.copy(showVerification = false) }

    fun submitWithCode(code: String) {
        val current = _state.value
        val qty = current.quantity.toIntOrNull() ?: return
        val listing = current.listing ?: return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val request = CreateOrderDto(
                listingId = listing.id,
                orderType = current.orderType.api,
                direction = current.direction.api,
                quantity = qty,
                // P1-mobile-banking-1 (R7-2016/2022): srpski zarez ("12,50") nije validan
                // za Kotlin `toBigDecimalOrNull()` → vracao bi null → LIMIT/STOP cena se
                // tiho gubila i order isao bez/sa pogresnom cenom. Koristi sr-RS svestan
                // `MoneyFormatter.parseBigDecimal` (zarez=decimala, tacka=hiljade).
                limitPrice = MoneyFormatter.parseBigDecimal(current.limitPrice),
                stopPrice = MoneyFormatter.parseBigDecimal(current.stopPrice),
                allOrNone = current.allOrNone,
                margin = current.useMargin,
                accountId = current.selectedAccount?.id,
                onBehalfOfFundId = current.onBehalfOfFundId.toLongOrNull(),
                otpCode = code
            )
            when (val result = orderRepository.create(request)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, showVerification = false) }
                    _events.send(CreateOrderEvent.Success(result.data.id))
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private suspend fun loadAccounts() {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update {
                it.copy(accounts = result.data, selectedAccount = it.selectedAccount ?: result.data.firstOrNull())
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadListing() {
        when (val result = listingRepository.byId(listingId)) {
            is ApiResult.Success -> {
                _state.update { it.copy(listing = result.data) }
                // Pokreni i exchange status da odredimo after-hours flag
                result.data.exchangeAcronym?.let { acronym ->
                    when (val ex = exchangeManagementRepository.list()) {
                        is ApiResult.Success -> {
                            val exchange = ex.data.firstOrNull { e -> e.acronym.equals(acronym, true) }
                            _state.update {
                                it.copy(
                                    exchangeIsOpen = exchange?.isOpen,
                                    exchangeNextOpen = exchange?.nextOpenTime
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }
}

enum class OrderDirection(val api: String, val label: String) {
    Buy("BUY", "Kupi"),
    Sell("SELL", "Prodaj");

    companion object {
        fun from(value: String): OrderDirection = entries.firstOrNull { it.api.equals(value, true) } ?: Buy
    }
}

enum class OrderType(val api: String, val label: String) {
    Market("MARKET", "Market"),
    Limit("LIMIT", "Limit"),
    Stop("STOP", "Stop"),
    StopLimit("STOP_LIMIT", "Stop-Limit")
}

data class CreateOrderState(
    val listing: ListingDto? = null,
    val accounts: List<AccountDto> = emptyList(),
    val selectedAccount: AccountDto? = null,
    val funds: List<FundSummaryDto> = emptyList(),
    val selectedFund: FundSummaryDto? = null,
    val direction: OrderDirection = OrderDirection.Buy,
    val orderType: OrderType = OrderType.Market,
    val quantity: String = "",
    val limitPrice: String = "",
    val stopPrice: String = "",
    val allOrNone: Boolean = false,
    val useMargin: Boolean = false,
    val onBehalfOfFundId: String = "",
    val isEmployee: Boolean = false,
    val canPickFund: Boolean = false,
    val canTrade: Boolean = true,  // ME-04: CLIENT bez TRADE_STOCKS permisije ne moze kreirati order
    val showVerification: Boolean = false,
    val submitting: Boolean = false,
    val exchangeIsOpen: Boolean? = null,
    val exchangeNextOpen: String? = null,
    val error: String? = null
) {
    val estimatedTotal: java.math.BigDecimal?
        get() {
            val qty = quantity.toIntOrNull() ?: return null
            // P1-mobile-banking-1: sr-RS parser da "12,50" ne procita kao null.
            val price = MoneyFormatter.parseBigDecimal(limitPrice) ?: listing?.price ?: return null
            return price.multiply(java.math.BigDecimal(qty))
        }

    /**
     * Spec Celine 3: "Obavestenje ako je berza zatvorena pri kreiranju ordera" +
     * "After-hours rezim: berza je u after-hours stanju (manje od 4 sata od zatvaranja)".
     *
     * Mobile dobija `isOpen` flag direktno sa /exchanges endpoint-a — ako je
     * `false`, berza je zatvorena. Heuristiku za 4h pre/posle ne mozemo
     * tacno proveriti bez tajming detalja, ali ako je `isOpen` null (FOREX
     * koji radi 24/7) ne prikazujemo upozorenje.
     */
    val showAfterHoursWarning: Boolean
        get() = exchangeIsOpen == false
}

sealed interface CreateOrderEvent {
    data class Success(val orderId: Long) : CreateOrderEvent
}
