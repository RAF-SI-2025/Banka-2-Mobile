package rs.raf.banka2.mobile.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
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
import rs.raf.banka2.mobile.data.api.AuditApi
import rs.raf.banka2.mobile.data.dto.audit.AuditLogDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse

/**
 * Verifikuje:
 *  - Successful response postaje ApiResult.Success
 *  - 403 (BE odbija ne-supervisor) postaje ApiError.Kind.Forbidden
 *  - Blank filteri se NE prosledjuju BE-u (`takeIf { isNotBlank() }`)
 */
class AuditRepositoryTest {

    private val api = mockk<AuditApi>()
    private val repo = AuditRepository(api)

    @Test
    fun query_returnsSuccessOnHappyPath() = runTest {
        val page = PageResponse(
            content = listOf(
                AuditLogDto(id = 1L, actionType = "ORDER_APPROVED", actorId = 100L, createdAt = "2026-05-25T10:00:00")
            ),
            totalElements = 1, totalPages = 1, number = 0, size = 20, first = true, last = true, empty = false
        )
        coEvery { api.queryAuditLogs(any(), any(), any(), any(), any(), any(), any()) } returns Response.success(page)

        val result = repo.query()
        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(1, data.content.size)
        assertEquals("ORDER_APPROVED", data.content[0].actionType)
    }

    @Test
    fun query_403_mapsToForbidden() = runTest {
        coEvery { api.queryAuditLogs(any(), any(), any(), any(), any(), any(), any()) } returns
            Response.error(403, "Forbidden".toResponseBody("text/plain".toMediaTypeOrNull()))

        val result = repo.query()
        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Forbidden, (result as ApiResult.Failure).error.kind)
    }

    @Test
    fun query_blankFilters_arePassedAsNull() = runTest {
        coEvery {
            api.queryAuditLogs(
                actionType = null,
                actorId = null,
                actorEmail = null,
                dateFrom = null,
                dateTo = null,
                page = any(),
                size = any()
            )
        } returns Response.success(PageResponse<AuditLogDto>())

        val result = repo.query(actionType = "", actorEmail = "  ", dateFrom = "", dateTo = "")
        // Ovaj mock matchuje SAMO ako se svi filteri sasase u null. Ako repository
        // prosledi prazne stringove, mock se ne aktivira i test ce pasti.
        assertTrue(result is ApiResult.Success)
        coVerify {
            api.queryAuditLogs(
                actionType = null,
                actorId = null,
                actorEmail = null,
                dateFrom = null,
                dateTo = null,
                page = 0,
                size = 20
            )
        }
    }
}
