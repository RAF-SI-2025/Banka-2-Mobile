package rs.raf.banka2.mobile.feature.otp

import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.payment.OtpResponseDto
import rs.raf.banka2.mobile.data.repository.PaymentRepository

/**
 * P1-mobile-trading-1 (R4-1757): refreshTick se inkrementira na svaki uspesan
 * refresh. UI countdown LaunchedEffect je keyed na refreshTick (ne na code) pa
 * se countdown restartuje svaki poll umesto da zamrzne na 0.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OtpViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<PaymentRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshTick_incrementsOnEachSuccessfulRefresh() = runTest(dispatcher) {
        coEvery { repository.getActiveOtp() } returns ApiResult.Success(
            OtpResponseDto(active = true, code = "123456", expiresIn = 30)
        )
        val vm = OtpViewModel(repository)  // init -> refresh #1
        advanceUntilIdle()
        val tickAfterInit = vm.state.value.refreshTick
        assertEquals(30, vm.state.value.secondsLeft)
        assertTrue(tickAfterInit >= 1)

        vm.refresh()  // refresh #2 — isti kod, svez secondsLeft
        advanceUntilIdle()
        assertEquals(tickAfterInit + 1, vm.state.value.refreshTick)
    }

    @Test
    fun tickOneSecond_decrementsUntilZero() = runTest(dispatcher) {
        coEvery { repository.getActiveOtp() } returns ApiResult.Success(
            OtpResponseDto(active = true, code = "111222", expiresIn = 2)
        )
        val vm = OtpViewModel(repository)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.secondsLeft)
        vm.tickOneSecond(); vm.tickOneSecond(); vm.tickOneSecond()
        assertEquals(0, vm.state.value.secondsLeft)
    }
}
