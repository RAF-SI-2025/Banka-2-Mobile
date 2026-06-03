package rs.raf.banka2.mobile.data.dto.otc

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OtcListingDto(
    val listingId: Long,
    @param:Json(name = "listingTicker") val ticker: String,
    @param:Json(name = "listingName") val name: String? = null,
    // BE `OtcListingDto.sellerId` (intra). Ranije citan kao `sellerUserId` → null
    // → CreateOtcOfferDto.sellerId (@NotNull) prazan → svaka intra ponuda 400.
    @param:Json(name = "sellerId") val sellerUserId: Long? = null,
    val sellerName: String? = null,
    val sellerRole: String? = null,
    @param:Json(name = "availablePublicQuantity") val publicQuantity: Int = 0,
    @param:Json(name = "listingCurrency") val currency: String? = null,
    val currentPrice: Double? = null,
    val bankRoutingNumber: String? = null,
    val foreign: Boolean = false,
    /** Inter-bank only: opaque seller public ID iz partner banke (preneti uz CreateOffer). */
    val foreignSellerPublicId: String? = null,
    /** Inter-bank only: bankCode partner banke (npr. "111", "222"); za UI label-e. */
    val foreignBankCode: String? = null
)

@JsonClass(generateAdapter = true)
data class OtcOfferDto(
    val id: Long,
    val listingId: Long,
    val listingTicker: String? = null,
    val listingName: String? = null,
    val currentPrice: Double? = null,
    // BE intra `OtcOfferDto.listingCurrency` (ranije `currency` → null za intra).
    @param:Json(name = "listingCurrency") val currency: String? = null,
    val quantity: Int,
    val pricePerStock: Double,
    val premium: Double,
    val settlementDate: String? = null,
    val status: String,                // ACTIVE / ACCEPTED / DECLINED / CANCELED
    // BE intra salje buyerId/sellerId — koristimo ih za derivaciju myRole (BE ga ne salje).
    val buyerId: Long? = null,
    val sellerId: Long? = null,
    val waitingOnUserId: Long? = null,
    val waitingOnRole: String? = null,
    val buyerName: String? = null,
    val sellerName: String? = null,
    val myRole: String? = null,        // BUYER / SELLER (derived client-side za intra)
    // BE intra `lastModifiedAt`/`lastModifiedByName` (ranije `lastModified`/`modifiedBy` → null).
    @param:Json(name = "lastModifiedAt") val lastModified: String? = null,
    @param:Json(name = "lastModifiedByName") val modifiedBy: String? = null,
    val foreign: Boolean = false,
    /** Inter-bank only: opaque offer ID iz partner banke ("{routingNumber}:{uuid}"). */
    val foreignId: String? = null,
    /** Inter-bank only: signal "myTurn" iz BE-a — true znaci da je naredna akcija na nama. */
    val myTurn: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateOtcOfferDto(
    val listingId: Long,
    // BE intra `CreateOtcOfferDto.sellerId` (@NotNull). Ranije slato kao
    // `sellerUserId` → BE @NotNull sellerId prazan → 400 na svaku intra ponudu.
    @param:Json(name = "sellerId") val sellerUserId: Long? = null,
    val sellerRole: String? = null,
    val quantity: Int,
    val pricePerStock: Double,
    val premium: Double,
    val settlementDate: String,
    val foreign: Boolean = false,
    val bankRoutingNumber: String? = null,
    /** Inter-bank only: opaque seller public ID iz partner banke. */
    val foreignSellerPublicId: String? = null,
    /** Inter-bank only: ticker je primarni identifikator listinga (ne listingId). */
    val foreignListingTicker: String? = null
)

@JsonClass(generateAdapter = true)
data class CounterOtcOfferDto(
    val quantity: Int,
    val pricePerStock: Double,
    val premium: Double,
    val settlementDate: String
)

@JsonClass(generateAdapter = true)
data class AcceptOtcOfferDto(
    val buyerAccountId: Long? = null
)

@JsonClass(generateAdapter = true)
data class OtcContractDto(
    val id: Long,
    val listingId: Long,
    val listingTicker: String? = null,
    val listingName: String? = null,
    val quantity: Int,
    val strikePrice: Double,
    val premium: Double,
    val settlementDate: String? = null,
    val status: String,                // ACTIVE / EXERCISED / EXPIRED / ABORTED
    // BE intra salje buyerId/sellerId — koristimo ih za derivaciju myRole.
    val buyerId: Long? = null,
    val sellerId: Long? = null,
    val buyerName: String? = null,
    val sellerName: String? = null,
    val myRole: String? = null,        // BUYER / SELLER (derived client-side za intra)
    val currentPrice: Double? = null,
    // BE intra `OtcContractDto.profit` (server-side, NET od premije, Celina4 §149).
    // Ranije citan kao `profitEstimate` → uvek null → profit prikaz prazan.
    @param:Json(name = "profit") val profitEstimate: Double? = null,
    val foreign: Boolean = false,
    val createdAt: String? = null,
    /** Inter-bank only: opaque contract ID iz partner banke ("{routingNumber}:{uuid}"). */
    val foreignId: String? = null
)

/**
 * Odgovor na `POST /otc/contracts/{id}/exercise` (Model-B SAGA orkestrator).
 * Matchuje BE `OtcExerciseResultDto { sagaId, sagaStatus, currentStep, id, status }`.
 *
 *  - `sagaId` — handle za polling preko `GET /otc/saga/{sagaId}`
 *  - `sagaStatus` — terminalni `SagaStatus` (COMPLETED / COMPENSATED / FAILED)
 *  - `currentStep` — ordinal poslednje pokusane forward faze (1..5)
 *  - `id` — id ugovora
 *  - `status` — status ugovora (EXERCISED na uspeh / ACTIVE na rollback)
 */
@JsonClass(generateAdapter = true)
data class OtcExerciseResultDto(
    val sagaId: String? = null,
    val sagaStatus: String? = null,
    val currentStep: Int = 0,
    val id: Long? = null,
    val status: String? = null
)

/**
 * Jedan zapis SAGA log-a — matchuje BE `SagaLogEntry { phase, kind, outcome, message, at }`.
 */
@JsonClass(generateAdapter = true)
data class SagaLogEntryDto(
    val phase: Int = 0,
    val kind: String? = null,          // FORWARD / COMPENSATE
    val outcome: String? = null,       // ok / err
    val message: String? = null,
    val at: String? = null
)

/**
 * Odgovor na `GET /otc/saga/{sagaId}` — matchuje BE
 * `SagaStatusDto { sagaId, status, currentStep, log }`.
 *
 * `status` je `SagaStatus` enum ime: RUNNING / COMPENSATING / COMPENSATED /
 * COMPLETED / FAILED. UI mapira ovo u svoje progress faze.
 */
@JsonClass(generateAdapter = true)
data class SagaStatusDto(
    val sagaId: String? = null,
    val status: String,                // RUNNING / COMPENSATING / COMPENSATED / COMPLETED / FAILED
    val currentStep: Int = 0,
    val log: List<SagaLogEntryDto> = emptyList()
)

// ─── INTER-BANK API DTO-i (matchuju BE InterbankOtcWrapperDtos.java) ─────
//
// Razlika od intra-bank DTO-a:
//  - offerId/contractId su STRING (UUID iz partner banke), ne Long
//  - quantity/price/premium su decimal (Double mapiranje OK za UI)
//  - listing identifikovan kroz (bankCode, sellerPublicId, listingTicker), ne Long ID

@JsonClass(generateAdapter = true)
data class OtcInterbankListingApiDto(
    val bankCode: String,
    val sellerPublicId: String,
    val sellerName: String? = null,
    val listingTicker: String,
    val listingName: String? = null,
    val listingCurrency: String? = null,
    val currentPrice: Double? = null,
    val availableQuantity: Double = 0.0,
    val sellerRole: String? = null
)

@JsonClass(generateAdapter = true)
data class OtcInterbankOfferApiDto(
    val offerId: String,
    val listingTicker: String,
    val listingName: String? = null,
    val listingCurrency: String? = null,
    val currentPrice: Double? = null,
    val buyerBankCode: String? = null,
    val buyerUserId: String? = null,
    val buyerName: String? = null,
    val sellerBankCode: String? = null,
    val sellerUserId: String? = null,
    val sellerName: String? = null,
    val quantity: Double,
    val pricePerStock: Double,
    val premium: Double,
    val settlementDate: String? = null,
    val waitingOnBankCode: String? = null,
    val waitingOnUserId: String? = null,
    val myTurn: Boolean = false,
    val status: String,
    val lastModifiedAt: String? = null,
    val lastModifiedByName: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateOtcInterbankOfferRequest(
    val sellerBankCode: String,
    val sellerUserId: String,
    val listingTicker: String,
    val quantity: Double,
    val pricePerStock: Double,
    val premium: Double,
    val settlementDate: String
)

@JsonClass(generateAdapter = true)
data class CounterOtcInterbankOfferRequest(
    val offerId: String? = null,
    val quantity: Double,
    val pricePerStock: Double,
    val premium: Double,
    val settlementDate: String
)

@JsonClass(generateAdapter = true)
data class OtcInterbankContractApiDto(
    val id: String,
    val listingId: Long? = null,
    val listingTicker: String,
    val listingName: String? = null,
    val listingCurrency: String? = null,
    val buyerUserId: String? = null,
    val buyerBankCode: String? = null,
    val buyerName: String? = null,
    val sellerUserId: String? = null,
    val sellerBankCode: String? = null,
    val sellerName: String? = null,
    val quantity: Double,
    val strikePrice: Double,
    val premium: Double,
    val currentPrice: Double? = null,
    val settlementDate: String? = null,
    val status: String,
    val createdAt: String? = null,
    val exercisedAt: String? = null
)
