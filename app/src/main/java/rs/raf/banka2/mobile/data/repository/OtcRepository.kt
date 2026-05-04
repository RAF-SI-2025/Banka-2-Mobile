package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.OtcApi
import rs.raf.banka2.mobile.data.dto.otc.AcceptOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcInterbankOfferRequest
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcInterbankOfferRequest
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.ExerciseRequestDto
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankContractApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankListingApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankOfferApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcListingDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto
import javax.inject.Inject
import javax.inject.Singleton

// Step 1: full intra-bank only — KSP test
@Singleton
class OtcRepository @Inject constructor(
    private val api: OtcApi
) {
    suspend fun discover(inter: Boolean): ApiResult<List<OtcListingDto>> = if (inter) {
        safeApiCall { api.discoverInter() }.map { list -> list.map { it.toUiListing() } }
    } else {
        safeApiCall { api.discoverIntra() }
    }

    private fun OtcInterbankListingApiDto.toUiListing(): OtcListingDto = OtcListingDto(
        listingId = ("$bankCode:$sellerPublicId:$listingTicker").hashCode().toLong(),
        ticker = listingTicker,
        name = listingName,
        sellerUserId = null,
        sellerName = sellerName,
        sellerRole = sellerRole,
        publicQuantity = availableQuantity.toInt(),
        currentPrice = currentPrice,
        currency = listingCurrency,
        bankRoutingNumber = bankCode,
        foreign = true,
        foreignSellerPublicId = sellerPublicId,
        foreignBankCode = bankCode
    )
    suspend fun createOffer(inter: Boolean, request: CreateOtcOfferDto): ApiResult<OtcOfferDto> {
        if (!inter) return safeApiCall { api.createIntraOffer(request) }
        val sellerBankCode = request.bankRoutingNumber
        val sellerPublicId = request.foreignSellerPublicId ?: request.sellerUserId?.toString()
        val ticker = request.foreignListingTicker
        if (sellerBankCode.isNullOrBlank() || sellerPublicId.isNullOrBlank() || ticker.isNullOrBlank()) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Nedostaju bankCode/sellerPublicId/ticker za inter-bank ponudu.",
                    kind = ApiError.Kind.Validation
                )
            )
        }
        val interReq = CreateOtcInterbankOfferRequest(
            sellerBankCode = sellerBankCode,
            sellerUserId = sellerPublicId,
            listingTicker = ticker,
            quantity = request.quantity.toDouble(),
            pricePerStock = request.pricePerStock,
            premium = request.premium,
            settlementDate = request.settlementDate
        )
        return safeApiCall { api.createInterOffer(interReq) }.map { it.toUiOffer() }
    }
    suspend fun listOffers(inter: Boolean): ApiResult<List<OtcOfferDto>> = if (inter) {
        safeApiCall { api.listMyInterOffers() }.map { list -> list.map { it.toUiOffer() } }
    } else {
        safeApiCall { api.listIntraOffers() }
    }

    private fun OtcInterbankOfferApiDto.toUiOffer(): OtcOfferDto = OtcOfferDto(
        id = offerId.hashCode().toLong(),
        listingId = 0L,
        listingTicker = listingTicker,
        listingName = listingName,
        currentPrice = currentPrice,
        currency = listingCurrency,
        quantity = quantity.toInt(),
        pricePerStock = pricePerStock,
        premium = premium,
        settlementDate = settlementDate,
        status = status,
        waitingOnUserId = null,
        waitingOnRole = null,
        buyerName = buyerName,
        sellerName = sellerName,
        myRole = null,
        lastModified = lastModifiedAt,
        modifiedBy = lastModifiedByName,
        foreign = true,
        foreignId = offerId,
        myTurn = myTurn
    )
    suspend fun counter(inter: Boolean, offer: OtcOfferDto, body: CounterOtcOfferDto): ApiResult<OtcOfferDto> {
        if (!inter) return safeApiCall { api.counterIntra(offer.id, body) }
        val foreignId = offer.foreignId ?: return ApiResult.Failure(
            ApiError(httpCode = 400, message = "Inter-bank ponuda nema foreignId.", kind = ApiError.Kind.Validation)
        )
        val interReq = CounterOtcInterbankOfferRequest(
            offerId = foreignId,
            quantity = body.quantity.toDouble(),
            pricePerStock = body.pricePerStock,
            premium = body.premium,
            settlementDate = body.settlementDate
        )
        return safeApiCall { api.counterInter(foreignId, interReq) }.map { it.toUiOffer() }
    }

    suspend fun decline(inter: Boolean, offer: OtcOfferDto): ApiResult<OtcOfferDto> {
        if (!inter) return safeApiCall { api.declineIntra(offer.id) }
        val foreignId = offer.foreignId ?: return ApiResult.Failure(
            ApiError(httpCode = 400, message = "Inter-bank ponuda nema foreignId.", kind = ApiError.Kind.Validation)
        )
        return safeApiCall { api.declineInter(foreignId) }.map { it.toUiOffer() }
    }

    suspend fun accept(inter: Boolean, offer: OtcOfferDto, buyerAccountId: Long?): ApiResult<OtcOfferDto> {
        if (!inter) {
            val body = AcceptOtcOfferDto(buyerAccountId)
            return safeApiCall { api.acceptIntra(offer.id, body) }
        }
        val foreignId = offer.foreignId ?: return ApiResult.Failure(
            ApiError(httpCode = 400, message = "Inter-bank ponuda nema foreignId.", kind = ApiError.Kind.Validation)
        )
        return safeApiCall { api.acceptInter(foreignId, buyerAccountId) }.map { it.toUiOffer() }
    }
    suspend fun listContracts(inter: Boolean, status: String? = null): ApiResult<List<OtcContractDto>> = if (inter) {
        safeApiCall { api.listMyInterContracts(status) }.map { list -> list.map { it.toUiContract() } }
    } else {
        safeApiCall { api.listIntraContracts(status) }
    }

    private fun OtcInterbankContractApiDto.toUiContract(): OtcContractDto = OtcContractDto(
        id = id.hashCode().toLong(),
        listingId = listingId ?: 0L,
        listingTicker = listingTicker,
        listingName = listingName,
        quantity = quantity.toInt(),
        strikePrice = strikePrice,
        premium = premium,
        settlementDate = settlementDate,
        status = status,
        buyerName = buyerName,
        sellerName = sellerName,
        myRole = null,
        currentPrice = currentPrice,
        profitEstimate = currentPrice?.let { (it - strikePrice) * quantity - premium },
        foreign = true,
        createdAt = createdAt,
        foreignId = id
    )
    suspend fun exercise(inter: Boolean, contract: OtcContractDto, buyerAccountId: Long?): ApiResult<OtcContractDto> {
        if (!inter) {
            val body = ExerciseRequestDto(buyerAccountId)
            return safeApiCall { api.exerciseIntra(contract.id, body) }
        }
        val foreignId = contract.foreignId ?: return ApiResult.Failure(
            ApiError(httpCode = 400, message = "Inter-bank ugovor nema foreignId.", kind = ApiError.Kind.Validation)
        )
        return safeApiCall { api.exerciseInter(foreignId, buyerAccountId) }.map { it.toUiContract() }
    }

    suspend fun sagaStatusIntra(contractId: Long): ApiResult<SagaStatusDto> = safeApiCall { api.sagaStatusIntra(contractId) }

    suspend fun pollInterContractStatus(foreignId: String): ApiResult<String> =
        safeApiCall { api.listMyInterContracts(null) }.map { list ->
            list.firstOrNull { it.id == foreignId }?.status ?: "UNKNOWN"
        }
}
