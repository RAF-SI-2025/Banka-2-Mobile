package rs.raf.banka2.mobile.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.api.OrderApi
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.order.OrderDto

/**
 * C3 #7 — testovi filtera za istoriju ordera (MyOrders).
 *  - "ALL" status mora postati null pre slanja BE-u (BE razlikuje "no filter" od "ALL").
 *  - Blank datum stringovi se ne salju BE-u.
 *  - Validne vrednosti se proksuju nepromenjeno.
 */
class OrderRepositoryFiltersTest {

    private val api = mockk<OrderApi>()
    private val repo = OrderRepository(api)

    @Test
    fun myOrdersFiltered_allStatusIsNotSent() = runTest {
        coEvery {
            api.getMyOrders(
                page = any(),
                size = any(),
                status = null,
                dateFrom = null,
                dateTo = null,
                listingType = null
            )
        } returns Response.success(PageResponse<OrderDto>())

        val result = repo.myOrdersFiltered(status = "ALL", listingType = "ALL", dateFrom = "", dateTo = "")
        assertTrue(result is ApiResult.Success)
        coVerify {
            api.getMyOrders(
                page = 0,
                size = 50,
                status = null,
                dateFrom = null,
                dateTo = null,
                listingType = null
            )
        }
    }

    @Test
    fun myOrdersFiltered_validFiltersArePassed() = runTest {
        coEvery {
            api.getMyOrders(
                page = any(),
                size = any(),
                status = "PENDING",
                dateFrom = "2026-05-01",
                dateTo = "2026-05-25",
                listingType = "STOCK"
            )
        } returns Response.success(PageResponse<OrderDto>())

        val result = repo.myOrdersFiltered(
            status = "PENDING",
            listingType = "STOCK",
            dateFrom = "2026-05-01",
            dateTo = "2026-05-25"
        )
        assertTrue(result is ApiResult.Success)
        coVerify {
            api.getMyOrders(
                page = 0,
                size = 50,
                status = "PENDING",
                dateFrom = "2026-05-01",
                dateTo = "2026-05-25",
                listingType = "STOCK"
            )
        }
    }
}
