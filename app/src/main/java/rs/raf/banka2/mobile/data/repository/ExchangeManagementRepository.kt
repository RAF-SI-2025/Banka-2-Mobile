package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.ExchangeManagementApi
import rs.raf.banka2.mobile.data.dto.listing.ExchangeManagementDto
import rs.raf.banka2.mobile.data.dto.listing.ToggleTestModeDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeManagementRepository @Inject constructor(
    private val api: ExchangeManagementApi
) {
    suspend fun list(): ApiResult<List<ExchangeManagementDto>> = safeApiCall { api.listExchanges() }

    suspend fun toggleTestMode(acronym: String, enabled: Boolean): ApiResult<ExchangeManagementDto> =
        safeApiCall { api.toggleTestMode(acronym, ToggleTestModeDto(enabled)) }
}
