package rs.raf.banka2.mobile.data.dto.otchistory

import com.squareup.moshi.JsonClass

/**
 * Jedan zapis u istoriji OTC pregovora — snimak jedne iteracije
 * (inicijalna ponuda ili kontraponuda). Paritet sa FE
 * `OtcNegotiationHistoryDto` (B10 spec, `/otc/negotiation-history` endpoint-i).
 */
@JsonClass(generateAdapter = true)
data class OtcNegotiationHistoryDto(
    val id: Long,
    /** ID originalne ponude (OtcOffer) na koju se zapis odnosi. */
    val negotiationId: Long,
    val quantity: Int,
    val pricePerShare: Double,
    val premium: Double,
    /** ISO datum izmirenja koji je vazio u toj iteraciji. */
    val settlementDate: String? = null,
    /** Status ponude (ACTIVE / ACCEPTED / DECLINED). */
    val status: String,
    val modifiedById: Long? = null,
    val modifiedByName: String? = null,
    /** ISO datetime kada je zapis nastao. */
    val createdAt: String
)
