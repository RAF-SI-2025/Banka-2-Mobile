package rs.raf.banka2.mobile.feature.orders.my

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.OrderRepository

/**
 * R1-597: datumski filteri se validiraju klijent-side pre BE poziva.
 * R1-592: status filter lista sadrzi SAMO realne BE OrderStatus vrednosti.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyOrdersViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<OrderRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery {
            repository.myOrdersFiltered(any(), any(), any(), any(), any(), any())
        } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun applyFilters_invalidDate_rejectedClientSide_noBeCall() = runTest(dispatcher) {
        val vm = MyOrdersViewModel(repository)
        advanceUntilIdle()
        // init() je vec pozvao refresh() jednom — racunamo dalje pozive.

        vm.setDateFrom("nije-datum")
        vm.applyFilters()
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.error!!.contains("YYYY-MM-DD"))
        // Samo init refresh — applyFilters sa nevalidnim datumom NE zove BE ponovo.
        coVerify(exactly = 1) { repository.myOrdersFiltered(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun applyFilters_validDate_callsBe() = runTest(dispatcher) {
        val vm = MyOrdersViewModel(repository)
        advanceUntilIdle()

        vm.setDateFrom("2026-05-01")
        vm.setDateTo("2026-05-31")
        vm.applyFilters()
        advanceUntilIdle()

        // init + applyFilters = 2 poziva.
        coVerify(exactly = 2) { repository.myOrdersFiltered(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun statusOptions_onlyRealBackendStatuses() {
        // R1-592: BE OrderStatus = PENDING/APPROVED/DECLINED/DONE (nema CANCELLED/PARTIAL_FILLED).
        assertFalse(MY_ORDERS_STATUS_OPTIONS.contains("CANCELLED"))
        assertFalse(MY_ORDERS_STATUS_OPTIONS.contains("PARTIAL_FILLED"))
        assertEquals(listOf("ALL", "PENDING", "APPROVED", "DONE", "DECLINED"), MY_ORDERS_STATUS_OPTIONS)
    }

    // OT-1251: dosad je samo PENDING/STOCK pass-through bio pokriven (OrderRepositoryFiltersTest).
    // Pinujemo da VM zaista PROSLEDJUJE izabrani status/listingType repozitorijumu —
    // ukljucujuci CANCELLED (koji NIJE u MY_ORDERS_STATUS_OPTIONS pa BE ne moze da ga vrati,
    // ali ako ga UI ipak posalje, VM ga prosledjuje neizmenjen; "ALL→null" je odgovornost repo-a).
    @Test
    fun setStatus_passesSelectedStatusToRepository() = runTest(dispatcher) {
        val vm = MyOrdersViewModel(repository)
        advanceUntilIdle() // init refresh sa filterStatus=null

        vm.setStatus("DECLINED")
        advanceUntilIdle()

        // setStatus okida refresh; verifikuj da je DECLINED stigao do repo-a.
        coVerify { repository.myOrdersFiltered(status = "DECLINED", listingType = any(), dateFrom = any(), dateTo = any(), page = any(), size = any()) }
    }

    @Test
    fun setListingType_passesOptionToRepository() = runTest(dispatcher) {
        val vm = MyOrdersViewModel(repository)
        advanceUntilIdle()

        vm.setListingType("OPTION")
        advanceUntilIdle()

        coVerify { repository.myOrdersFiltered(status = any(), listingType = "OPTION", dateFrom = any(), dateTo = any(), page = any(), size = any()) }
    }

    @Test
    fun resetFilters_clearsStatusAndListingType_andRefreshesWithNulls() = runTest(dispatcher) {
        val vm = MyOrdersViewModel(repository)
        advanceUntilIdle()
        vm.setStatus("PENDING")
        vm.setListingType("STOCK")
        advanceUntilIdle()

        vm.resetFilters()
        advanceUntilIdle()

        val state = vm.state.value
        assertNull(state.filterStatus)
        assertNull(state.filterListingType)
        // poslednji refresh ide sa oba filtera = null
        coVerify { repository.myOrdersFiltered(status = null, listingType = null, dateFrom = any(), dateTo = any(), page = any(), size = any()) }
    }
}
