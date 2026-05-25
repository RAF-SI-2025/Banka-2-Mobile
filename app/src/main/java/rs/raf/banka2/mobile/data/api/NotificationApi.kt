package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.notification.NotificationDto
import rs.raf.banka2.mobile.data.dto.notification.NotificationPageDto
import rs.raf.banka2.mobile.data.dto.notification.UnreadCountDto

/**
 * TODO_final C2 #4 — In-app notifikacije.
 *
 * Endpoint-i prate FE `notificationService` (`/notifications`,
 * `/notifications/unread-count`, `/notifications/{id}/read`,
 * `/notifications/read-all`). Filter mode se mapira u BE query param `read`
 * (true/false). FE koristi paginaciju (`page` + `size`).
 */
interface NotificationApi {

    /**
     * @param read null = sve; true = procitane; false = neprocitane.
     */
    @GET("notifications")
    suspend fun list(
        @Query("read") read: Boolean? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): Response<NotificationPageDto>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountDto>

    @PATCH("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: Long): Response<NotificationDto>

    @PATCH("notifications/read-all")
    suspend fun markAllAsRead(): Response<Unit>
}
