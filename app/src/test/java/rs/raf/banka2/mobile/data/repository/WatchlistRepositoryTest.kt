package rs.raf.banka2.mobile.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.api.WatchlistApi
import rs.raf.banka2.mobile.data.dto.watchlist.CreateWatchlistRequest
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistDto
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistFilterType
import rs.raf.banka2.mobile.data.dto.watchlist.WatchlistItemDto
import java.math.BigDecimal

/**
 * Unit testovi za WatchlistRepository — verifikuju da:
 *  - successful response postaje ApiResult.Success
 *  - HTTP 409 (Conflict) postaje ApiError.Kind.Conflict (kriticno za UX poruke)
 *  - filter ALL salje null `type` query param (BE listItems vraca sve)
 *  - filter STOCK salje "STOCK" query param
 */
class WatchlistRepositoryTest {

    private val api = mockk<WatchlistApi>()
    private val repo = WatchlistRepository(api)

    @Test
    fun listMyWatchlists_returnsSuccessOnHappyPath() = runTest {
        val data = listOf(
            WatchlistDto(id = 1, name = "Tech", itemCount = 3),
            WatchlistDto(id = 2, name = "Crypto", itemCount = 5),
        )
        coEvery { api.listMy() } returns Response.success(data)

        val result = repo.listMyWatchlists()
        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
        assertEquals("Tech", result.data[0].name)
    }

    @Test
    fun create_returnsSuccess() = runTest {
        val newList = WatchlistDto(id = 10, name = "Banks", itemCount = 0)
        coEvery { api.create(CreateWatchlistRequest("Banks")) } returns Response.success(newList)

        val result = repo.create("Banks")
        assertTrue(result is ApiResult.Success)
        assertEquals(10L, (result as ApiResult.Success).data.id)
    }

    @Test
    fun addItem_409Conflict_mapsToConflictApiError() = runTest {
        val errorBody = """{"message":"already in watchlist"}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        coEvery { api.addItem(1L, 99L) } returns Response.error(409, errorBody)

        val result = repo.addItem(1L, 99L)
        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Conflict, error.kind)
        assertEquals(409, error.httpCode)
    }

    @Test
    fun listItems_filterAll_sendsNullTypeParam() = runTest {
        val items = listOf(
            WatchlistItemDto(
                id = 1, watchlistId = 1, listingId = 100, ticker = "AAPL",
                currentPrice = BigDecimal("180.50"),
            )
        )
        coEvery { api.listItems(1L, null) } returns Response.success(items)

        val result = repo.listItems(1L, WatchlistFilterType.ALL)
        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.size)
    }

    @Test
    fun listItems_filterStock_sendsSTOCKTypeParam() = runTest {
        val items = listOf(
            WatchlistItemDto(
                id = 2, watchlistId = 1, listingId = 200, ticker = "MSFT",
                securityType = "STOCK",
            )
        )
        coEvery { api.listItems(1L, "STOCK") } returns Response.success(items)

        val result = repo.listItems(1L, WatchlistFilterType.STOCK)
        assertTrue(result is ApiResult.Success)
        assertEquals("MSFT", (result as ApiResult.Success).data[0].ticker)
    }

    @Test
    fun removeItem_usesListingIdNotItemId() = runTest {
        // Kriticno: BE brise po listingId, NE itemId. Test verifikuje da repo
        // prosledjuje pravi parametar.
        coEvery { api.removeItem(1L, 500L) } returns Response.success(Unit)

        val result = repo.removeItem(watchlistId = 1L, listingId = 500L)
        assertTrue(result is ApiResult.Success)
    }
}
