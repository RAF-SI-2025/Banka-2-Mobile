package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.PriceAlertApi
import rs.raf.banka2.mobile.data.dto.pricealert.CreatePriceAlertRequest
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertCondition
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertDto
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [FE2 Mobile port] Wrapper oko [PriceAlertApi] koji vraca [ApiResult].
 */
@Singleton
class PriceAlertRepository @Inject constructor(
    private val api: PriceAlertApi,
) {
    suspend fun listMy(active: Boolean? = null): ApiResult<List<PriceAlertDto>> =
        safeApiCall { api.listMy(active) }

    suspend fun create(
        listingId: Long,
        condition: PriceAlertCondition,
        threshold: BigDecimal,
    ): ApiResult<PriceAlertDto> = safeApiCall {
        api.create(CreatePriceAlertRequest(listingId, condition.apiValue, threshold))
    }

    suspend fun delete(id: Long): ApiResult<Unit> =
        safeApiCall { api.delete(id) }
}
