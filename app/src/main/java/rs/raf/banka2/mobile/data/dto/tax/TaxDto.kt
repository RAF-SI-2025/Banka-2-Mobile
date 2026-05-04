package rs.raf.banka2.mobile.data.dto.tax

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TaxRecordDto(
    val userId: Long? = null,
    val name: String? = null,
    val email: String? = null,
    val userType: String? = null,
    val totalGain: Double? = null,
    val totalLoss: Double? = null,
    val taxableIncome: Double? = null,
    val taxAmount: Double? = null,
    val paidThisYear: Double? = null,
    val owed: Double? = null,
    val currency: String? = null
)

/**
 * Per-listing breakdown poreza za korisnika (Spec Celina 3 §516-518).
 * UI prikazuje "AAPL: +$50 -> 7.50 RSD" za svaku hartiju koja je doprinela
 * profitu/gubitku.
 */
@JsonClass(generateAdapter = true)
data class TaxBreakdownItemDto(
    val listingId: Long? = null,
    val ticker: String? = null,
    val listingCurrency: String? = null,
    val profitNative: Double? = null,
    val profitRsd: Double? = null,
    val taxOwed: Double? = null
)
