package rs.raf.banka2.mobile.feature.funds.details

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.api.EmployeeAdminApi
import rs.raf.banka2.mobile.data.dto.common.EmployeeDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.fund.FundDetailDto
import rs.raf.banka2.mobile.data.dto.fund.FundPositionDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.FundRepository
import retrofit2.Response
import java.math.BigDecimal

/**
 * TEST-mobile-trading-vm-1 (R4-1078): 0-test karakterizacioni baseline za
 * [FundDetailsViewModel]. Pinuje:
 *  - load() sklapa fund/performance/accounts/myPosition u state happy-path
 *  - isSupervisor se izvodi iz sesije (CLIENT → false, SUPERVISOR → true) i
 *    azurira na promenu sesije (collect grana)
 *  - statistics 404/501 = tihi fallback (state ostaje null bez greske)
 *  - myPositions filtrira poziciju za bas ovaj fundId
 *  - reassign-manager (supervizor flow): openReassignDialog filtrira kandidate
 *    (samo SUPERVISOR/ADMIN, bez trenutnog menadzera), reassign uspeh → fund update
 *
 * SavedStateHandle["fundId"] = 7L. Repo/api se mockuju (deterministicki VM logika);
 * HTTP/wire asercije su u FundRepositoryTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FundDetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fundRepository = mockk<FundRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val employeeAdminApi = mockk<EmployeeAdminApi>()
    private val sessionManager = mockk<SessionManager>()

    private val fundDetail = FundDetailDto(
        id = 7L,
        name = "Tech Fund",
        managerId = 100L,
        totalValue = BigDecimal("1000000"),
        liquidFunds = BigDecimal("250000")
    )

    private val myPosition = FundPositionDto(id = 5L, fundId = 7L, totalInvested = BigDecimal("5000"))
    private val otherPosition = FundPositionDto(id = 6L, fundId = 99L, totalInvested = BigDecimal("9000"))

    private fun loggedIn(role: UserRole) = SessionState.LoggedIn(
        UserProfile(
            id = 1L, email = "u@x.rs", firstName = "U", lastName = "X",
            role = role, permissions = emptySet()
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { sessionManager.state } returns MutableStateFlow(loggedIn(UserRole.Client))
        coEvery { fundRepository.details(7L) } returns ApiResult.Success(fundDetail)
        coEvery { fundRepository.performance(7L) } returns ApiResult.Success(emptyList())
        coEvery { fundRepository.myPositions() } returns ApiResult.Success(listOf(myPosition, otherPosition))
        coEvery { fundRepository.statistics(7L) } returns ApiResult.Success(
            rs.raf.banka2.mobile.data.dto.fundstatistics.FundStatisticsDto(fundId = 7L)
        )
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun savedState() = androidx.lifecycle.SavedStateHandle(mapOf("fundId" to 7L))

    private fun vm() = FundDetailsViewModel(savedState(), fundRepository, accountRepository, employeeAdminApi, sessionManager)

    @Test
    fun load_assemblesFundPerformanceAccountsAndOwnPosition() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(7L, state.fund?.id)
        assertEquals(BigDecimal("250000"), state.fund?.liquidFunds)
        // myPosition mora biti TACNO pozicija za fundId=7 (ne 99).
        assertEquals(5L, state.myPosition?.id)
        assertEquals(7L, state.myPosition?.fundId)
        assertNull(state.error)
    }

    @Test
    fun isSupervisor_falseForClient() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertFalse(vm.state.value.isSupervisor)
    }

    @Test
    fun isSupervisor_trueForSupervisor() = runTest(dispatcher) {
        every { sessionManager.state } returns MutableStateFlow(loggedIn(UserRole.Supervisor))
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.state.value.isSupervisor)
    }

    @Test
    fun isSupervisor_trueForAdmin_adminIsSupervisor() = runTest(dispatcher) {
        every { sessionManager.state } returns MutableStateFlow(loggedIn(UserRole.Admin))
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.state.value.isSupervisor)
    }

    @Test
    fun statistics_missingEndpoint_silentFallback_noError() = runTest(dispatcher) {
        // 404/501 = BE ne podrzava endpoint → statistics ostaje null, BEZ banner greske.
        coEvery { fundRepository.statistics(7L) } returns ApiResult.Failure(
            ApiError(httpCode = 404, message = "Not Found", kind = ApiError.Kind.NotFound)
        )
        val vm = vm()
        advanceUntilIdle()

        assertNull(vm.state.value.statistics)
        assertNull(vm.state.value.error)
    }

    @Test
    fun fetchDetails_failure_setsError() = runTest(dispatcher) {
        coEvery { fundRepository.details(7L) } returns ApiResult.Failure(
            ApiError(httpCode = 500, message = "Greska na serveru.", kind = ApiError.Kind.Server)
        )
        val vm = vm()
        advanceUntilIdle()

        assertEquals("Greska na serveru.", vm.state.value.error)
    }

    @Test
    fun invest_success_emitsToast_andReloads() = runTest(dispatcher) {
        coEvery { fundRepository.invest(7L, 1L, BigDecimal("1000"), "RSD") } returns
            ApiResult.Success(myPosition)
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<FundDetailsEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.invest(sourceAccountId = 1L, amount = BigDecimal("1000"))
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is FundDetailsEvent.Toast })
        assertFalse(vm.state.value.submitting)
        // init load (1×) + post-invest reload (1×) = 2× details
        coVerify(exactly = 2) { fundRepository.details(7L) }
    }

    @Test
    fun invest_failure_setsError_noToast() = runTest(dispatcher) {
        coEvery { fundRepository.invest(any(), any(), any(), any()) } returns ApiResult.Failure(
            ApiError(httpCode = 400, message = "Ispod minimalnog uloga.", kind = ApiError.Kind.Validation)
        )
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<FundDetailsEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.invest(sourceAccountId = 1L, amount = BigDecimal("1"))
        advanceUntilIdle()
        job.cancel()

        assertEquals("Ispod minimalnog uloga.", vm.state.value.error)
        assertTrue("ne sme Toast na neuspeh", collected.isEmpty())
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun openReassignDialog_filtersToSupervisorsAndAdmins_excludingCurrentManager() = runTest(dispatcher) {
        // managerId = 100; samo SUPERVISOR/ADMIN kandidati koji NISU trenutni menadzer.
        val candidates = PageResponse(
            content = listOf(
                EmployeeDto(id = 100L, email = "mgr@x.rs", permissions = listOf("SUPERVISOR")),    // trenutni menadzer → izbaciti
                EmployeeDto(id = 101L, email = "sup@x.rs", permissions = listOf("SUPERVISOR")),     // OK
                EmployeeDto(id = 102L, email = "adm@x.rs", permissions = listOf("ADMIN")),          // OK
                EmployeeDto(id = 103L, email = "agt@x.rs", permissions = listOf("AGENT"))           // agent → izbaciti
            )
        )
        coEvery { employeeAdminApi.list(page = 0, limit = 200) } returns Response.success(candidates)

        val vm = vm()
        advanceUntilIdle()
        vm.openReassignDialog()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.reassignDialogVisible)
        val ids = state.reassignCandidates.map { it.id }.toSet()
        assertEquals(setOf(101L, 102L), ids)
    }

    @Test
    fun reassignManager_success_updatesFund_andEmitsToast() = runTest(dispatcher) {
        val updated = fundDetail.copy(managerId = 101L, managerName = "Novi Menadzer")
        coEvery { fundRepository.reassignManager(7L, 101L) } returns ApiResult.Success(updated)
        // reassignManager poziva load() na kraju koji re-fetcha details(); BE posle
        // prebacaja vraca novog menadzera — mock to reflektuje (inace bi reload
        // pregazio optimisticki update). Karakterizujemo BE-consistent stanje.
        coEvery { fundRepository.details(7L) } returns ApiResult.Success(updated)
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<FundDetailsEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.reassignManager(101L)
        advanceUntilIdle()
        job.cancel()

        val state = vm.state.value
        assertEquals(101L, state.fund?.managerId)
        assertFalse(state.reassignDialogVisible)
        assertTrue(state.reassignCandidates.isEmpty())
        assertTrue(collected.any { it is FundDetailsEvent.Toast })
        coVerify(exactly = 1) { fundRepository.reassignManager(7L, 101L) }
    }
}
