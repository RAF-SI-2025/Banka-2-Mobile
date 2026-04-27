package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.ClientApi
import rs.raf.banka2.mobile.data.dto.common.ClientDto
import rs.raf.banka2.mobile.data.dto.common.CreateClientRequestDto
import rs.raf.banka2.mobile.data.dto.common.UpdateClientRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val api: ClientApi
) {
    suspend fun list(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null
    ): ApiResult<List<ClientDto>> =
        safeApiCall { api.list(firstName = firstName, lastName = lastName, email = email) }
            .map { it.content }

    suspend fun byId(id: Long): ApiResult<ClientDto> = safeApiCall { api.byId(id) }

    suspend fun create(request: CreateClientRequestDto): ApiResult<ClientDto> =
        safeApiCall { api.create(request) }

    suspend fun update(id: Long, request: UpdateClientRequestDto): ApiResult<ClientDto> =
        safeApiCall { api.update(id, request) }
}
