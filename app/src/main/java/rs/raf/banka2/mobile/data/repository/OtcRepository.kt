package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.OtcApi
import rs.raf.banka2.mobile.data.dto.otc.AcceptOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.ExerciseRequestDto
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcListingDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Jedan repository pokriva intra i inter OTC — flag `inter: Boolean` bira
 * ka kojem endpoint-u ide poziv. Tako UI sloj ne mora da zna razliku.
 */
@Singleton
class OtcRepository @Inject constructor(
    private val api: OtcApi
) {
    suspend fun discover(inter: Boolean): ApiResult<List<OtcListingDto>> = safeApiCall {
        if (inter) api.discoverInter() else api.discoverIntra()
    }

    suspend fun createOffer(inter: Boolean, request: CreateOtcOfferDto): ApiResult<OtcOfferDto> = safeApiCall {
        if (inter) api.createInterOffer(request) else api.createIntraOffer(request)
    }

    suspend fun listOffers(inter: Boolean): ApiResult<List<OtcOfferDto>> = safeApiCall {
        if (inter) api.listInterOffers() else api.listIntraOffers()
    }

    suspend fun counter(inter: Boolean, offerId: Long, body: CounterOtcOfferDto): ApiResult<OtcOfferDto> = safeApiCall {
        if (inter) api.counterInter(offerId, body) else api.counterIntra(offerId, body)
    }

    suspend fun decline(inter: Boolean, offerId: Long): ApiResult<OtcOfferDto> = safeApiCall {
        if (inter) api.declineInter(offerId) else api.declineIntra(offerId)
    }

    suspend fun accept(inter: Boolean, offerId: Long, buyerAccountId: Long?): ApiResult<OtcOfferDto> = safeApiCall {
        val body = AcceptOtcOfferDto(buyerAccountId)
        if (inter) api.acceptInter(offerId, body) else api.acceptIntra(offerId, body)
    }

    suspend fun listContracts(inter: Boolean, status: String? = null): ApiResult<List<OtcContractDto>> = safeApiCall {
        if (inter) api.listInterContracts(status) else api.listIntraContracts(status)
    }

    suspend fun exercise(inter: Boolean, contractId: Long, buyerAccountId: Long?): ApiResult<OtcContractDto> =
        safeApiCall {
            val body = ExerciseRequestDto(buyerAccountId)
            if (inter) api.exerciseInter(contractId, body) else api.exerciseIntra(contractId, body)
        }

    suspend fun sagaStatus(contractId: Long): ApiResult<SagaStatusDto> =
        safeApiCall { api.sagaStatus(contractId) }
}
