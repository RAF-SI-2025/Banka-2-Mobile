package rs.raf.banka2.mobile.core.ui.components

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.payment.OtpResponseDto
import rs.raf.banka2.mobile.data.repository.PaymentRepository

/**
 * TEST-mobile-banking-vm-1 (R4-1364-verif): 0-test karakterizacioni baseline za
 * [VerificationViewModel] (2FA OTP dijalog state). Pinuje:
 *  - initOnOpen() reset (secondsLeft=300, attemptsRemaining=3, devCode=null) +
 *    requestOtpToMobile() poziv
 *  - onCodeChange sanitizacija (samo cifre, max 6)
 *  - tickOneSecond odbrojavanje, ne ide ispod 0
 *  - requestEmailOtp() resetuje secondsLeft i flag-uje requestingEmail tokom poziva
 *  - R1-866: nema decrement-a attemptsRemaining (dijalog ne verifikuje sam)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VerificationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<PaymentRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // getActiveOtp() se zove u initOnOpen samo u DEBUG build-u; vratimo neaktivan
        // kod tako da devCode ostane null deterministicki (nezavisno od BuildConfig).
        coEvery { repository.getActiveOtp() } returns ApiResult.Success(
            OtpResponseDto(active = false, code = null)
        )
        coEvery { repository.requestOtpToMobile() } returns ApiResult.Success(Unit)
        coEvery { repository.requestOtpViaEmail() } returns ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = VerificationViewModel(repository)

    @Test
    fun initOnOpen_resetsState_andRequestsOtpToMobile() = runTest(dispatcher) {
        val vm = vm()
        vm.initOnOpen()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals("", s.code)
        assertEquals(300, s.secondsLeft)
        assertEquals(3, s.attemptsRemaining)
        assertNull(s.devCode)
        assertNull(s.localError)
        coVerify { repository.requestOtpToMobile() }
    }

    @Test
    fun onCodeChange_keepsDigitsOnly_maxSix() = runTest(dispatcher) {
        val vm = vm()
        vm.onCodeChange("12a3-45b6789")
        assertEquals("123456", vm.state.value.code)
    }

    @Test
    fun onCodeChange_clearsLocalError() = runTest(dispatcher) {
        val vm = vm()
        vm.setLocalError("Kod mora imati 6 cifara.")
        assertEquals("Kod mora imati 6 cifara.", vm.state.value.localError)

        vm.onCodeChange("1")
        assertNull(vm.state.value.localError)
    }

    @Test
    fun tickOneSecond_decrements_andStopsAtZero() = runTest(dispatcher) {
        val vm = vm()
        vm.initOnOpen()
        advanceUntilIdle()

        vm.tickOneSecond()
        assertEquals(299, vm.state.value.secondsLeft)

        // dovoljno tick-ova da se isprazni — ne sme ispod 0
        repeat(400) { vm.tickOneSecond() }
        assertEquals(0, vm.state.value.secondsLeft)
    }

    @Test
    fun requestEmailOtp_resetsTimer_andClearsRequestingFlag() = runTest(dispatcher) {
        val vm = vm()
        vm.initOnOpen()
        advanceUntilIdle()
        repeat(50) { vm.tickOneSecond() }
        assertEquals(250, vm.state.value.secondsLeft)

        vm.requestEmailOtp()

        coVerify { repository.requestOtpViaEmail() }
        // posle slanja email-a, timer je resetovan na 300 i flag ugasen
        assertEquals(300, vm.state.value.secondsLeft)
        assertEquals(false, vm.state.value.requestingEmail)
    }

    @Test
    fun fillFromActiveCode_noDevCode_isNoOp() = runTest(dispatcher) {
        // devCode je null (getActiveOtp neaktivan / release) → kod ostaje prazan.
        val vm = vm()
        vm.initOnOpen()
        advanceUntilIdle()

        vm.fillFromActiveCode()
        assertEquals("", vm.state.value.code)
    }

    @Test
    fun initOnOpen_populatesDevCode_whenServerExposesActiveOtp() = runTest(dispatcher) {
        // Mobilni-kao-autentifikator: kada server izlaze aktivan kod
        // (payments.expose-active-otp=true), initOnOpen ga dovuce i prikaze u
        // modalu — i u release build-u (klijentski DEBUG gate je uklonjen; server
        // je jedini autoritet o izlaganju). fillFromActiveCode ga potom upisuje.
        coEvery { repository.getActiveOtp() } returns ApiResult.Success(
            OtpResponseDto(active = true, code = "072758")
        )
        val vm = vm()
        vm.initOnOpen()
        advanceUntilIdle()

        assertEquals("072758", vm.state.value.devCode)
        coVerify { repository.getActiveOtp() }

        vm.fillFromActiveCode()
        assertEquals("072758", vm.state.value.code)
    }
}
