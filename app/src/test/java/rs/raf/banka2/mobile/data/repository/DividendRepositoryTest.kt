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
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.api.DividendApi
import rs.raf.banka2.mobile.data.dto.dividend.DividendPayoutDto
import java.math.BigDecimal

class DividendRepositoryTest {

    private val api = mockk<DividendApi>()
    private val repo = DividendRepository(api)

    @Test
    fun getMy_returnsSuccess() = runTest {
        val data = listOf(
            DividendPayoutDto(id = 1L, stockTicker = "AAPL", grossAmount = BigDecimal("100.0"), tax = BigDecimal("15.0"), netAmount = BigDecimal("85.0"), currencyCode = "USD"),
            DividendPayoutDto(id = 2L, stockTicker = "MSFT", grossAmount = BigDecimal("200.0"), tax = BigDecimal("30.0"), netAmount = BigDecimal("170.0"), currencyCode = "USD"),
        )
        coEvery { api.getMy() } returns Response.success(data)

        val result = repo.getMy()
        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
        assertEquals("AAPL", result.data[0].stockTicker)
        assertEquals(BigDecimal("85.0"), result.data[0].netAmount)
    }

    @Test
    fun getByPosition_returnsSuccess() = runTest {
        val data = listOf(
            DividendPayoutDto(id = 5L, stockTicker = "AAPL", grossAmount = BigDecimal("50.0"), tax = BigDecimal("7.5"), netAmount = BigDecimal("42.5"), currencyCode = "USD")
        )
        coEvery { api.getByPosition(42L) } returns Response.success(data)

        val result = repo.getByPosition(42L)
        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.size)
        assertEquals("AAPL", result.data[0].stockTicker)
    }

    @Test
    fun getByPosition_404_propagatesNotFound() = runTest {
        coEvery { api.getByPosition(any()) } returns
            Response.error(404, "Not Found".toResponseBody("text/plain".toMediaTypeOrNull()))

        val result = repo.getByPosition(99L)
        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.NotFound, (result as ApiResult.Failure).error.kind)
    }
}
