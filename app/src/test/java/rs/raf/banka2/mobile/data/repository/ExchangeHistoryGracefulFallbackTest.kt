package rs.raf.banka2.mobile.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.api.ExchangeApi
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeHistoryPointDto

/**
 * Mobile-bonus #5: graceful fallback za istoriju kursa.
 *
 * BE endpoint `GET /exchange/history` jos uvek mozda nije implementiran.
 * Repository mora hvata 404/501/405 i vraca Success(prazna lista) tako da
 * UI ne renderuje sparkline.
 */
class ExchangeHistoryGracefulFallbackTest {

    private val api = mockk<ExchangeApi>(relaxed = true)
    private val repo = ExchangeRepository(api)

    @Test
    fun history_200_returnsData() = runTest {
        val data = listOf(
            ExchangeHistoryPointDto("2026-04-25", 117.0),
            ExchangeHistoryPointDto("2026-05-25", 119.0)
        )
        coEvery { api.history(any(), any()) } returns Response.success(data)

        val result = repo.history("EUR", 30)
        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
    }

    @Test
    fun history_404_returnsEmptyList() = runTest {
        coEvery { api.history(any(), any()) } returns
            Response.error(404, "Not Found".toResponseBody("text/plain".toMediaTypeOrNull()))

        val result = repo.history("EUR", 30)
        // Mora biti Success sa praznom listom (graceful fallback).
        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    @Test
    fun history_501_returnsEmptyList() = runTest {
        coEvery { api.history(any(), any()) } returns
            Response.error(501, "Not Implemented".toResponseBody("text/plain".toMediaTypeOrNull()))

        val result = repo.history("EUR", 30)
        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    @Test
    fun history_500_propagatesFailure() = runTest {
        coEvery { api.history(any(), any()) } returns
            Response.error(500, "Server".toResponseBody("text/plain".toMediaTypeOrNull()))

        val result = repo.history("EUR", 30)
        // Pravi server error se i dalje propagira — UI moze prikazati error toast.
        assertTrue(result is ApiResult.Failure)
    }
}
