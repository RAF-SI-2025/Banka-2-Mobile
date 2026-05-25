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
import rs.raf.banka2.mobile.data.api.PriceAlertApi
import rs.raf.banka2.mobile.data.dto.pricealert.CreatePriceAlertRequest
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertCondition
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertDto
import java.math.BigDecimal

/**
 * Unit testovi za PriceAlertRepository.
 */
class PriceAlertRepositoryTest {

    private val api = mockk<PriceAlertApi>()
    private val repo = PriceAlertRepository(api)

    @Test
    fun create_sendsConditionApiValueAsString() = runTest {
        val expectedReq = CreatePriceAlertRequest(
            listingId = 1L,
            condition = "ABOVE",
            threshold = BigDecimal("150.00"),
        )
        val resp = PriceAlertDto(
            id = 1L, listingId = 1L, listingTicker = "AAPL",
            condition = "ABOVE", threshold = BigDecimal("150.00"),
            active = true,
        )
        coEvery { api.create(expectedReq) } returns Response.success(resp)

        val result = repo.create(1L, PriceAlertCondition.ABOVE, BigDecimal("150.00"))
        assertTrue(result is ApiResult.Success)
        assertEquals("ABOVE", (result as ApiResult.Success).data.condition)
    }

    @Test
    fun listMy_returnsFlatList() = runTest {
        val alerts = listOf(
            PriceAlertDto(id = 1, listingId = 1, listingTicker = "AAPL", condition = "ABOVE", threshold = BigDecimal("150"), active = true),
            PriceAlertDto(id = 2, listingId = 2, listingTicker = "MSFT", condition = "BELOW", threshold = BigDecimal("200"), active = false),
        )
        coEvery { api.listMy(null) } returns Response.success(alerts)

        val result = repo.listMy(active = null)
        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
    }

    @Test
    fun listMy_active_true_sendsActiveFilter() = runTest {
        coEvery { api.listMy(true) } returns Response.success(emptyList())
        val result = repo.listMy(active = true)
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun delete_204_isSuccess() = runTest {
        coEvery { api.delete(5L) } returns Response.success(Unit)
        val result = repo.delete(5L)
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun create_400_validationFailure_mapsToValidationError() = runTest {
        val body = """{"message":"threshold must be > 0"}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        coEvery { api.create(any()) } returns Response.error(400, body)

        val result = repo.create(1L, PriceAlertCondition.ABOVE, BigDecimal("-1"))
        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Validation, (result as ApiResult.Failure).error.kind)
    }
}
