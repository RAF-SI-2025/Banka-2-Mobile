package rs.raf.banka2.mobile.data.dto.otc

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OtcListingDto(
    val listingId: Long,
    val ticker: String,
    val name: String? = null,
    val sellerUserId: Long? = null,
    val sellerName: String? = null,
    val sellerRole: String? = null,
    val publicQuantity: Int = 0,
    val currentPrice: Double? = null,
    val currency: String? = null,
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
    val currency: String? = null,
    val quantity: Int,
    val pricePerStock: Double,
    val premium: Double,
    val settlementDate: String? = null,
    val status: String,                // ACTIVE / ACCEPTED / DECLINED / CANCELED
    val waitingOnUserId: Long? = null,
    val waitingOnRole: String? = null,
    val buyerName: String? = null,
    val sellerName: String? = null,
    val myRole: String? = null,        // BUYER / SELLER
    val lastModified: String? = null,
    val modifiedBy: String? = null,
    val foreign: Boolean = false,
    /** Inter-bank only: opaque offer ID iz partner banke ("{routingNumber}:{uuid}"). */
    val foreignId: String? = null,
    /** Inter-bank only: signal "myTurn" iz BE-a — true znaci da je naredna akcija na nama. */
    val myTurn: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateOtcOfferDto(
    val listingId: Long,
    val sellerUserId: Long? = null,
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
    val buyerName: String? = null,
    val sellerName: String? = null,
    val myRole: String? = null,        // BUYER / SELLER
    val currentPrice: Double? = null,
    val profitEstimate: Double? = null,
    val foreign: Boolean = false,
    val createdAt: String? = null,
    /** Inter-bank only: opaque contract ID iz partner banke ("{routingNumber}:{uuid}"). */
    val foreignId: String? = null
)

@JsonClass(generateAdapter = true)
data class ExerciseRequestDto(
    val buyerAccountId: Long? = null
)

/**
 * Status SAGA exercise transakcije za inter-bank ugovore. Svaka faza
 * je jasno odvojena tako da UI moze da prikaze progress (5 koraka):
 *  1. RESERVE_FUNDS — kupcu rezervisati novac
 *  2. RESERVE_SHARES — prodavcu rezervisati hartije (u drugoj banci)
 *  3. TRANSFER_FUNDS — prebacivanje novca
 *  4. TRANSFER_OWNERSHIP — prebacivanje vlasnistva hartija
 *  5. COMMITTED — finalizacija
 *
 * Statusi se polluju sa `/otc/contracts/{id}/saga-status`.
 */
@JsonClass(generateAdapter = true)
data class SagaStatusDto(
    val contractId: Long,
    val phase: String,                 // INITIATED / RESERVE_FUNDS / RESERVE_SHARES / TRANSFER_FUNDS / TRANSFER_OWNERSHIP / COMMITTED / ABORTED / STUCK
    val message: String? = null,
    val updatedAt: String? = null
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
