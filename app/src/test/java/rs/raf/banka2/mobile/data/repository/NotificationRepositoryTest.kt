package rs.raf.banka2.mobile.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.api.NotificationApi
import rs.raf.banka2.mobile.data.dto.notification.NotificationDto
import rs.raf.banka2.mobile.data.dto.notification.NotificationFilter
import rs.raf.banka2.mobile.data.dto.notification.NotificationPageDto
import rs.raf.banka2.mobile.data.dto.notification.UnreadCountDto

/**
 * TODO_final C2 #4 — NotificationRepository test:
 * - filter ALL vraca onlyUnread=null query param
 * - filter UNREAD vraca onlyUnread=true query param (P1-mobile-banking-1 R3-1626:
 *   BE cita `onlyUnread`, NE `read` — ranije je UNREAD vracao SVE)
 * - 200 wrap u ApiResult.Success
 * - greska iz API-ja propagira do ApiResult.Failure
 */
class NotificationRepositoryTest {

    private val api = mockk<NotificationApi>()
    private val repo = NotificationRepository(api)

    @Test
    fun `list ALL passes onlyUnread=null query param`() = runTest {
        val page = NotificationPageDto(
            content = listOf(
                NotificationDto(
                    id = 1, type = "PAYMENT", title = "T", message = "M",
                    read = false, createdAt = "2026-05-26T10:00:00Z"
                )
            ),
            totalElements = 1, totalPages = 1, number = 0, size = 20
        )
        coEvery { api.list(onlyUnread = null, page = 0, size = 20) } returns Response.success(page)

        val result = repo.list(NotificationFilter.ALL)

        assertTrue(result is ApiResult.Success)
        result as ApiResult.Success
        assertEquals(1, result.data.content.size)
        coVerify { api.list(onlyUnread = null, page = 0, size = 20) }
    }

    @Test
    fun `list UNREAD passes onlyUnread=true query param`() = runTest {
        val page = NotificationPageDto(content = emptyList(), totalPages = 0)
        coEvery { api.list(onlyUnread = true, page = 0, size = 20) } returns Response.success(page)

        repo.list(NotificationFilter.UNREAD)

        coVerify { api.list(onlyUnread = true, page = 0, size = 20) }
    }

    @Test
    fun `getUnreadCount returns count on success`() = runTest {
        coEvery { api.getUnreadCount() } returns Response.success(UnreadCountDto(count = 5))

        val result = repo.getUnreadCount()
        assertTrue(result is ApiResult.Success)
        result as ApiResult.Success
        assertEquals(5, result.data.count)
    }

    @Test
    fun `markAsRead invokes API and returns updated dto`() = runTest {
        val updated = NotificationDto(
            id = 1, type = "GENERIC", title = "t", message = "m",
            read = true, createdAt = "2026-05-26T10:00:00Z"
        )
        coEvery { api.markAsRead(1L) } returns Response.success(updated)

        val result = repo.markAsRead(1L)
        assertTrue(result is ApiResult.Success)
        result as ApiResult.Success
        assertEquals(true, result.data.read)
        coVerify { api.markAsRead(1L) }
    }

    @Test
    fun `markAllAsRead invokes API`() = runTest {
        coEvery { api.markAllAsRead() } returns Response.success(Unit)

        val result = repo.markAllAsRead()
        assertTrue(result is ApiResult.Success)
        coVerify { api.markAllAsRead() }
    }
}
