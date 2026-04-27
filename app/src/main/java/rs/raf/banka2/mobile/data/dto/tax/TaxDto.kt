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
