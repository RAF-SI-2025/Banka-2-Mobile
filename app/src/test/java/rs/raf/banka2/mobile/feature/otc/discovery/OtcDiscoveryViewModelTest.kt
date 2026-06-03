package rs.raf.banka2.mobile.feature.otc.discovery

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.OtcRepository

/**
 * P1-fe-mobile-authz-1 (1753): OtcDiscoveryViewModel trade-gating mora biti
 * FAIL-CLOSED kad nema profila (process death — SessionManager in-memory).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OtcDiscoveryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<OtcRepository>()
    private val sessionManager = mockk<SessionManager>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.discover(any()) } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeVm(session: SessionState): OtcDiscoveryViewModel {
        every { sessionManager.state } returns MutableStateFlow(session)
        return OtcDiscoveryViewModel(repository, sessionManager)
    }

    @Test
    fun init_clientWithTrade_canTradeTrue_andRefreshes() = runTest(dispatcher) {
        val profile = UserProfile(
            id = 1L, email = "c@b.rs", firstName = "C", lastName = "B",
            role = UserRole.Client, permissions = emptySet(), canTradeStocks = true
        )
        val vm = makeVm(SessionState.LoggedIn(profile))
        advanceUntilIdle()
        assertTrue(vm.state.value.canTrade)
        coVerify { repository.discover(any()) }
    }

    @Test
    fun init_clientWithoutTrade_canTradeFalse() = runTest(dispatcher) {
        val profile = UserProfile(
            id = 1L, email = "c@b.rs", firstName = "C", lastName = "B",
            role = UserRole.Client, permissions = emptySet(), canTradeStocks = false
        )
        val vm = makeVm(SessionState.LoggedIn(profile))
        advanceUntilIdle()
        assertFalse(vm.state.value.canTrade)
    }

    @Test
    fun init_noProfileAfterProcessDeath_failsClosed_noRefresh() = runTest(dispatcher) {
        val vm = makeVm(SessionState.LoggedOut)
        advanceUntilIdle()
        // FAIL-CLOSED: bez profila canTrade=false i NE pozivamo discover().
        assertFalse(vm.state.value.canTrade)
        assertFalse(vm.state.value.canSendOffer)
        coVerify(exactly = 0) { repository.discover(any()) }
    }

    // ─── [SEC] R1-591/600: OTC ponudu sme da inicira SAMO klijent/supervizor, NE agent ───

    private fun profile(role: UserRole, canTrade: Boolean = true) = UserProfile(
        id = 1L, email = "u@b.rs", firstName = "U", lastName = "B",
        role = role, permissions = emptySet(), canTradeStocks = canTrade
    )

    @Test
    fun agent_canSeeListings_butCannotSendOffer() = runTest(dispatcher) {
        val vm = makeVm(SessionState.LoggedIn(profile(UserRole.Agent)))
        advanceUntilIdle()
        // Agent sme da vidi discovery (canTrade=true) ali NE sme da posalje ponudu.
        assertTrue(vm.state.value.canTrade)
        assertFalse(vm.state.value.canSendOffer)
    }

    @Test
    fun client_withTrade_canSendOffer() = runTest(dispatcher) {
        val vm = makeVm(SessionState.LoggedIn(profile(UserRole.Client, canTrade = true)))
        advanceUntilIdle()
        assertTrue(vm.state.value.canSendOffer)
    }

    @Test
    fun client_withoutTrade_cannotSendOffer() = runTest(dispatcher) {
        val vm = makeVm(SessionState.LoggedIn(profile(UserRole.Client, canTrade = false)))
        advanceUntilIdle()
        assertFalse(vm.state.value.canSendOffer)
    }

    @Test
    fun supervisor_canSendOffer() = runTest(dispatcher) {
        val vm = makeVm(SessionState.LoggedIn(profile(UserRole.Supervisor)))
        advanceUntilIdle()
        assertTrue(vm.state.value.canSendOffer)
    }

    @Test
    fun agent_submitOffer_isBlocked_noRepositoryCall() = runTest(dispatcher) {
        val vm = makeVm(SessionState.LoggedIn(profile(UserRole.Agent)))
        advanceUntilIdle()

        val listing = rs.raf.banka2.mobile.data.dto.otc.OtcListingDto(
            listingId = 5L, ticker = "AAPL", sellerUserId = 42L, publicQuantity = 100
        )
        vm.submitOffer(listing, quantity = 1, pricePerStock = 10.0, premium = 1.0, settlementDate = "2026-12-31")
        advanceUntilIdle()

        // FAIL-CLOSED: agentu se postavi greska i NIKAD se ne poziva createOffer.
        assertFalse(vm.state.value.canSendOffer)
        coVerify(exactly = 0) { repository.createOffer(any(), any()) }
    }
}
