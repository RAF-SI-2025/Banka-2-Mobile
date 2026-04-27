package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.EmployeeAdminApi
import rs.raf.banka2.mobile.data.dto.common.CreateEmployeeRequestDto
import rs.raf.banka2.mobile.data.dto.common.EmployeeDto
import rs.raf.banka2.mobile.data.dto.common.UpdateEmployeePermissionsDto
import rs.raf.banka2.mobile.data.dto.common.UpdateEmployeeRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmployeeAdminRepository @Inject constructor(
    private val api: EmployeeAdminApi
) {
    suspend fun list(
        email: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        position: String? = null
    ): ApiResult<List<EmployeeDto>> =
        safeApiCall { api.list(email = email, firstName = firstName, lastName = lastName, position = position) }
            .map { it.content }

    suspend fun byId(id: Long): ApiResult<EmployeeDto> = safeApiCall { api.byId(id) }

    suspend fun create(request: CreateEmployeeRequestDto): ApiResult<EmployeeDto> =
        safeApiCall { api.create(request) }

    suspend fun update(id: Long, request: UpdateEmployeeRequestDto): ApiResult<EmployeeDto> =
        safeApiCall { api.update(id, request) }

    suspend fun deactivate(id: Long): ApiResult<EmployeeDto> = safeApiCall { api.deactivate(id) }

    suspend fun getPermissions(id: Long): ApiResult<List<String>> =
        safeApiCall { api.getPermissions(id) }

    suspend fun updatePermissions(
        id: Long,
        isAgent: Boolean,
        isSupervisor: Boolean,
        isAdmin: Boolean? = null
    ): ApiResult<EmployeeDto> = safeApiCall {
        api.updatePermissions(id, UpdateEmployeePermissionsDto(isAgent, isSupervisor, isAdmin))
    }
}
