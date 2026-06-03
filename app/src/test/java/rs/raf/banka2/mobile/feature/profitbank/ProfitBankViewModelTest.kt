package rs.raf.banka2.mobile.feature.profitbank

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.fund.FundPositionDto
import rs.raf.banka2.mobile.data.dto.profitbank.ActuaryProfitDto
import rs.raf.banka2.mobile.data.dto.profitbank.BankFundPositionDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.FundRepository
import rs.raf.banka2.mobile.data.repository.ProfitBankRepository
import java.math.BigDecimal

/**
 * TEST-mobile-trading-vm-1 (R4-1129): 0-test VM baseline za [ProfitBankViewModel].
 * Pinuje (authz/baseline):
 *  - init { refresh + loadAccounts } sklapa actuaries/fundPositions/accounts u state
 *  - loadAccounts koristi `listAllAccounts()` (BANKINI racuni — R1-272), NE `getMyAccounts()`
 *  - AGENT 403 na bilo kom izvoru (actuary/fund-positions/accounts) → state.error
 *    (gating je BE-side; VM samo surfaceuje Forbidden poruku) ; accounts-403 je tih
 *  - invest/withdraw uspeh → Toast + refresh; neuspeh → state.error bez Toast-a
 *
 * Repo se mockuje; HTTP-shape je u ProfitBankRepository/DTO testovima.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfitBankViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ProfitBankRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val fundRepository = mockk<FundRepository>(relaxed = true)

    private val actuary = ActuaryProfitDto(employeeId = 9L, name = "Aktuar A", realizedProfitRsd = BigDecimal("1200"))
    private val position = BankFundPositionDto(fundId = 3L, fundName = "Banka Fond", shareAmountRsd = BigDecimal("50000"))
    private val bankAccount = AccountDto(id = 222L, accountNumber = "222-BANK", currency = "RSD", ownerName = "Banka")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.actuaryProfits() } returns ApiResult.Success(listOf(actuary))
        coEvery { repository.bankFundPositions() } returns ApiResult.Success(listOf(position))
        coEvery { accountRepository.listAllAccounts() } returns ApiResult.Success(listOf(bankAccount))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = ProfitBankViewModel(repository, accountRepository, fundRepository)

    @Test
    fun init_loadsActuariesFundPositionsAndBankAccounts() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(1, state.actuaries.size)
        assertEquals(9L, state.actuaries[0].employeeId)
        assertEquals(1, state.fundPositions.size)
        assertEquals(3L, state.fundPositions[0].fundId)
        assertEquals(1, state.accounts.size)
        assertEquals(222L, state.accounts[0].id)
        assertNull(state.error)
    }

    @Test
    fun loadAccounts_usesListAllAccounts_notMyAccounts() = runTest(dispatcher) {
        // R1-272: ProfitBank koristi BANKINE racune (GET /accounts/all), NE supervizorove licne.
        val vm = vm()
        advanceUntilIdle()

        coVerify(exactly = 1) { accountRepository.listAllAccounts() }
        coVerify(exactly = 0) { accountRepository.getMyAccounts() }
    }

    @Test
    fun agent403_onActuaryProfits_surfacesForbiddenError() = runTest(dispatcher) {
        // AGENT nema profit-bank pristup; gating je BE-side → 403 koje VM surfaceuje.
        coEvery { repository.actuaryProfits() } returns ApiResult.Failure(
            ApiError(httpCode = 403, message = "Nemate dozvolu za ovu akciju.", kind = ApiError.Kind.Forbidden)
        )
        val vm = vm()
        advanceUntilIdle()

        assertEquals("Nemate dozvolu za ovu akciju.", vm.state.value.error)
        assertTrue(vm.state.value.actuaries.isEmpty())
    }

    @Test
    fun agent403_onFundPositions_surfacesForbiddenError() = runTest(dispatcher) {
        coEvery { repository.bankFundPositions() } returns ApiResult.Failure(
            ApiError(httpCode = 403, message = "Nemate dozvolu.", kind = ApiError.Kind.Forbidden)
        )
        val vm = vm()
        advanceUntilIdle()

        assertEquals("Nemate dozvolu.", vm.state.value.error)
        assertTrue(vm.state.value.fundPositions.isEmpty())
    }

    @Test
    fun accountsFailure_isSilent_noError() = runTest(dispatcher) {
        // loadAccounts gresku tretira tiho (Failure → Unit); ne sme da prikaze banner
        // jer actuary/fund-positions su glavni sadrzaj ekrana.
        coEvery { accountRepository.listAllAccounts() } returns ApiResult.Failure(
            ApiError(httpCode = 403, message = "Nemate dozvolu.", kind = ApiError.Kind.Forbidden)
        )
        val vm = vm()
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.accounts.isEmpty())
    }

    @Test
    fun invest_success_emitsToast_clearsTarget_andRefreshes() = runTest(dispatcher) {
        coEvery { fundRepository.invest(3L, 222L, BigDecimal("10000"), "RSD") } returns
            ApiResult.Success(FundPositionDto(id = 1L, fundId = 3L))
        val vm = vm()
        advanceUntilIdle()
        vm.openInvestDialog(position)

        val collected = mutableListOf<ProfitBankEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.invest(fundId = 3L, sourceAccountId = 222L, amount = BigDecimal("10000"))
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is ProfitBankEvent.Toast })
        assertNull(vm.state.value.investTarget)
        assertFalse(vm.state.value.submitting)
        // init refresh (actuaryProfits 1×) + post-invest refresh (1×) = 2×
        coVerify(exactly = 2) { repository.actuaryProfits() }
    }

    @Test
    fun invest_failure_setsError_noToast() = runTest(dispatcher) {
        coEvery { fundRepository.invest(any(), any(), any(), any()) } returns ApiResult.Failure(
            ApiError(httpCode = 400, message = "Nedovoljno sredstava na bankinom racunu.", kind = ApiError.Kind.Validation)
        )
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<ProfitBankEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.invest(fundId = 3L, sourceAccountId = 222L, amount = BigDecimal("999999999"))
        advanceUntilIdle()
        job.cancel()

        assertEquals("Nedovoljno sredstava na bankinom racunu.", vm.state.value.error)
        assertTrue(collected.isEmpty())
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun withdraw_success_emitsToast_clearsTarget() = runTest(dispatcher) {
        coEvery { fundRepository.withdraw(3L, 222L, null, true) } returns
            ApiResult.Success(mockk(relaxed = true))
        val vm = vm()
        advanceUntilIdle()
        vm.openWithdrawDialog(position)

        val collected = mutableListOf<ProfitBankEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.withdraw(fundId = 3L, destinationAccountId = 222L, amount = null, withdrawAll = true)
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is ProfitBankEvent.Toast })
        assertNull(vm.state.value.withdrawTarget)
    }
}
