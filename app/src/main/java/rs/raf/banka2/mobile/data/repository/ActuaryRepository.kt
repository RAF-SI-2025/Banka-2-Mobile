package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.ActuaryApi
import rs.raf.banka2.mobile.data.dto.actuary.ActuaryDto
import rs.raf.banka2.mobile.data.dto.actuary.UpdateActuaryLimitDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActuaryRepository @Inject constructor(
    private val api: ActuaryApi
) {
    suspend fun listAgents(): ApiResult<List<ActuaryDto>> = safeApiCall { api.listAgents() }

    suspend fun get(employeeId: Long): ApiResult<ActuaryDto> = safeApiCall { api.getActuary(employeeId) }

    suspend fun updateLimit(employeeId: Long, dailyLimit: Double, needApproval: Boolean): ApiResult<ActuaryDto> =
        safeApiCall { api.updateLimit(employeeId, UpdateActuaryLimitDto(dailyLimit, needApproval)) }

    suspend fun resetLimit(employeeId: Long): ApiResult<ActuaryDto> =
        safeApiCall { api.resetLimit(employeeId) }
}
