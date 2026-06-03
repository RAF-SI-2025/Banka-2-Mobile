package rs.raf.banka2.mobile.feature.transfers.create

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
import rs.raf.banka2.mobile.data.dto.exchange.CalculateExchangeResponseDto
import rs.raf.banka2.mobile.data.dto.transfer.TransferFxRequestDto
import rs.raf.banka2.mobile.data.dto.transfer.TransferInternalRequestDto
import rs.raf.banka2.mobile.data.dto.transfer.TransferResponseDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.ExchangeRepository
import rs.raf.banka2.mobile.data.repository.TransferRepository
import java.math.BigDecimal

/**
 * NewTransferViewModel — interni (ista valuta) vs FX (razlicita valuta)
 * transfer, OTP gate (openVerification -> submitWithCode), validacija.
 *
 * Napomena: MoneyFormatter strip-uje tacku kao separator hiljada pa decimale
 * unosimo zarezom (sr-RS) ili koristimo cele brojeve.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewTransferViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val accountRepository = mockk<AccountRepository>()
    private val transferRepository = mockk<TransferRepository>()
    private val exchangeRepository = mockk<ExchangeRepository>()

    // R1 865: fixture-i nose realan availableBalance (raniji default 0 je padao
    // novi soft balance pre-check). Dovoljno za sve happy-path iznose (<=1000).
    private val rsd = AccountDto(id = 1L, accountNumber = "222-RSD", currency = "RSD", balance = BigDecimal("50000"), availableBalance = BigDecimal("50000"))
    private val rsd2 = AccountDto(id = 2L, accountNumber = "222-RSD2", currency = "RSD", balance = BigDecimal("50000"), availableBalance = BigDecimal("50000"))
    private val eur = AccountDto(id = 3L, accountNumber = "222-EUR", currency = "EUR", balance = BigDecimal("50000"), availableBalance = BigDecimal("50000"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // FX estimate poziv je best-effort — default stub da ne padne ako se okine.
        coEvery { exchangeRepository.calculate(any(), any(), any()) } returns
            ApiResult.Success(
                CalculateExchangeResponseDto(
                    fromCurrency = "RSD",
                    toCurrency = "EUR",
                    amount = BigDecimal("1000.0"),
                    convertedAmount = BigDecimal("850.0"),
                    rate = BigDecimal("117.0"),
                    exchangeRate = BigDecimal("117.0")
                )
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeVm(accounts: List<AccountDto>): NewTransferViewModel {
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(accounts)
        return NewTransferViewModel(accountRepository, transferRepository, exchangeRepository)
    }

    @Test
    fun init_autoSelectsRsdSourceAndDistinctDestination() = runTest(dispatcher) {
        val vm = makeVm(listOf(eur, rsd, rsd2))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(1L, state.fromAccount?.id)    // RSD prefer
        assertEquals(eur.id, state.toAccount?.id)  // prvi razlicit od source-a
    }

    @Test
    fun openVerification_sameAccount_setsError() = runTest(dispatcher) {
        val vm = makeVm(listOf(rsd))
        advanceUntilIdle()
        vm.setSource(rsd)
        vm.setDestination(rsd)
        vm.setAmount("100")

        vm.openVerification()
        advanceUntilIdle()

        assertEquals("Izvor i cilj moraju biti razliciti racuni.", vm.state.value.error)
        assertFalse(vm.state.value.showVerification)
    }

    @Test
    fun openVerification_invalidAmount_setsError() = runTest(dispatcher) {
        val vm = makeVm(listOf(rsd, rsd2))
        advanceUntilIdle()
        vm.setSource(rsd)
        vm.setDestination(rsd2)
        vm.setAmount("0")

        vm.openVerification()
        advanceUntilIdle()

        assertEquals("Unesi validan iznos.", vm.state.value.error)
        assertFalse(vm.state.value.showVerification)
    }

    @Test
    fun openVerification_amountExceedsAvailableBalance_setsError_noOtp() = runTest(dispatcher) {
        // R1 865: soft pre-check — iznos veci od raspolozivog salda izvora ne sme
        // otvoriti OTP modal.
        val poor = AccountDto(id = 8L, accountNumber = "222-LOW", currency = "RSD", balance = BigDecimal("100"), availableBalance = BigDecimal("100"))
        val vm = makeVm(listOf(poor, rsd2))
        advanceUntilIdle()
        vm.setSource(poor)
        vm.setDestination(rsd2)
        vm.setAmount("1000")

        vm.openVerification()
        advanceUntilIdle()

        assertEquals("Iznos prevazilazi raspolozivi saldo izvornog racuna.", vm.state.value.error)
        assertFalse(vm.state.value.showVerification)
    }

    @Test
    fun sameCurrency_submit_callsInternalTransfer() = runTest(dispatcher) {
        val vm = makeVm(listOf(rsd, rsd2))
        advanceUntilIdle()
        vm.setSource(rsd)
        vm.setDestination(rsd2)
        vm.setAmount("1000")
        assertFalse("RSD->RSD nije FX", vm.state.value.isFx)

        val captured = slot<TransferInternalRequestDto>()
        coEvery { transferRepository.internal(capture(captured)) } returns
            ApiResult.Success(TransferResponseDto(id = 10L, status = "COMPLETED"))

        vm.openVerification()
        advanceUntilIdle()
        assertTrue(vm.state.value.showVerification)

        val collected = mutableListOf<NewTransferEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.submitWithCode("123456")
        advanceUntilIdle()
        job.cancel()

        coVerify(exactly = 1) { transferRepository.internal(any()) }
        coVerify(exactly = 0) { transferRepository.fx(any()) }
        val dto = captured.captured
        // P1-mobile-banking-1 (R1-125): BE trazi broj racuna (NE id).
        assertEquals("222-RSD", dto.fromAccountNumber)
        assertEquals("222-RSD2", dto.toAccountNumber)
        assertEquals(BigDecimal("1000"), dto.amount)
        assertEquals("123456", dto.otpCode)
        assertTrue(collected.any { it is NewTransferEvent.Success && it.transferId == 10L })
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun differentCurrency_submit_callsFxTransfer_withTargetCurrency() = runTest(dispatcher) {
        val vm = makeVm(listOf(rsd, eur))
        advanceUntilIdle()
        vm.setSource(rsd)
        vm.setDestination(eur)
        vm.setAmount("1000")
        assertTrue("RSD->EUR je FX", vm.state.value.isFx)

        val captured = slot<TransferFxRequestDto>()
        coEvery { transferRepository.fx(capture(captured)) } returns
            ApiResult.Success(TransferResponseDto(id = 20L, status = "COMPLETED"))

        vm.openVerification()
        advanceUntilIdle()

        vm.submitWithCode("654321")
        advanceUntilIdle()

        coVerify(exactly = 1) { transferRepository.fx(any()) }
        coVerify(exactly = 0) { transferRepository.internal(any()) }
        val dto = captured.captured
        // P1-mobile-banking-1 (R1-125): FX DTO salje broj racuna (NE id/currency).
        assertEquals("222-RSD", dto.fromAccountNumber)
        assertEquals("222-EUR", dto.toAccountNumber)
        assertEquals("654321", dto.otpCode)
    }

    @Test
    fun submit_failure_setsErrorMessage() = runTest(dispatcher) {
        val vm = makeVm(listOf(rsd, rsd2))
        advanceUntilIdle()
        vm.setSource(rsd)
        vm.setDestination(rsd2)
        vm.setAmount("1000")

        coEvery { transferRepository.internal(any()) } returns
            ApiResult.Failure(ApiError(httpCode = 422, message = "Nedovoljno sredstava", kind = ApiError.Kind.Validation))

        vm.openVerification()
        advanceUntilIdle()
        vm.submitWithCode("123456")
        advanceUntilIdle()

        assertEquals("Nedovoljno sredstava", vm.state.value.error)
        assertFalse(vm.state.value.submitting)
    }
}
