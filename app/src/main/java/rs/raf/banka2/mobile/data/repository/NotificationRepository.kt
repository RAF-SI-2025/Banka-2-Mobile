package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.NotificationApi
import rs.raf.banka2.mobile.data.dto.notification.NotificationDto
import rs.raf.banka2.mobile.data.dto.notification.NotificationFilter
import rs.raf.banka2.mobile.data.dto.notification.NotificationPageDto
import rs.raf.banka2.mobile.data.dto.notification.UnreadCountDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO_final C2 #4 — In-app notifikacije.
 *
 * Mobile repository iznad [NotificationApi]. Sve metode vracaju [ApiResult]
 * i ne bacaju izuzetke — paralelno sa SavingsRepository i drugim repo-ima.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val api: NotificationApi,
) {

    suspend fun list(
        filter: NotificationFilter,
        page: Int = 0,
        size: Int = 20,
    ): ApiResult<NotificationPageDto> {
        // P1-mobile-banking-1 (R3-1626): BE param je `onlyUnread` (true=neprocitane);
        // za ALL ga izostavljamo (null) da bismo dobili sve.
        val onlyUnread: Boolean? = when (filter) {
            NotificationFilter.ALL -> null
            NotificationFilter.UNREAD -> true
        }
        return safeApiCall { api.list(onlyUnread = onlyUnread, page = page, size = size) }
    }

    suspend fun getUnreadCount(): ApiResult<UnreadCountDto> =
        safeApiCall { api.getUnreadCount() }

    suspend fun markAsRead(id: Long): ApiResult<NotificationDto> =
        safeApiCall { api.markAsRead(id) }

    suspend fun markAllAsRead(): ApiResult<Unit> =
        safeApiCall { api.markAllAsRead() }
}
