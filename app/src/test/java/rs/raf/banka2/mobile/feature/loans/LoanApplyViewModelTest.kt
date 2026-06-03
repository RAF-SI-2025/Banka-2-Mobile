package rs.raf.banka2.mobile.feature.loans

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationDto
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationResponseDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.LoanRepository
import java.math.BigDecimal

/**
 * LoanApplyViewModel — forma za apliciranje za kredit (ME-09: OTP gate).
 *  - submit() validira i otvara OTP modal (showVerification), NE poziva BE.
 *  - submitWithOtp(code) salje LoanApplicationDto sa parsiranim iznosom +
 *    otpCode-om; happy/error putanje.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoanApplyViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val accountRepository = mockk<AccountRepository>()
    private val loanRepository = mockk<LoanRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(
            listOf(AccountDto(id = 3L, accountNumber = "222-ACC", currency = "RSD"))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun filledVm(): LoanApplyViewModel {
        val vm = LoanApplyViewModel(accountRepository, loanRepository)
        vm.setAmount("500000")
        vm.setDuration("24")
        vm.setPurpose("Renoviranje")
        return vm
    }

    @Test
    fun submit_missingAmount_setsError_andDoesNotOpenVerification() = runTest(dispatcher) {
        val vm = LoanApplyViewModel(accountRepository, loanRepository)
        advanceUntilIdle()
        vm.setDuration("24")
        vm.setPurpose("Auto")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Iznos je obavezan.", vm.state.value.error)
        assertFalse(vm.state.value.showVerification)
    }

    @Test
    fun submit_missingDuration_setsError() = runTest(dispatcher) {
        val vm = LoanApplyViewModel(accountRepository, loanRepository)
        advanceUntilIdle()
        vm.setAmount("100000")
        vm.setPurpose("Auto")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Trajanje (broj meseci) je obavezno.", vm.state.value.error)
        assertFalse(vm.state.value.showVerification)
    }

    @Test
    fun submit_validForm_opensVerification_andCachesParsedValues() = runTest(dispatcher) {
        val vm = filledVm()
        advanceUntilIdle()

        vm.submit()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.showVerification)
        assertEquals(BigDecimal("500000"), state.parsedAmount)
        assertEquals(24, state.parsedDuration)
        assertEquals(null, state.error)
        // submit ne sme zvati BE — to radi submitWithOtp
        coVerify(exactly = 0) { loanRepository.apply(any()) }
    }

    @Test
    fun submitWithOtp_success_sendsApplicationWithOtp_andEmitsSubmitted() = runTest(dispatcher) {
        val captured = slot<LoanApplicationDto>()
        coEvery { loanRepository.apply(capture(captured)) } returns
            ApiResult.Success(LoanApplicationResponseDto(id = 1L, status = "PENDING"))

        val vm = filledVm()
        advanceUntilIdle()
        vm.submit()              // otvori OTP modal, popuni parsed vrednosti
        advanceUntilIdle()

        val collected = mutableListOf<LoanApplyEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.submitWithOtp("123456")
        advanceUntilIdle()
        job.cancel()

        coVerify(exactly = 1) { loanRepository.apply(any()) }
        val dto = captured.captured
        // P1-mobile-banking-1 (R1-131): DTO uskladjen sa BE LoanRequestDto.
        assertEquals(BigDecimal("500000"), dto.amount)
        assertEquals(24, dto.repaymentPeriod)
        assertEquals("FIXED", dto.interestType)
        assertEquals("Renoviranje", dto.loanPurpose)
        assertEquals("123456", dto.otpCode)
        assertEquals("222-ACC", dto.accountNumber)
        assertEquals("RSD", dto.currency)
        assertTrue("expected Submitted event, got=$collected", collected.any { it is LoanApplyEvent.Submitted })
        assertFalse(vm.state.value.submitting)
        assertFalse(vm.state.value.showVerification)
    }

    @Test
    fun submitWithOtp_failure_setsError_keepsModalLogic() = runTest(dispatcher) {
        coEvery { loanRepository.apply(any()) } returns
            ApiResult.Failure(ApiError(httpCode = 401, message = "Pogresan OTP", kind = ApiError.Kind.Unauthorized))

        val vm = filledVm()
        advanceUntilIdle()
        vm.submit()
        advanceUntilIdle()

        vm.submitWithOtp("000000")
        advanceUntilIdle()

        assertEquals("Pogresan OTP", vm.state.value.error)
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun submitWithOtp_withoutPriorValidation_isNoOp() = runTest(dispatcher) {
        val vm = LoanApplyViewModel(accountRepository, loanRepository)
        advanceUntilIdle()

        // parsedAmount/parsedDuration su null jer submit() nije pozvan
        vm.submitWithOtp("123456")
        advanceUntilIdle()

        coVerify(exactly = 0) { loanRepository.apply(any()) }
    }
}
