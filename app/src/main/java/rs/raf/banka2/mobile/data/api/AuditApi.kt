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
    /**
     * BE: `@RequestMapping("/audit")` + `@GetMapping`, parametri
     * `actionType` / `actorId` (Long) / `actorName` (String) / `from` / `to`
     * (ISO LocalDateTime String) / `page` / `size`.
     *
     * Gateway rutira `/audit` -> trading-service. Akter se filtrira po `actorId`
     * (numericki ID) ILI `actorName` (ime aktera/supervizora — Sc45; BE razresi
     * ime -> actorId-eve preko banka-core). Ako su oba data, BE bira `actorId`.
     */
    @GET("audit")
    suspend fun queryAuditLogs(
        @Query("actionType") actionType: String? = null,
        @Query("actorId") actorId: Long? = null,
        @Query("actorName") actorName: String? = null,
        @Query("from") from: String? = null, // ISO LocalDateTime, npr. 2026-05-30T00:00:00
        @Query("to") to: String? = null,     // ISO LocalDateTime, npr. 2026-05-30T23:59:59
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<AuditLogDto>>
}
