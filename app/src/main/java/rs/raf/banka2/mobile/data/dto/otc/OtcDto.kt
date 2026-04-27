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
    val foreign: Boolean = false
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
    val foreign: Boolean = false
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
    val bankRoutingNumber: String? = null
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
    val createdAt: String? = null
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
