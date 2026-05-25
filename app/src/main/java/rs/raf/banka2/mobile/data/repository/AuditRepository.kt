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
 */
@Singleton
class AuditRepository @Inject constructor(
    private val api: AuditApi
) {
    suspend fun query(
        actionType: String? = null,
        actorId: Long? = null,
        actorEmail: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        page: Int = 0,
        size: Int = 20
    ): ApiResult<PageResponse<AuditLogDto>> =
        safeApiCall {
            api.queryAuditLogs(
                actionType = actionType?.takeIf { it.isNotBlank() },
                actorId = actorId,
                actorEmail = actorEmail?.takeIf { it.isNotBlank() },
                dateFrom = dateFrom?.takeIf { it.isNotBlank() },
                dateTo = dateTo?.takeIf { it.isNotBlank() },
                page = page,
                size = size
            )
        }
}
