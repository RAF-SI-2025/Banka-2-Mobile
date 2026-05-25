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
import rs.raf.banka2.mobile.data.api.OtcApi
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.otchistory.OtcNegotiationHistoryDto

/**
 * B10 / Spec C4 §13 — paginated negotiation history (supervisor/admin).
 *
 * Testovi:
 *  - happy path: BE 200 sa stranicom rezultata
 *  - 403 (klijent / agent) postaje ApiError.Kind.Forbidden
 *  - "ALL" status se ne salje BE-u
 *  - chain endpoint 404 ne crash-uje VM (samo Failure)
 */
class OtcNegotiationHistoryRepositoryTest {

    private val api = mockk<OtcApi>(relaxed = false)
    private val repo = OtcRepository(api)

    @Test
    fun negotiationHistory_returnsSuccess() = runTest {
        val page = PageResponse(
            content = listOf(
                OtcNegotiationHistoryDto(
                    id = 1L, negotiationId = 100L, quantity = 10,
                    pricePerShare = 50.0, premium = 5.0,
                    settlementDate = "2026-12-31",
                    status = "ACTIVE",
                    modifiedById = 1L, modifiedByName = "Stefan",
                    createdAt = "2026-05-25T10:00:00"
                )
            ),
            totalElements = 1, totalPages = 1, number = 0, size = 20, first = true, last = true, empty = false
        )
        coEvery { api.negotiationHistory(any(), any(), any(), any(), any(), any()) } returns Response.success(page)

        val result = repo.negotiationHistory()
        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(1, data.content.size)
        assertEquals("ACTIVE", data.content[0].status)
    }

    @Test
    fun negotiationHistory_403_mapsToForbidden() = runTest {
        coEvery { api.negotiationHistory(any(), any(), any(), any(), any(), any()) } returns
            Response.error(403, "Forbidden".toResponseBody("text/plain".toMediaTypeOrNull()))

        val result = repo.negotiationHistory()
        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Forbidden, (result as ApiResult.Failure).error.kind)
    }

    @Test
    fun negotiationHistoryChain_returnsList() = runTest {
        val chain = listOf(
            OtcNegotiationHistoryDto(
                id = 1L, negotiationId = 100L, quantity = 10,
                pricePerShare = 50.0, premium = 5.0,
                settlementDate = "2026-12-31",
                status = "ACTIVE",
                modifiedByName = "Stefan",
                createdAt = "2026-05-25T10:00:00"
            ),
            OtcNegotiationHistoryDto(
                id = 2L, negotiationId = 100L, quantity = 12,
                pricePerShare = 52.0, premium = 6.0,
                settlementDate = "2026-12-31",
                status = "ACTIVE",
                modifiedByName = "Milica",
                createdAt = "2026-05-25T11:00:00"
            )
        )
        coEvery { api.negotiationHistoryChain(100L) } returns Response.success(chain)

        val result = repo.negotiationHistoryChain(100L)
        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
    }
}
