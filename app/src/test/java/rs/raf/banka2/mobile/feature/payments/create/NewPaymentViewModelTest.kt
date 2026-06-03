package rs.raf.banka2.mobile.feature.payments.create

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
import rs.raf.banka2.mobile.core.storage.PaymentRecoveryStore
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.interbank.InitiateInterbankPaymentDto
import rs.raf.banka2.mobile.data.dto.interbank.InterbankTransactionDto
import rs.raf.banka2.mobile.data.dto.payment.CreatePaymentRequestDto
import rs.raf.banka2.mobile.data.dto.payment.PaymentResponseDto
import rs.raf.banka2.mobile.data.dto.recipient.RecipientDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.InterbankRepository
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import rs.raf.banka2.mobile.data.repository.RecipientRepository
import java.math.BigDecimal

/**
 * NewPaymentViewModel — routing detekcija (intra "222" vs inter-bank po
 * routing prefiksu), confirm + OTP gate, intra-bank create i inter-bank 2PC
 * initiate. HTTP-shape assercije su u InterbankRepositoryTest/PaymentRepository*
 * — ovde testiramo VM state-logiku na mokovanim repo-ima.
 *
 * Inter-bank happy-path vraca TERMINALNI status (COMMITTED) odmah da bismo
 * izbegli polling loop (delay(3000)×40) koji bi advanceUntilIdle razvukao.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewPaymentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val accountRepository = mockk<AccountRepository>()
    private val recipientRepository = mockk<RecipientRepository>()
    private val paymentRepository = mockk<PaymentRepository>()
    private val interbankRepository = mockk<InterbankRepository>()
    private val recoveryStore = mockk<PaymentRecoveryStore>(relaxed = true)

    // R1 865: realan availableBalance (raniji default 0 padao bi novi soft
    // balance pre-check za happy-path iznose 1500).
    private val rsdAccount = AccountDto(id = 1L, accountNumber = "222-RSD", currency = "RSD", balance = BigDecimal("100000"), availableBalance = BigDecimal("100000"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(listOf(rsdAccount))
        coEvery { recipientRepository.list() } returns ApiResult.Success(emptyList<RecipientDto>())
        // Nema aktivnog 2PC iz prethodne sesije.
        coEvery { recoveryStore.getActive2PC() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeVm(): NewPaymentViewModel = NewPaymentViewModel(
        accountRepository, recipientRepository, paymentRepository, interbankRepository, recoveryStore
    )

    private fun fillValidForm(vm: NewPaymentViewModel, toAccount: String) {
        vm.setRecipientName("Marko Markovic")
        vm.setToAccountNumber(toAccount)
        vm.setAmount("1500")
        vm.setPurpose("Racun")
    }

    @Test
    fun init_loadsRsdAccountAsDefault() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()
        assertEquals(1L, vm.state.value.fromAccount?.id)
    }

    @Test
    fun openConfirmDialog_missingRecipient_setsError() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()
        vm.setToAccountNumber("222000001")
        vm.setAmount("100")
        vm.setPurpose("x")

        vm.openConfirmDialog()
        advanceUntilIdle()

        assertEquals("Ime primaoca je obavezno.", vm.state.value.error)
        assertFalse(vm.state.value.showConfirmDialog)
    }

    @Test
    fun openConfirmDialog_zeroAmount_setsError() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()
        vm.setRecipientName("Marko")
        vm.setToAccountNumber("222000001")
        vm.setAmount("0")
        vm.setPurpose("x")

        vm.openConfirmDialog()
        advanceUntilIdle()

        assertEquals("Iznos mora biti veci od 0.", vm.state.value.error)
        assertFalse(vm.state.value.showConfirmDialog)
    }

    @Test
    fun openConfirmDialog_amountExceedsAvailableBalance_setsError_noDialog() = runTest(dispatcher) {
        // R1 865: soft pre-check — iznos veci od raspolozivog salda izvora ne
        // otvara confirm dialog (sprecava nepotrebni OTP korak).
        val poor = AccountDto(id = 2L, accountNumber = "222-LOW", currency = "RSD", balance = BigDecimal("200"), availableBalance = BigDecimal("200"))
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(listOf(poor))
        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "222000001234567890") // iznos 1500 > 200

        vm.openConfirmDialog()
        advanceUntilIdle()

        assertEquals("Iznos prevazilazi raspolozivi saldo izvornog racuna.", vm.state.value.error)
        assertFalse(vm.state.value.showConfirmDialog)
    }

    @Test
    fun openConfirmDialog_ourBankPrefix_marksIntraBank() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "222000001234567890")  // prefix 222 = nasa banka

        vm.openConfirmDialog()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.showConfirmDialog)
        assertFalse("222 prefix => intra-bank", state.isInterbank)
        assertEquals(BigDecimal("1500"), state.parsedAmount)
        assertEquals(null, state.error)
    }

    @Test
    fun openConfirmDialog_foreignBankPrefix_marksInterBank() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "111000001234567890")  // prefix 111 = druga banka

        vm.openConfirmDialog()
        advanceUntilIdle()

        assertTrue("111 prefix => inter-bank", vm.state.value.isInterbank)
    }

    @Test
    fun intraBankSubmit_success_callsCreate_andEmitsSuccess() = runTest(dispatcher) {
        val captured = slot<CreatePaymentRequestDto>()
        coEvery { paymentRepository.create(capture(captured)) } returns
            ApiResult.Success(PaymentResponseDto(id = 321L, status = "COMPLETED"))

        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "222000001234567890")
        vm.openConfirmDialog()
        advanceUntilIdle()
        vm.confirmAndOpenOtp()

        val collected = mutableListOf<NewPaymentEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.submitWithCode("123456")
        advanceUntilIdle()
        job.cancel()

        coVerify(exactly = 1) { paymentRepository.create(any()) }
        coVerify(exactly = 0) { interbankRepository.initiate(any()) }
        val dto = captured.captured
        assertEquals("222000001234567890", dto.toAccountNumber)
        assertEquals("123456", dto.otpCode)
        assertEquals(BigDecimal("1500"), dto.amount)
        assertEquals(1L, dto.fromAccountId)
        assertTrue(collected.any { it is NewPaymentEvent.Success && it.paymentId == 321L })
        assertFalse(vm.state.value.verifying)
    }

    @Test
    fun intraBankSubmit_failure_setsError() = runTest(dispatcher) {
        coEvery { paymentRepository.create(any()) } returns
            ApiResult.Failure(ApiError(httpCode = 422, message = "Prekoracen dnevni limit", kind = ApiError.Kind.Validation))

        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "222000001234567890")
        vm.openConfirmDialog()
        advanceUntilIdle()
        vm.confirmAndOpenOtp()

        vm.submitWithCode("123456")
        advanceUntilIdle()

        assertEquals("Prekoracen dnevni limit", vm.state.value.error)
        assertFalse(vm.state.value.verifying)
    }

    @Test
    fun interBankSubmit_initiates2PC_savesRecovery_andShowsProgress() = runTest(dispatcher) {
        val captured = slot<InitiateInterbankPaymentDto>()
        // Terminalni COMMITTED odmah -> nema polling loop-a.
        coEvery { interbankRepository.initiate(capture(captured)) } returns ApiResult.Success(
            InterbankTransactionDto(
                transactionId = "555",
                status = "COMMITTED",
                message = "Placanje uspesno izvrseno."
            )
        )

        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "111000001234567890")  // inter-bank
        vm.openConfirmDialog()
        advanceUntilIdle()
        vm.confirmAndOpenOtp()

        vm.submitWithCode("654321")
        advanceUntilIdle()

        coVerify(exactly = 1) { interbankRepository.initiate(any()) }
        coVerify(exactly = 0) { paymentRepository.create(any()) }
        val dto = captured.captured
        assertEquals("111000001234567890", dto.toAccountNumber)
        assertEquals("654321", dto.otpCode)

        // recovery store dobija transactionId, pa ga (terminal) odmah cisti.
        coVerify { recoveryStore.saveActive2PC("555") }
        coVerify { recoveryStore.clearActive2PC() }

        val progress = vm.state.value.interbankProgress
        assertEquals("555", progress?.transactionId)
        assertEquals("COMMITTED", progress?.status)
        assertFalse(vm.state.value.verifying)
    }

    @Test
    fun interBankSubmit_failure_marksProgressAborted() = runTest(dispatcher) {
        coEvery { interbankRepository.initiate(any()) } returns
            ApiResult.Failure(ApiError(httpCode = 400, message = "Banka primalac nedostupna", kind = ApiError.Kind.Validation))

        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "111000001234567890")
        vm.openConfirmDialog()
        advanceUntilIdle()
        vm.confirmAndOpenOtp()

        vm.submitWithCode("654321")
        advanceUntilIdle()

        val progress = vm.state.value.interbankProgress
        assertEquals("ABORTED", progress?.status)
        assertEquals("Banka primalac nedostupna", progress?.message)
        assertFalse(vm.state.value.verifying)
    }

    // R4-1444b: 2PC fazno renderovanje. Posle initiate-a koji vrati NE-terminalni
    // status (npr. COMMITTING), VM mora da NASTAVI polling i da progresivno
    // azurira interbankProgress.status kroz faze dok ne dosegne terminal (COMMITTED).
    @Test
    fun interBank_nonTerminalInitiate_pollsThroughPhases_untilCommitted() = runTest(dispatcher) {
        // initiate → COMMITTING (PROCESSING faza), nije terminal pa krece polling.
        coEvery { interbankRepository.initiate(any()) } returns ApiResult.Success(
            InterbankTransactionDto(transactionId = "900", status = "COMMITTING", message = "U obradi (commit faza).")
        )
        // prvi (i jedini potreban) poll vraca terminalni COMMITTED.
        coEvery { interbankRepository.status("900") } returns ApiResult.Success(
            InterbankTransactionDto(transactionId = "900", status = "COMMITTED", message = "Placanje uspesno izvrseno.")
        )

        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "111000001234567890")
        vm.openConfirmDialog()
        advanceUntilIdle()
        vm.confirmAndOpenOtp()

        vm.submitWithCode("654321")
        advanceUntilIdle()

        // polling je pozvan i dosegao terminal COMMITTED.
        coVerify(atLeast = 1) { interbankRepository.status("900") }
        val progress = vm.state.value.interbankProgress
        assertEquals("COMMITTED", progress?.status)
        // recovery store ociscen posle terminal status-a u polling-u.
        coVerify { recoveryStore.clearActive2PC() }
    }

    @Test
    fun interBank_pollingTimeout_marksStuck() = runTest(dispatcher) {
        // initiate non-terminal; status uvek vraca NE-terminalni COMMITTING →
        // posle 40 pokusaja VM markira STUCK (banka ce dovrsiti naknadno).
        coEvery { interbankRepository.initiate(any()) } returns ApiResult.Success(
            InterbankTransactionDto(transactionId = "901", status = "INITIATED")
        )
        coEvery { interbankRepository.status("901") } returns ApiResult.Success(
            InterbankTransactionDto(transactionId = "901", status = "COMMITTING")
        )

        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "111000001234567890")
        vm.openConfirmDialog()
        advanceUntilIdle()
        vm.confirmAndOpenOtp()

        vm.submitWithCode("654321")
        advanceUntilIdle()

        assertEquals("STUCK", vm.state.value.interbankProgress?.status)
        coVerify { recoveryStore.clearActive2PC() }
    }

    @Test
    fun recoverActive2PC_nonTerminalStatus_reconstructsProgress_andPolls() = runTest(dispatcher) {
        // ME-08: pri mount-u postoji cuvani 2PC txId u ne-terminalnoj fazi (PREPARED-ekvivalent
        // COMMITTING) → VM rekonstruise progress dialog i nastavlja polling do COMMITTED.
        coEvery { recoveryStore.getActive2PC() } returns "555"
        coEvery { interbankRepository.status("555") } returnsMany listOf(
            ApiResult.Success(InterbankTransactionDto(transactionId = "555", status = "COMMITTING")),
            ApiResult.Success(InterbankTransactionDto(transactionId = "555", status = "COMMITTED"))
        )

        val vm = makeVm()
        advanceUntilIdle()

        // progress je rekonstruisan iz prethodne sesije i dosegao COMMITTED.
        val progress = vm.state.value.interbankProgress
        assertTrue(vm.state.value.isInterbank)
        assertEquals("COMMITTED", progress?.status)
        coVerify { recoveryStore.clearActive2PC() }
    }

    @Test
    fun recoverActive2PC_status404_clearsRecovery_noProgress() = runTest(dispatcher) {
        // BE ne nalazi transakciju (expired/404) → obrisi recovery, bez progress dialog-a.
        coEvery { recoveryStore.getActive2PC() } returns "999"
        coEvery { interbankRepository.status("999") } returns ApiResult.Failure(
            ApiError(httpCode = 404, message = "Not Found", kind = ApiError.Kind.NotFound)
        )

        val vm = makeVm()
        advanceUntilIdle()

        assertEquals(null, vm.state.value.interbankProgress)
        coVerify { recoveryStore.clearActive2PC() }
    }

    // ──────────────────────────────────────────────────────────────────────
    // R4-1364-paymentfx [REVIEWER, money-adjacent]: FX-field mapping kroz 2PC
    // InterbankProgress. Postojeci testovi pinuju status-tranzicije; ovi pinuju
    // da rate/convertedAmount/convertedCurrency/fee iz BE InterbankTransactionDto
    // budu KOREKTNO preneti u InterbankProgress (cross-currency inter-bank uplata).
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun interBankSubmit_crossCurrency_mapsFxFieldsIntoProgress() = runTest(dispatcher) {
        // BE odmah vraca terminalni COMMITTED sa FX poljima (RSD→EUR konverzija).
        coEvery { interbankRepository.initiate(any()) } returns ApiResult.Success(
            InterbankTransactionDto(
                transactionId = "777",
                status = "COMMITTED",
                amount = BigDecimal("1500"),
                currency = "RSD",
                convertedAmount = BigDecimal("12.79"),
                convertedCurrency = "EUR",
                rate = 117.28,
                fee = BigDecimal("7.50"),
                message = "Placanje uspesno izvrseno."
            )
        )

        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "111000001234567890")  // inter-bank
        vm.openConfirmDialog()
        advanceUntilIdle()
        vm.confirmAndOpenOtp()

        vm.submitWithCode("654321")
        advanceUntilIdle()

        val progress = vm.state.value.interbankProgress
        assertEquals("COMMITTED", progress?.status)
        // FX polja moraju preci iz BE DTO-a u UI progress (ranije bila hardkodirana null).
        assertEquals(117.28, progress?.rate!!, 0.0001)
        assertEquals(BigDecimal("12.79"), progress.convertedAmount)
        assertEquals("EUR", progress.convertedCurrency)
        assertEquals(BigDecimal("7.50"), progress.fee)
    }

    @Test
    fun interBank_polling_updatesFxFieldsFromStatusResponse() = runTest(dispatcher) {
        // initiate vraca ne-terminalni status BEZ FX polja (jos nije obracunato),
        // a poll vraca terminalni COMMITTED SA FX poljima → progress mora da ih usvoji.
        coEvery { interbankRepository.initiate(any()) } returns ApiResult.Success(
            InterbankTransactionDto(transactionId = "778", status = "COMMITTING")
        )
        coEvery { interbankRepository.status("778") } returns ApiResult.Success(
            InterbankTransactionDto(
                transactionId = "778",
                status = "COMMITTED",
                convertedAmount = BigDecimal("25.58"),
                convertedCurrency = "EUR",
                rate = 117.28,
                fee = BigDecimal("9.99"),
                message = "Placanje uspesno izvrseno."
            )
        )

        val vm = makeVm()
        advanceUntilIdle()
        fillValidForm(vm, toAccount = "111000001234567890")
        vm.openConfirmDialog()
        advanceUntilIdle()
        vm.confirmAndOpenOtp()

        vm.submitWithCode("654321")
        advanceUntilIdle()

        val progress = vm.state.value.interbankProgress
        assertEquals("COMMITTED", progress?.status)
        assertEquals(117.28, progress?.rate!!, 0.0001)
        assertEquals(BigDecimal("25.58"), progress.convertedAmount)
        assertEquals("EUR", progress.convertedCurrency)
        assertEquals(BigDecimal("9.99"), progress.fee)
    }

    @Test
    fun recoverActive2PC_reconstructsFxFieldsFromStatus() = runTest(dispatcher) {
        // ME-08: rekonstrukcija iz prethodne sesije terminalna (COMMITTED) sa FX
        // poljima → progress dialog mora da prikaze obracunatu konverziju, ne null.
        coEvery { recoveryStore.getActive2PC() } returns "779"
        coEvery { interbankRepository.status("779") } returns ApiResult.Success(
            InterbankTransactionDto(
                transactionId = "779",
                status = "COMMITTED",
                convertedAmount = BigDecimal("8.53"),
                convertedCurrency = "EUR",
                rate = 117.28,
                fee = BigDecimal("5.00")
            )
        )

        val vm = makeVm()
        advanceUntilIdle()

        val progress = vm.state.value.interbankProgress
        assertTrue(vm.state.value.isInterbank)
        assertEquals("COMMITTED", progress?.status)
        assertEquals(117.28, progress?.rate!!, 0.0001)
        assertEquals(BigDecimal("8.53"), progress.convertedAmount)
        assertEquals("EUR", progress.convertedCurrency)
        assertEquals(BigDecimal("5.00"), progress.fee)
        // terminal → recovery store ociscen.
        coVerify { recoveryStore.clearActive2PC() }
    }

    @Test
    fun selectRecipient_fillsNameAndAccount() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        vm.selectRecipient(RecipientDto(id = 9L, name = "Pera", accountNumber = "222000009999"))

        val state = vm.state.value
        assertEquals("Pera", state.recipientName)
        assertEquals("222000009999", state.toAccountNumber)
    }
}
