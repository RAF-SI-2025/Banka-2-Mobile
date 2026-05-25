package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.audit.AuditLogDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse

/**
 * Audit log API — Spec C3 §69 / B7 backend zadatak.
 * Dostupno samo ADMIN + SUPERVISOR rolama (BE vraca 403 inace).
 */
interface AuditApi {
    @GET("audit-logs")
    suspend fun queryAuditLogs(
        @Query("actionType") actionType: String? = null,
        @Query("actorId") actorId: Long? = null,
        @Query("actorEmail") actorEmail: String? = null,
        @Query("dateFrom") dateFrom: String? = null, // ISO YYYY-MM-DD
        @Query("dateTo") dateTo: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<AuditLogDto>>
}
