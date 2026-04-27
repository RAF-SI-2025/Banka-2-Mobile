package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.RecipientApi
import rs.raf.banka2.mobile.data.dto.recipient.CreateRecipientDto
import rs.raf.banka2.mobile.data.dto.recipient.RecipientDto
import rs.raf.banka2.mobile.data.dto.recipient.UpdateRecipientDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipientRepository @Inject constructor(
    private val api: RecipientApi
) {
    suspend fun list(): ApiResult<List<RecipientDto>> =
        safeApiCall { api.list() }.map { it.content }

    suspend fun create(name: String, accountNumber: String, description: String?): ApiResult<RecipientDto> =
        safeApiCall { api.create(CreateRecipientDto(name, accountNumber, description)) }

    suspend fun update(id: Long, name: String, accountNumber: String, description: String?): ApiResult<RecipientDto> =
        safeApiCall { api.update(id, UpdateRecipientDto(name, accountNumber, description)) }

    suspend fun delete(id: Long): ApiResult<Unit> = safeApiCall { api.delete(id) }.map { }
}
