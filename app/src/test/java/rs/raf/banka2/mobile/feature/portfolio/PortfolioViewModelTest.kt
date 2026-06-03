package rs.raf.banka2.mobile.feature.portfolio

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
import rs.raf.banka2.mobile.data.dto.dividend.DividendPayoutDto
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioItemDto
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioSummaryDto
import rs.raf.banka2.mobile.data.repository.DividendRepository
import rs.raf.banka2.mobile.data.repository.OptionRepository
import rs.raf.banka2.mobile.data.repository.PortfolioRepository
import rs.raf.banka2.mobile.data.repository.TaxRepository
import java.math.BigDecimal

/**
 * TEST-mobile-trading-vm-1 (OT-1255 sample): karakterizacioni baseline za
 * [PortfolioViewModel] (nepokriven ekran). Pinuje:
 *  - init refresh sklapa positions/summary/taxBreakdown
 *  - taxBreakdown greska se NE prikazuje kao banner (samo cuva poruku za debug)
 *  - toggleDividends fetcha jednom + cache-uje (re-expand bez network-a) + collapse
 *  - dividend missing (404/501) → prazna lista, BEZ dividendsError
 *  - exerciseOption salje optionId (ne portfolio-row id) i refreshuje na uspeh
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<PortfolioRepository>(relaxed = true)
    private val optionRepository = mockk<OptionRepository>(relaxed = true)
    private val taxRepository = mockk<TaxRepository>(relaxed = true)
    private val dividendRepository = mockk<DividendRepository>(relaxed = true)

    private val stock = PortfolioItemDto(id = 10L, listingId = 1L, listingTicker = "AAPL", listingType = "STOCK", quantity = 5)
    private val summary = PortfolioSummaryDto(totalValue = BigDecimal("12345"), totalProfit = BigDecimal("200"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.myPortfolio() } returns ApiResult.Success(listOf(stock))
        coEvery { repository.summary() } returns ApiResult.Success(summary)
        coEvery { taxRepository.getMyBreakdown() } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = PortfolioViewModel(repository, optionRepository, taxRepository, dividendRepository)

    @Test
    fun init_assemblesPositionsAndSummary() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(1, state.positions.size)
        assertEquals("AAPL", state.positions[0].listingTicker)
        assertEquals(BigDecimal("12345"), state.summary?.totalValue)
        assertNull(state.error)
    }

    @Test
    fun loadTaxBreakdown_failure_isNotShownAsBanner() = runTest(dispatcher) {
        coEvery { taxRepository.getMyBreakdown() } returns ApiResult.Failure(
            ApiError(httpCode = 500, message = "tax fail", kind = ApiError.Kind.Server)
        )
        val vm = vm()
        advanceUntilIdle()

        // greska se cuva u taxBreakdownError (debug), ali NE u glavnom error banner-u.
        assertEquals("tax fail", vm.state.value.taxBreakdownError)
        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.taxBreakdown.isEmpty())
    }

    @Test
    fun toggleDividends_fetchesOnce_thenCaches_onReExpand() = runTest(dispatcher) {
        val payout = DividendPayoutDto(id = 1L, stockTicker = "AAPL", netAmount = BigDecimal("42"))
        coEvery { dividendRepository.getByPosition(10L) } returns ApiResult.Success(listOf(payout))
        val vm = vm()
        advanceUntilIdle()

        vm.toggleDividends(10L)           // expand → fetch
        advanceUntilIdle()
        assertEquals(10L, vm.state.value.expandedDividendPositionId)
        assertEquals(1, vm.state.value.dividendsByPosition[10L]?.size)

        vm.toggleDividends(10L)           // collapse
        advanceUntilIdle()
        assertNull(vm.state.value.expandedDividendPositionId)

        vm.toggleDividends(10L)           // re-expand → iz cache-a, bez novog poziva
        advanceUntilIdle()
        assertEquals(10L, vm.state.value.expandedDividendPositionId)

        coVerify(exactly = 1) { dividendRepository.getByPosition(10L) }
    }

    @Test
    fun toggleDividends_missingEndpoint_emptyList_noError() = runTest(dispatcher) {
        coEvery { dividendRepository.getByPosition(10L) } returns ApiResult.Failure(
            ApiError(httpCode = 404, message = "Not Found", kind = ApiError.Kind.NotFound)
        )
        val vm = vm()
        advanceUntilIdle()

        vm.toggleDividends(10L)
        advanceUntilIdle()

        assertNull(vm.state.value.dividendsError)
        assertTrue(vm.state.value.dividendsByPosition[10L]?.isEmpty() == true)
    }

    @Test
    fun exerciseOption_sendsOptionId_andEmitsToast() = runTest(dispatcher) {
        coEvery { optionRepository.exercise(777L) } returns ApiResult.Success(Unit)
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<PortfolioEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.exerciseOption(optionId = 777L)
        advanceUntilIdle()
        job.cancel()

        coVerify(exactly = 1) { optionRepository.exercise(777L) }
        assertTrue(collected.any { it is PortfolioEvent.Toast })
    }

    @Test
    fun exerciseOption_failure_setsError() = runTest(dispatcher) {
        coEvery { optionRepository.exercise(any()) } returns ApiResult.Failure(
            ApiError(httpCode = 400, message = "Opcija nije ITM.", kind = ApiError.Kind.Validation)
        )
        val vm = vm()
        advanceUntilIdle()

        vm.exerciseOption(optionId = 777L)
        advanceUntilIdle()

        assertEquals("Opcija nije ITM.", vm.state.value.error)
    }
}
