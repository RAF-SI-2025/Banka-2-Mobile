package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.AuditApi
import rs.raf.banka2.mobile.data.dto.audit.AuditLogDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audit log repository — B7 / Spec C3 §69. Supervisor/admin only.
 * BE vraca 403 ako poziv stigne sa drugacijim profilom.
 *
 * NAPOMENA: gateway rutira `/audit` -> trading-service. Akter se filtrira po
 * `actorId` (numericki ID) ILI `actorName` (ime aktera/supervizora — Sc45; BE
 * razresi ime -> actorId-eve preko banka-core). Datumi (`YYYY-MM-DD`) se
 * konvertuju u ISO `LocalDateTime` String (`from` = pocetak dana, `to` = kraj
 * dana) koji BE parsira sa `LocalDateTime.parse(...)`.
 */
@Singleton
class AuditRepository @Inject constructor(
    private val api: AuditApi
) {
    suspend fun query(
        actionType: String? = null,
        actorId: Long? = null,
        actorName: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        page: Int = 0,
        size: Int = 20
    ): ApiResult<PageResponse<AuditLogDto>> =
        safeApiCall {
            api.queryAuditLogs(
                actionType = actionType?.takeIf { it.isNotBlank() },
                actorId = actorId,
                actorName = actorName?.takeIf { it.isNotBlank() },
                from = toStartOfDayIso(dateFrom),
                to = toEndOfDayIso(dateTo),
                page = page,
                size = size
            )
        }

    private fun toStartOfDayIso(date: String?): String? =
        date?.takeIf { it.isNotBlank() }?.let { "${it}T00:00:00" }

    private fun toEndOfDayIso(date: String?): String? =
        date?.takeIf { it.isNotBlank() }?.let { "${it}T23:59:59" }
}
