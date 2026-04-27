package rs.raf.banka2.mobile.data.dto.margin

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MarginAccountDto(
    val id: Long,
    val accountNumber: String? = null,
    val initialMargin: Double = 0.0,
    val maintenanceMargin: Double = 0.0,
    val loanValue: Double = 0.0,
    val bankParticipation: Double? = null,
    val currency: String? = null,
    val active: Boolean = true,
    val status: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MarginTransactionDto(
    val id: Long,
    val type: String? = null,
    val amount: Double = 0.0,
    val timestamp: String? = null,
    val balanceAfter: Double? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateMarginAccountDto(
    val initialMargin: Double,
    val maintenanceMargin: Double,
    val bankParticipation: Double,
    val userId: Long? = null,
    val companyId: Long? = null
)

@JsonClass(generateAdapter = true)
data class MarginAmountRequestDto(
    val amount: Double
)
