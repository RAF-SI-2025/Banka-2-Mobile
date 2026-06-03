package rs.raf.banka2.mobile.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.ui.theme.ThemeManager
import rs.raf.banka2.mobile.core.ui.theme.ThemeMode
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeRateDto
import rs.raf.banka2.mobile.data.dto.order.OrderDto
import rs.raf.banka2.mobile.data.dto.payment.PaymentListItemDto
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioSummaryDto
import rs.raf.banka2.mobile.data.dto.recipient.RecipientDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.EmployeeAdminRepository
import rs.raf.banka2.mobile.data.repository.ExchangeRepository
import rs.raf.banka2.mobile.data.repository.OrderRepository
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import rs.raf.banka2.mobile.data.repository.PortfolioRepository
import rs.raf.banka2.mobile.data.repository.RecipientRepository
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val accountRepository: AccountRepository,
    private val paymentRepository: PaymentRepository,
    private val exchangeRepository: ExchangeRepository,
    private val recipientRepository: RecipientRepository,
    private val portfolioRepository: PortfolioRepository,
    private val orderRepository: OrderRepository,
    private val employeeRepository: EmployeeAdminRepository,
    private val themeManager: ThemeManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = themeManager.mode

    init {
        observeSession()
        refresh()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.state.collect { session ->
                val profile = (session as? SessionState.LoggedIn)?.profile
                val previousRole = _state.value.profile?.role
                _state.update { it.copy(profile = profile) }
                if (profile?.role != previousRole) {
                    loadRoleData(profile?.role ?: UserRole.Unknown)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { loadAccounts() }
        viewModelScope.launch { loadRecentPayments() }
        viewModelScope.launch { loadRates() }
        viewModelScope.launch { loadRecipients() }
        loadRoleData(_state.value.profile?.role ?: UserRole.Unknown)
    }

    private fun loadRoleData(role: UserRole) {
        if (role.isClient) return
        if (!role.isEmployee) return
        viewModelScope.launch { loadPortfolioSummary() }
        viewModelScope.launch { loadRecentOrders() }
        if (role.isAdmin) viewModelScope.launch { loadEmployeeStats() }
    }

    fun toggleTheme() = themeManager.toggleLightDark()

    private suspend fun loadAccounts() {
        _state.update { it.copy(loadingAccounts = true) }
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update {
                it.copy(loadingAccounts = false, accounts = result.data, accountsError = null)
            }
            is ApiResult.Failure -> _state.update {
                it.copy(loadingAccounts = false, accountsError = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    private suspend fun loadRecentPayments() {
        when (val result = paymentRepository.getMyPayments(page = 0, limit = 6)) {
            is ApiResult.Success -> _state.update { it.copy(recentPayments = result.data) }
            else -> Unit
        }
    }

    private suspend fun loadRates() {
        when (val result = exchangeRepository.rates()) {
            is ApiResult.Success -> _state.update { it.copy(exchangeRates = result.data.take(6)) }
            else -> Unit
        }
    }

    private suspend fun loadRecipients() {
        when (val result = recipientRepository.list()) {
            is ApiResult.Success -> _state.update { it.copy(recipients = result.data.take(6)) }
            else -> Unit
        }
    }

    private suspend fun loadPortfolioSummary() {
        when (val result = portfolioRepository.summary()) {
            is ApiResult.Success -> _state.update { it.copy(portfolioSummary = result.data) }
            else -> Unit
        }
    }

    private suspend fun loadRecentOrders() {
        when (val result = orderRepository.myOrders(page = 0, size = 5)) {
            is ApiResult.Success -> _state.update { it.copy(recentOrders = result.data) }
            else -> Unit
        }
    }

    private suspend fun loadEmployeeStats() {
        when (val result = employeeRepository.list()) {
            is ApiResult.Success -> {
                val total = result.data.size
                val active = result.data.count { it.active == true }
                _state.update { it.copy(employeesTotal = total, employeesActive = active) }
            }
            else -> Unit
        }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.logout() }
    }
}

data class HomeState(
    val profile: UserProfile? = null,
    val accounts: List<AccountDto> = emptyList(),
    val recentPayments: List<PaymentListItemDto> = emptyList(),
    val exchangeRates: List<ExchangeRateDto> = emptyList(),
    val recipients: List<RecipientDto> = emptyList(),
    val recentOrders: List<OrderDto> = emptyList(),
    val portfolioSummary: PortfolioSummaryDto? = null,
    val employeesTotal: Int = 0,
    val employeesActive: Int = 0,
    val loadingAccounts: Boolean = false,
    val accountsError: String? = null
) {
    val totalRsdBalance: BigDecimal
        get() = accounts.filter { it.currency.equals("RSD", true) }
            .fold(BigDecimal.ZERO) { acc, a -> acc + a.balance }

    val foreignAccountsCount: Int
        get() = accounts.count { !it.currency.equals("RSD", true) }

    /**
     * Kumulativni trend salda — uzimamo poslednjih 7 placanja u obrnutom redu
     * i racunamo tekuci saldo kao da svako placanje ide unatrag.
     *
     * ME-11: rezultat je `BigDecimal` jer su Account.balance i Payment.amount
     * sada BigDecimal. Sparkline crtanje koristi `.toDouble()` na point-u.
     *
     * R2-1492 [money]: trend krece od `totalRsdBalance` (samo RSD racuni), pa
     * SME da sabira/oduzima SAMO RSD placanja. Ranije se `p.amount` sabirao bez
     * obzira na valutu → FX placanje od npr. 1000 EUR bi pomerilo RSD trend za
     * 1000 (kao da je 1000 RSD) i besmisleno iskrivilo grafik. Filtriramo na
     * RSD valutu (placanja bez valute tretiramo kao RSD — domaci default).
     */
    val balanceTrend: List<BigDecimal>
        get() {
            val rsdPayments = recentPayments.filter {
                it.currency.isNullOrBlank() || it.currency.equals("RSD", true)
            }
            val sorted = rsdPayments.takeLast(7)
            if (sorted.isEmpty()) return emptyList()
            var running = totalRsdBalance
            val points = mutableListOf<BigDecimal>()
            sorted.reversed().forEach { p ->
                points += running
                running = if (p.direction.equals("INCOMING", true)) running - p.amount else running + p.amount
            }
            return points.reversed()
        }
}
