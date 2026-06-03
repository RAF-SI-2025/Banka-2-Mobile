package rs.raf.banka2.mobile.feature.orders.create

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.listing.ListingDto
import rs.raf.banka2.mobile.data.dto.order.CreateOrderDto
import rs.raf.banka2.mobile.data.dto.order.OrderDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.ExchangeManagementRepository
import rs.raf.banka2.mobile.data.repository.FundRepository
import rs.raf.banka2.mobile.data.repository.ListingRepository
import rs.raf.banka2.mobile.data.repository.OrderRepository
import java.math.BigDecimal

/**
 * CreateOrderViewModel — validacija ordera (kolicina, order-type specificna
 * polja), uloga/canTrade observacija iz SessionManager-a, i submit putanja.
 *
 * SessionManager.state je mokovan kao realan MutableStateFlow da observeRole()
 * dobije profil. HTTP oblik CreateOrderDto-a je u OrderRepositoryMockServerTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateOrderViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val accountRepository = mockk<AccountRepository>()
    private val listingRepository = mockk<ListingRepository>()
    private val exchangeManagementRepository = mockk<ExchangeManagementRepository>()
    private val orderRepository = mockk<OrderRepository>()
    private val fundRepository = mockk<FundRepository>()
    private val sessionManager = mockk<SessionManager>()

    private val stockListing = ListingDto(
        id = 88L, ticker = "AAPL", name = "Apple", listingType = "STOCK",
        exchangeAcronym = null, currency = "USD", price = BigDecimal("150.0")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(
            listOf(AccountDto(id = 3L, accountNumber = "222-USD", currency = "USD"))
        )
        coEvery { listingRepository.byId(88L) } returns ApiResult.Success(stockListing)
        coEvery { fundRepository.list(any(), any(), any()) } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun session(role: UserRole, canTrade: Boolean = true): MutableStateFlow<SessionState> {
        val profile = UserProfile(
            id = 1L, email = "u@b.rs", firstName = "U", lastName = "B",
            role = role, permissions = emptySet(), canTradeStocks = canTrade
        )
        return MutableStateFlow(SessionState.LoggedIn(profile))
    }

    private fun makeVm(
        sessionFlow: MutableStateFlow<SessionState>,
        listingId: Long = 88L
    ): CreateOrderViewModel {
        every { sessionManager.state } returns sessionFlow
        val handle = SavedStateHandle(mapOf("listingId" to listingId, "direction" to "BUY"))
        return CreateOrderViewModel(
            handle, accountRepository, listingRepository,
            exchangeManagementRepository, orderRepository, fundRepository, sessionManager
        )
    }

    @Test
    fun init_loadsListing_andClientCanTradeFlag() = runTest(dispatcher) {
        val vm = makeVm(session(UserRole.Client, canTrade = true))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(88L, state.listing?.id)
        assertEquals(3L, state.selectedAccount?.id)
        assertTrue(state.canTrade)
        assertFalse(state.isEmployee)
    }

    @Test
    fun observeRole_clientWithoutTradePermission_setsCanTradeFalse() = runTest(dispatcher) {
        val vm = makeVm(session(UserRole.Client, canTrade = false))
        advanceUntilIdle()
        assertFalse(vm.state.value.canTrade)
    }

    // P1-fe-mobile-authz-1 (1753): kad sesija nema profil (process death —
    // SessionManager je in-memory, LoggedOut posle restart-a) trade-gating mora
    // biti FAIL-CLOSED (canTrade=false), ne ALLOW.
    @Test
    fun observeRole_noProfileAfterProcessDeath_failsClosed() = runTest(dispatcher) {
        val vm = makeVm(MutableStateFlow(SessionState.LoggedOut))
        advanceUntilIdle()
        assertFalse(vm.state.value.canTrade)
    }

    @Test
    fun observeRole_supervisor_canPickFund_andLoadsFunds() = runTest(dispatcher) {
        val vm = makeVm(session(UserRole.Supervisor))
        advanceUntilIdle()

        assertTrue(vm.state.value.canPickFund)
        assertTrue(vm.state.value.isEmployee)
        coVerify { fundRepository.list(any(), any(), any()) }
    }

    @Test
    fun openVerification_zeroQuantity_setsError() = runTest(dispatcher) {
        val vm = makeVm(session(UserRole.Client))
        advanceUntilIdle()
        vm.setQuantity("0")

        vm.openVerification()
        advanceUntilIdle()

        assertEquals("Kolicina mora biti veca od 0.", vm.state.value.error)
        assertFalse(vm.state.value.showVerification)
    }

    @Test
    fun openVerification_limitOrderWithoutPrice_setsError() = runTest(dispatcher) {
        val vm = makeVm(session(UserRole.Client))
        advanceUntilIdle()
        vm.setQuantity("10")
        vm.setOrderType(OrderType.Limit)
        // limitPrice prazan

        vm.openVerification()
        advanceUntilIdle()

        assertEquals("Limit cena je obavezna.", vm.state.value.error)
        assertFalse(vm.state.value.showVerification)
    }

    @Test
    fun openVerification_stopLimitMissingBothPrices_setsError() = runTest(dispatcher) {
        val vm = makeVm(session(UserRole.Client))
        advanceUntilIdle()
        vm.setQuantity("10")
        vm.setOrderType(OrderType.StopLimit)

        vm.openVerification()
        advanceUntilIdle()

        assertEquals("Stop i limit cena su obavezne.", vm.state.value.error)
    }

    @Test
    fun openVerification_clientWithoutAccount_setsError() = runTest(dispatcher) {
        // Klijent bez racuna (override getMyAccounts da vrati praznu listu)
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(emptyList())
        val vm = makeVm(session(UserRole.Client))
        advanceUntilIdle()
        vm.setQuantity("10")

        vm.openVerification()
        advanceUntilIdle()

        assertEquals("Odaberi racun za podmirenje.", vm.state.value.error)
    }

    @Test
    fun openVerification_validMarketOrder_opensVerification() = runTest(dispatcher) {
        val vm = makeVm(session(UserRole.Client))
        advanceUntilIdle()
        vm.setQuantity("10")

        vm.openVerification()
        advanceUntilIdle()

        assertTrue(vm.state.value.showVerification)
        assertEquals(null, vm.state.value.error)
    }

    @Test
    fun submitWithCode_buildsCreateOrderDto_andEmitsSuccess() = runTest(dispatcher) {
        val captured = slot<CreateOrderDto>()
        coEvery { orderRepository.create(capture(captured)) } returns
            ApiResult.Success(OrderDto(id = 700L, orderType = "LIMIT", direction = "BUY", quantity = 10, status = "PENDING"))

        val vm = makeVm(session(UserRole.Client))
        advanceUntilIdle()
        vm.setQuantity("10")
        vm.setOrderType(OrderType.Limit)
        // P1-mobile-banking-1 (R7-2016): sr-RS unos koristi zarez za decimale.
        vm.setLimitPrice("151,5")
        vm.openVerification()
        advanceUntilIdle()

        val collected = mutableListOf<CreateOrderEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.submitWithCode("123456")
        advanceUntilIdle()
        job.cancel()

        coVerify(exactly = 1) { orderRepository.create(any()) }
        val dto = captured.captured
        assertEquals(88L, dto.listingId)
        assertEquals("LIMIT", dto.orderType)
        assertEquals("BUY", dto.direction)
        assertEquals(10, dto.quantity)
        assertEquals(BigDecimal("151.5"), dto.limitPrice)
        assertEquals(3L, dto.accountId)
        assertEquals("123456", dto.otpCode)
        assertTrue(collected.any { it is CreateOrderEvent.Success && it.orderId == 700L })
        assertFalse(vm.state.value.submitting)
    }

    // P1-mobile-banking-1 (R7-2016/2022): pre fix-a `limitPrice.toBigDecimalOrNull()`
    // je vracao null za srpski zarez ("12,50") → limit/stop cena se tiho gubila i order
    // je isao bez cene. Sada se koristi MoneyFormatter.parseBigDecimal (zarez=decimala).
    @Test
    fun submitWithCode_serbianCommaLimitAndStop_parsedCorrectly() = runTest(dispatcher) {
        val captured = slot<CreateOrderDto>()
        coEvery { orderRepository.create(capture(captured)) } returns
            ApiResult.Success(OrderDto(id = 701L, orderType = "STOP_LIMIT", direction = "BUY", quantity = 5, status = "PENDING"))

        val vm = makeVm(session(UserRole.Client))
        advanceUntilIdle()
        vm.setQuantity("5")
        vm.setOrderType(OrderType.StopLimit)
        vm.setLimitPrice("1.234,56")   // sr-RS: tacka=hiljade, zarez=decimala
        vm.setStopPrice("99,90")
        vm.openVerification()
        advanceUntilIdle()

        vm.submitWithCode("123456")
        advanceUntilIdle()

        coVerify(exactly = 1) { orderRepository.create(any()) }
        val dto = captured.captured
        assertEquals(BigDecimal("1234.56"), dto.limitPrice)
        assertEquals(BigDecimal("99.90"), dto.stopPrice)
    }

    @Test
    fun submitWithCode_repoFailure_setsError() = runTest(dispatcher) {
        coEvery { orderRepository.create(any()) } returns
            ApiResult.Failure(ApiError(httpCode = 409, message = "Berza je zatvorena", kind = ApiError.Kind.Conflict))

        val vm = makeVm(session(UserRole.Client))
        advanceUntilIdle()
        vm.setQuantity("10")
        vm.openVerification()
        advanceUntilIdle()

        vm.submitWithCode("123456")
        advanceUntilIdle()

        assertEquals("Berza je zatvorena", vm.state.value.error)
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun setQuantity_keepsDigitsOnly() = runTest(dispatcher) {
        val vm = makeVm(session(UserRole.Client))
        advanceUntilIdle()
        vm.setQuantity("1a2b3")
        assertEquals("123", vm.state.value.quantity)
    }

    // ─── R2-1491: supervizor — racun XOR fond ───

    @Test
    fun supervisor_neitherAccountNorFund_setsError() = runTest(dispatcher) {
        // Bez racuna i bez fonda → mora biti greska.
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(emptyList())
        val vm = makeVm(session(UserRole.Supervisor))
        advanceUntilIdle()
        vm.setQuantity("10")

        vm.openVerification()
        advanceUntilIdle()

        assertEquals("Izaberi racun banke ili fond za podmirenje.", vm.state.value.error)
        assertFalse(vm.state.value.showVerification)
    }

    @Test
    fun supervisor_selectFund_clearsAccount_andVerifies() = runTest(dispatcher) {
        coEvery { fundRepository.list(any(), any(), any()) } returns ApiResult.Success(
            listOf(rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto(id = 9L, name = "Fond A"))
        )
        val vm = makeVm(session(UserRole.Supervisor))
        advanceUntilIdle()
        vm.setQuantity("10")

        // Default je racun selektovan; biranje fonda ga ponisti (XOR).
        vm.selectFund(rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto(id = 9L, name = "Fond A"))
        assertEquals(null, vm.state.value.selectedAccount)
        assertEquals(9L, vm.state.value.selectedFund?.id)

        vm.openVerification()
        advanceUntilIdle()

        assertTrue(vm.state.value.showVerification)
        assertEquals(null, vm.state.value.error)
    }

    @Test
    fun supervisor_defaultAccountOnly_verifies() = runTest(dispatcher) {
        // Supervizor sa default-selektovanim racunom (bez fonda) → kupovina u ime banke.
        val vm = makeVm(session(UserRole.Supervisor))
        advanceUntilIdle()
        vm.setQuantity("10")

        vm.openVerification()
        advanceUntilIdle()

        assertTrue(vm.state.value.showVerification)
        assertEquals(null, vm.state.value.error)
    }
}
