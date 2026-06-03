package rs.raf.banka2.mobile.feature.tax

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.tax.TaxBreakdownItemDto
import rs.raf.banka2.mobile.data.dto.tax.TaxRecordDto
import rs.raf.banka2.mobile.data.repository.TaxRepository
import java.math.BigDecimal

/**
 * TEST-mobile-trading-vm-1 (OT-1255 sample): karakterizacioni baseline za
 * [TaxViewModel] (nepokriven ekran). Pinuje:
 *  - init refresh ucitava records
 *  - calculate() uspeh → Toast + re-refresh; neuspeh → error
 *  - openBreakdown bez userId/userType → breakdownError, BEZ network poziva
 *  - openBreakdown sa validnim userId/userType → getBreakdown(userId, userType)
 *  - closeBreakdown cisti breakdown state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaxViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<TaxRepository>(relaxed = true)

    private val record = TaxRecordDto(userId = 5L, name = "Klijent", userType = "CLIENT", taxAmount = BigDecimal("150"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.listAll() } returns ApiResult.Success(listOf(record))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = TaxViewModel(repository)

    @Test
    fun init_loadsRecords() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        assertEquals(1, vm.state.value.records.size)
        assertEquals(5L, vm.state.value.records[0].userId)
        assertNull(vm.state.value.error)
    }

    @Test
    fun calculate_success_emitsToast_andRefreshes() = runTest(dispatcher) {
        coEvery { repository.calculate() } returns ApiResult.Success(Unit)
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<TaxEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.calculate()
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is TaxEvent.Toast })
        // init refresh + post-calculate refresh = 2×
        coVerify(exactly = 2) { repository.listAll() }
    }

    @Test
    fun calculate_failure_setsError() = runTest(dispatcher) {
        coEvery { repository.calculate() } returns ApiResult.Failure(
            ApiError(httpCode = 403, message = "Samo supervizor moze pokrenuti obracun.", kind = ApiError.Kind.Forbidden)
        )
        val vm = vm()
        advanceUntilIdle()

        vm.calculate()
        advanceUntilIdle()

        assertEquals("Samo supervizor moze pokrenuti obracun.", vm.state.value.error)
    }

    @Test
    fun openBreakdown_missingUserId_setsBreakdownError_noNetwork() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.openBreakdown(TaxRecordDto(userId = null, userType = "CLIENT"))
        advanceUntilIdle()

        assertEquals("Nema validnog userId/userType za breakdown.", vm.state.value.breakdownError)
        coVerify(exactly = 0) { repository.getBreakdown(any(), any()) }
    }

    @Test
    fun openBreakdown_valid_callsGetBreakdown_andFillsItems() = runTest(dispatcher) {
        val items = listOf(TaxBreakdownItemDto(ticker = "AAPL", taxOwed = BigDecimal("7.5")))
        coEvery { repository.getBreakdown(5L, "CLIENT") } returns ApiResult.Success(items)
        val vm = vm()
        advanceUntilIdle()

        vm.openBreakdown(record)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.getBreakdown(5L, "CLIENT") }
        assertEquals(1, vm.state.value.breakdownItems.size)
        assertEquals("AAPL", vm.state.value.breakdownItems[0].ticker)
        assertEquals(5L, vm.state.value.breakdownTarget?.userId)
    }

    @Test
    fun closeBreakdown_clearsBreakdownState() = runTest(dispatcher) {
        coEvery { repository.getBreakdown(any(), any()) } returns ApiResult.Success(
            listOf(TaxBreakdownItemDto(ticker = "AAPL"))
        )
        val vm = vm()
        advanceUntilIdle()
        vm.openBreakdown(record)
        advanceUntilIdle()

        vm.closeBreakdown()

        val state = vm.state.value
        assertNull(state.breakdownTarget)
        assertTrue(state.breakdownItems.isEmpty())
        assertNull(state.breakdownError)
    }
}
