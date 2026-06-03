package rs.raf.banka2.mobile.feature.funds.discovery

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.fund.FundSummaryDto
import rs.raf.banka2.mobile.data.dto.fundstatistics.FundStatisticsDto
import rs.raf.banka2.mobile.data.repository.FundRepository
import java.util.concurrent.atomic.AtomicInteger

/**
 * R1-596: FundsDiscovery.loadStatistics salje statistike PARALELNO (async/awaitAll),
 * ne sekvencijalno N round-trip-ova.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FundsDiscoveryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<FundRepository>()
    private val sessionManager = mockk<SessionManager>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { sessionManager.state } returns MutableStateFlow(SessionState.LoggedOut)
        coEvery { repository.myPositions() } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadStatistics_fetchesAllFunds_inParallel() = runTest(dispatcher) {
        val funds = (1L..4L).map { FundSummaryDto(id = it, name = "F$it") }
        coEvery { repository.list(any(), any(), any()) } returns ApiResult.Success(funds)

        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        coEvery { repository.statistics(any()) } coAnswers {
            val now = concurrent.incrementAndGet()
            maxConcurrent.updateAndGet { maxOf(it, now) }
            delay(100L) // drzi poziv "u letu" da se preklope ako su paralelni
            concurrent.decrementAndGet()
            ApiResult.Success(FundStatisticsDto(fundId = firstArg()))
        }

        val vm = FundsDiscoveryViewModel(repository, sessionManager)
        advanceUntilIdle()

        // Svi fondovi su upitani i sklopljeni u mapu.
        coVerify(exactly = 1) { repository.statistics(1L) }
        coVerify(exactly = 1) { repository.statistics(4L) }
        assertEquals(4, vm.state.value.statisticsByFundId.size)
        // KLJUCNO za R1-596: vise od jednog poziva je istovremeno u letu → paralelno.
        assertTrue("Ocekivano paralelno izvrsenje (maxConcurrent > 1)", maxConcurrent.get() > 1)
    }
}
