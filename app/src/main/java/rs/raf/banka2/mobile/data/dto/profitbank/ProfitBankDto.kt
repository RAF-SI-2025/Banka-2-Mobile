package rs.raf.banka2.mobile.data.dto.profitbank

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActuaryProfitDto(
    val employeeId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val position: String? = null,
    val realizedProfitRsd: Double = 0.0
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").takeIf { it.isNotBlank() }
            ?: email.orEmpty()
}

@JsonClass(generateAdapter = true)
data class BankFundPositionDto(
    val fundId: Long,
    val fundName: String? = null,
    val managerName: String? = null,
    val sharePercent: Double? = null,
    val shareAmountRsd: Double? = null,
    val profitRsd: Double? = null,
    val accountNumber: String? = null
)
