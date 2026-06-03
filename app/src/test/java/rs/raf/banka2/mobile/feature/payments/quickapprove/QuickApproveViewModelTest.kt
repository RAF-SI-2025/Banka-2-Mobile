package rs.raf.banka2.mobile.feature.payments.quickapprove

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.payment.PaymentResponseDto
import rs.raf.banka2.mobile.data.repository.PaymentRepository
import java.math.BigDecimal

/**
 * P2-8: Quick Approve ViewModel ponasanje (validacija OTP-a + state handling).
 * URL/body assercija je u `PaymentRepositoryApproveTest` (MockWebServer);
 * ovde mockujemo repo da deterministicki testiramo VM logiku.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuickApproveViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<PaymentRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // init { loadPayment() } poziv — vrati PENDING payment
        coEvery { repository.getPaymentById(any()) } returns ApiResult.Success(
            PaymentResponseDto(
                id = 42L,
                toAccount = "265000001234567890",
                amount = BigDecimal("1500"),
                currency = "RSD",
                status = "PENDING",
                recipientName = "Pera",
                description = "Racun",
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeVm(paymentId: Long = 42L): QuickApproveViewModel {
        val handle = SavedStateHandle(mapOf("paymentId" to paymentId))
        return QuickApproveViewModel(handle, repository)
    }

    private fun makeVmWithTimestamp(timestamp: String?): QuickApproveViewModel {
        val handle = SavedStateHandle(
            mapOf("paymentId" to 42L, "notificationCreatedAt" to timestamp)
        )
        return QuickApproveViewModel(handle, repository)
    }

    @Test
    fun computeExpired_unparseableTimestamp_failsClosed() = runTest(dispatcher) {
        // R1-587: timestamp POSTOJI ali je korumpiran/nepoznatog formata → expired=true
        // (ranije fail-OPEN false → odobravanje isteklog linka).
        val vm = makeVmWithTimestamp("not-a-valid-instant")
        advanceUntilIdle()
        assertTrue(vm.state.value.expired)
    }

    @Test
    fun computeExpired_oldTimestamp_expired() = runTest(dispatcher) {
        val old = java.time.Instant.now().minusSeconds(600).toString() // 10 min ago > 5 min
        val vm = makeVmWithTimestamp(old)
        advanceUntilIdle()
        assertTrue(vm.state.value.expired)
    }

    @Test
    fun computeExpired_freshTimestamp_notExpired() = runTest(dispatcher) {
        val fresh = java.time.Instant.now().toString()
        val vm = makeVmWithTimestamp(fresh)
        // NE advance-ujemo virtuelno vreme — `expired` se postavlja sinhrono u init-u.
        // (Ticker re-evaluira preko realnog sata u produkciji; ovde proveravamo
        // inicijalnu vrednost: svez link nije istekao.)
        assertFalse(vm.state.value.expired)
    }

    @Test
    fun onApproveRequested_expiredLink_doesNotCallRepo() = runTest(dispatcher) {
        val vm = makeVmWithTimestamp("not-a-valid-instant") // expired=true (fail-closed)
        advanceUntilIdle()

        vm.setOtpCode("123456")
        vm.onApproveRequested()
        advanceUntilIdle()

        assertEquals("Link za odobravanje je istekao.", vm.state.value.error)
        coVerify(exactly = 0) { repository.approveQuick(any(), any()) }
    }

    @Test
    fun onApproveRequested_withValidOtp_callsRepoAndSetsStatus() = runTest(dispatcher) {
        coEvery { repository.approveQuick(42L, "123456") } returns ApiResult.Success(
            PaymentResponseDto(id = 42L, status = "COMPLETED")
        )

        val vm = makeVm()
        advanceUntilIdle()

        vm.setOtpCode("123456")
        vm.onApproveRequested()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.approveQuick(42L, "123456") }
        val state = vm.state.value
        assertFalse(state.approving)
        assertEquals("COMPLETED", state.status)
        assertEquals(null, state.error)
    }

    @Test
    fun onApproveRequested_success_emitsNavigateBack() = runTest(dispatcher) {
        coEvery { repository.approveQuick(any(), any()) } returns ApiResult.Success(
            PaymentResponseDto(id = 42L, status = "COMPLETED")
        )

        val vm = makeVm()
        advanceUntilIdle()

        val collected = mutableListOf<QuickApproveEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.setOtpCode("123456")
        vm.onApproveRequested()
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is QuickApproveEvent.NavigateBack })
    }

    @Test
    fun onApproveRequested_repoFailure_setsErrorMessage() = runTest(dispatcher) {
        coEvery { repository.approveQuick(any(), any()) } returns ApiResult.Failure(
            ApiError(httpCode = 401, message = "Pogresan OTP", kind = ApiError.Kind.Unauthorized)
        )

        val vm = makeVm()
        advanceUntilIdle()

        vm.setOtpCode("000000")
        vm.onApproveRequested()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.approving)
        assertEquals("Pogresan OTP", state.error)
        coVerify { repository.approveQuick(42L, "000000") }
    }

    @Test
    fun onApproveRequested_incompleteOtp_doesNotCallRepo() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        vm.setOtpCode("12")
        vm.onApproveRequested()
        advanceUntilIdle()

        assertEquals("Verifikacioni kod mora imati 6 cifara.", vm.state.value.error)
        coVerify(exactly = 0) { repository.approveQuick(any(), any()) }
    }

    @Test
    fun setOtpCode_keepsDigitsOnly_maxSix() = runTest(dispatcher) {
        val vm = makeVm()
        advanceUntilIdle()

        vm.setOtpCode("12a34b5678")
        assertTrue(vm.state.value.otpCode.all { it.isDigit() })
        assertEquals("123456", vm.state.value.otpCode)
    }
}
