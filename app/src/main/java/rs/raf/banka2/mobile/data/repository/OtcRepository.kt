package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.core.util.stableLongId
import rs.raf.banka2.mobile.data.api.OtcApi
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcInterbankOfferRequest
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcInterbankOfferRequest
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcExerciseResultDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankContractApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankListingApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcInterbankOfferApiDto
import rs.raf.banka2.mobile.data.dto.otc.OtcListingDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto
import rs.raf.banka2.mobile.data.dto.otchistory.OtcNegotiationHistoryDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse
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
        // R2-1482: stabilan 64-bitni id (FNV-1a) umesto `String.hashCode().toLong()`
        // (32-bit → kolizije → Compose duplicate-key crash u discovery listi).
        listingId = ("$bankCode:$sellerPublicId:$listingTicker").stableLongId(),
        ticker = listingTicker,
        name = listingName,
        sellerUserId = null,
        sellerName = sellerName,
        sellerRole = sellerRole,
        // R2-1493: OTC kolicina je broj akcija (ceo broj). `Double.toInt()` truncate-uje
        // (10.99 → 10 = tiho gubljenje), pa zaokruzujemo na najblizi ceo broj.
        publicQuantity = roundQuantity(availableQuantity),
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
        // R2-1482: stabilan 64-bitni id (FNV-1a) — Compose list key; `foreignId` nosi pravi UUID.
        id = offerId.stableLongId(),
        listingId = 0L,
        listingTicker = listingTicker,
        listingName = listingName,
        currentPrice = currentPrice,
        currency = listingCurrency,
        quantity = roundQuantity(quantity), // R2-1493: zaokruzi (ne truncate)
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
            // BE cita buyerAccountId kao query param (ne body).
            return safeApiCall { api.acceptIntra(offer.id, buyerAccountId) }
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
        // R2-1482: stabilan 64-bitni id (FNV-1a) — Compose list key; `foreignId` nosi pravi UUID.
        id = id.stableLongId(),
        listingId = listingId ?: 0L,
        listingTicker = listingTicker,
        listingName = listingName,
        quantity = roundQuantity(quantity), // R2-1493: zaokruzi (ne truncate)
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
    /**
     * Intra-bank exercise (Model-B SAGA orkestrator). BE izvrsava SAGA-u
     * sinhrono i vraca terminalni `OtcExerciseResultDto` sa `sagaId`-em za
     * polling preko `GET /otc/saga/{sagaId}`.
     */
    suspend fun exerciseIntra(contract: OtcContractDto, buyerAccountId: Long?): ApiResult<OtcExerciseResultDto> =
        safeApiCall { api.exerciseIntra(contract.id, buyerAccountId) }

    /**
     * Inter-bank exercise (cross-bank wrapper). Vraca azuriran ugovor; SAGA faze
     * BE wrapper ne ekspozira, pa se progress prati pollovanjem `listMyInterContracts`.
     */
    suspend fun exerciseInter(contract: OtcContractDto, buyerAccountId: Long?): ApiResult<OtcContractDto> {
        val foreignId = contract.foreignId ?: return ApiResult.Failure(
            ApiError(httpCode = 400, message = "Inter-bank ugovor nema foreignId.", kind = ApiError.Kind.Validation)
        )
        return safeApiCall { api.exerciseInter(foreignId, buyerAccountId) }.map { it.toUiContract() }
    }

    /** Polling stanja SAGA instance preko `GET /otc/saga/{sagaId}`. */
    suspend fun sagaStatusIntra(sagaId: String): ApiResult<SagaStatusDto> = safeApiCall { api.sagaStatusIntra(sagaId) }

    /**
     * R1-479: rucno odustajanje od intra-bank OTC ugovora. BE `POST /otc/contracts/{id}/abandon`.
     * Samo intra-bank — inter-bank abandon ne postoji u BE wrapper-u (vraca Validation failure).
     */
    suspend fun abandonContract(contract: OtcContractDto): ApiResult<OtcContractDto> {
        if (contract.foreign) {
            return ApiResult.Failure(
                ApiError(
                    httpCode = 400,
                    message = "Odustajanje od inter-bank ugovora nije podrzano.",
                    kind = ApiError.Kind.Validation
                )
            )
        }
        return safeApiCall { api.abandonIntra(contract.id) }
    }

    suspend fun pollInterContractStatus(foreignId: String): ApiResult<String> =
        safeApiCall { api.listMyInterContracts(null) }.map { list ->
            list.firstOrNull { it.id == foreignId }?.status ?: "UNKNOWN"
        }

    /**
     * B10 / Spec C4 §13: paginiran pregled OTC pregovora.
     * Supervisor/admin only — BE vraca 403 inace.
     */
    suspend fun negotiationHistory(
        status: String? = null,
        modifiedById: Long? = null,
        from: String? = null,
        to: String? = null,
        page: Int = 0,
        size: Int = 20
    ): ApiResult<PageResponse<OtcNegotiationHistoryDto>> =
        safeApiCall {
            api.negotiationHistory(
                status = status?.takeIf { it.isNotBlank() && it != "ALL" },
                modifiedById = modifiedById,
                // BE radi `LocalDateTime.parse(raw)` — bare `YYYY-MM-DD` baca
                // DateTimeParseException → IllegalArgument → 400. Normalizujemo
                // date-only filter na pun ISO LocalDateTime (od pocetka / do kraja dana).
                from = from?.takeIf { it.isNotBlank() }?.let { toDayStart(it) },
                to = to?.takeIf { it.isNotBlank() }?.let { toDayEnd(it) },
                page = page,
                size = size
            )
        }

    /** `2026-05-01` -> `2026-05-01T00:00:00`; vec-puni ISO LocalDateTime ostavlja netaknut. */
    private fun toDayStart(raw: String): String =
        if (raw.length == 10 && !raw.contains('T')) "${raw}T00:00:00" else raw

    /** `2026-05-30` -> `2026-05-30T23:59:59`; vec-puni ISO LocalDateTime ostavlja netaknut. */
    private fun toDayEnd(raw: String): String =
        if (raw.length == 10 && !raw.contains('T')) "${raw}T23:59:59" else raw

    /** Hronoloski lanac kontraponuda jednog pregovora (sve iteracije). */
    suspend fun negotiationHistoryChain(negotiationId: Long): ApiResult<List<OtcNegotiationHistoryDto>> =
        safeApiCall { api.negotiationHistoryChain(negotiationId) }

    /**
     * R2-1493: BE inter-bank OTC kolicina je `Double` (deljeni decimal protokol), a
     * Mobile model je `Int` (broj akcija). `Double.toInt()` TRUNCATE-uje (10.99 → 10),
     * sto je tiho gubljenje. OTC kolicina je broj akcija (ceo broj) pa zaokruzujemo
     * na najblizi ceo broj umesto da odsecamo.
     */
    private fun roundQuantity(value: Double): Int = Math.round(value).toInt()
}
