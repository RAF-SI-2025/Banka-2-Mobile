package rs.raf.banka2.mobile.data.repository

import retrofit2.Response
import rs.raf.banka2.mobile.core.network.ApiError
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
 *
 * INTER-BANK 501 GUARD: BE T2 (outbound) i T3 (inbound) jos nisu
 * potpuno mergovani na main — OtcNegotiationController vraca 501 za
 * /negotiations rute dok cherry-pick PR #72 ne uleti i T3 ne stigne.
 * Sve metode koje gadjaju inter-bank rute imaju graceful fallback: kad
 * BE vrati 501 (Not Implemented), repository to pretvara u prazan
 * Success(emptyList()) za list-ove ili Failure sa user-friendly
 * porukom za akcije, da UI moze da prikaze placeholder umesto crash-a.
 */
@Singleton
class OtcRepository @Inject constructor(
    private val api: OtcApi
) {
    suspend fun discover(inter: Boolean): ApiResult<List<OtcListingDto>> =
        if (inter) gracefulList { api.discoverInter() } else safeApiCall { api.discoverIntra() }

    suspend fun createOffer(inter: Boolean, request: CreateOtcOfferDto): ApiResult<OtcOfferDto> =
        if (inter) gracefulAction { api.createInterOffer(request) }
        else safeApiCall { api.createIntraOffer(request) }

    suspend fun listOffers(inter: Boolean): ApiResult<List<OtcOfferDto>> =
        if (inter) gracefulList { api.listInterOffers() } else safeApiCall { api.listIntraOffers() }

    suspend fun counter(inter: Boolean, offerId: Long, body: CounterOtcOfferDto): ApiResult<OtcOfferDto> =
        if (inter) gracefulAction { api.counterInter(offerId, body) }
        else safeApiCall { api.counterIntra(offerId, body) }

    suspend fun decline(inter: Boolean, offerId: Long): ApiResult<OtcOfferDto> =
        if (inter) gracefulAction { api.declineInter(offerId) }
        else safeApiCall { api.declineIntra(offerId) }

    suspend fun accept(inter: Boolean, offerId: Long, buyerAccountId: Long?): ApiResult<OtcOfferDto> {
        val body = AcceptOtcOfferDto(buyerAccountId)
        return if (inter) gracefulAction { api.acceptInter(offerId, body) }
        else safeApiCall { api.acceptIntra(offerId, body) }
    }

    suspend fun listContracts(inter: Boolean, status: String? = null): ApiResult<List<OtcContractDto>> =
        if (inter) gracefulList { api.listInterContracts(status) }
        else safeApiCall { api.listIntraContracts(status) }

    suspend fun exercise(inter: Boolean, contractId: Long, buyerAccountId: Long?): ApiResult<OtcContractDto> {
        val body = ExerciseRequestDto(buyerAccountId)
        return if (inter) gracefulAction { api.exerciseInter(contractId, body) }
        else safeApiCall { api.exerciseIntra(contractId, body) }
    }

    suspend fun sagaStatus(contractId: Long): ApiResult<SagaStatusDto> =
        safeApiCall { api.sagaStatus(contractId) }

    /**
     * Inter-bank list endpoint sa 501 fallback-om — vraca prazan list ako
     * BE jos nije implementiran (T2/T3). UI prikazuje EmptyState umesto greske.
     */
    private suspend fun <T> gracefulList(block: suspend () -> Response<List<T>>): ApiResult<List<T>> {
        val result: ApiResult<List<T>> = safeApiCall { block() }
        if (result is ApiResult.Failure && isNotImplemented(result.error)) {
            return ApiResult.Success(emptyList<T>())
        }
        return result
    }

    /**
     * Inter-bank akcija sa 501 fallback-om — vraca user-friendly Failure ako
     * BE jos nije implementiran. UI moze da prikaze toast sa porukom.
     */
    private suspend fun <T> gracefulAction(block: suspend () -> Response<T>): ApiResult<T> {
        val result = safeApiCall { block() }
        if (result is ApiResult.Failure && isNotImplemented(result.error)) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 501,
                    message = "Inter-bank OTC jos nije dostupan — backend tim radi na implementaciji.",
                    kind = ApiError.Kind.Server,
                    cause = result.error.cause
                )
            )
        }
        return result
    }

    private fun isNotImplemented(error: ApiError): Boolean = error.httpCode == 501
}
