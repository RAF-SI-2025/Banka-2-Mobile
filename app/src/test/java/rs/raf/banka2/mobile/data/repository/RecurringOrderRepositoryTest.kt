package rs.raf.banka2.mobile.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.api.RecurringOrderApi
import rs.raf.banka2.mobile.data.dto.recurringorder.CreateRecurringOrderRequest
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringCadence
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringDirection
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringMode
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringOrderDto
import java.math.BigDecimal

/**
 * Unit testovi za RecurringOrderRepository — mapiranje enum-a u API string vrednosti.
 */
class RecurringOrderRepositoryTest {

    private val api = mockk<RecurringOrderApi>()
    private val repo = RecurringOrderRepository(api)

    @Test
    fun create_mapsAllEnumsToApiValues() = runTest {
        val expectedReq = CreateRecurringOrderRequest(
            listingId = 10L,
            direction = "BUY",
            mode = "BYAMOUNT",
            value = BigDecimal("100"),
            accountId = 5L,
            cadence = "MONTHLY",
            firstRun = null,
        )
        val response = RecurringOrderDto(
            id = 1L, listingId = 10L, listingTicker = "AAPL",
            direction = "BUY", mode = "BYAMOUNT", value = BigDecimal("100"),
            accountId = 5L, cadence = "MONTHLY", active = true,
        )
        coEvery { api.create(expectedReq) } returns Response.success(response)

        val result = repo.create(
            listingId = 10L,
            direction = RecurringDirection.BUY,
            mode = RecurringMode.BYAMOUNT,
            value = BigDecimal("100"),
            accountId = 5L,
            cadence = RecurringCadence.MONTHLY,
        )
        assertTrue(result is ApiResult.Success)
        assertEquals("BUY", (result as ApiResult.Success).data.direction)
        assertEquals("MONTHLY", result.data.cadence)
    }

    @Test
    fun create_sellByQuantityWeekly_mapsCorrectly() = runTest {
        val expectedReq = CreateRecurringOrderRequest(
            listingId = 20L, direction = "SELL", mode = "BYQUANTITY",
            value = BigDecimal("3"), accountId = 7L, cadence = "WEEKLY",
            firstRun = null,
        )
        val response = RecurringOrderDto(
            id = 2L, listingId = 20L, direction = "SELL", mode = "BYQUANTITY",
            value = BigDecimal("3"), accountId = 7L, cadence = "WEEKLY", active = true,
        )
        coEvery { api.create(expectedReq) } returns Response.success(response)

        val result = repo.create(
            listingId = 20L, direction = RecurringDirection.SELL,
            mode = RecurringMode.BYQUANTITY, value = BigDecimal("3"),
            accountId = 7L, cadence = RecurringCadence.WEEKLY,
        )
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun pause_returnsUpdatedOrder() = runTest {
        val paused = RecurringOrderDto(
            id = 1L, listingId = 10L, direction = "BUY", mode = "BYAMOUNT",
            value = BigDecimal("100"), accountId = 5L, cadence = "MONTHLY",
            active = false,
        )
        coEvery { api.pause(1L) } returns Response.success(paused)

        val result = repo.pause(1L)
        assertTrue(result is ApiResult.Success)
        assertEquals(false, (result as ApiResult.Success).data.active)
    }

    @Test
    fun resume_returnsActiveOrder() = runTest {
        val resumed = RecurringOrderDto(
            id = 1L, listingId = 10L, direction = "BUY", mode = "BYAMOUNT",
            value = BigDecimal("100"), accountId = 5L, cadence = "MONTHLY",
            active = true,
        )
        coEvery { api.resume(1L) } returns Response.success(resumed)

        val result = repo.resume(1L)
        assertTrue(result is ApiResult.Success)
        assertEquals(true, (result as ApiResult.Success).data.active)
    }

    @Test
    fun cancel_204_isSuccess() = runTest {
        coEvery { api.cancel(1L) } returns Response.success(Unit)
        val result = repo.cancel(1L)
        assertTrue(result is ApiResult.Success)
    }
}
