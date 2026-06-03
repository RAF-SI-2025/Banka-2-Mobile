package rs.raf.banka2.mobile.feature.audit

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.repository.AuditRepository

/**
 * R1-597: audit datumski filteri se validiraju klijent-side.
 * R1-599: filter aktera je numericki ID (`actorId`), ne email (BE nema email filter).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuditLogViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AuditRepository>()

    private fun emptyPage() = PageResponse<rs.raf.banka2.mobile.data.dto.audit.AuditLogDto>(
        content = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery {
            repository.query(any(), any(), any(), any(), any(), any())
        } returns ApiResult.Success(emptyPage())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun applyFilters_invalidDate_rejectedClientSide_noExtraBeCall() = runTest(dispatcher) {
        val vm = AuditLogViewModel(repository)
        advanceUntilIdle() // init refresh

        vm.setDateFrom("loš-datum")
        vm.applyFilters()
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.error!!.contains("YYYY-MM-DD"))
        coVerify(exactly = 1) { repository.query(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun setActorIdText_keepsDigits_andSentAsActorId() = runTest(dispatcher) {
        val vm = AuditLogViewModel(repository)
        advanceUntilIdle()

        vm.setActorIdText("a12b3")
        vm.applyFilters()
        advanceUntilIdle()

        // Numericki actorId = 123, NE email.
        coVerify { repository.query(actionType = any(), actorId = 123L, dateFrom = any(), dateTo = any(), page = 0, size = any()) }
    }
}
