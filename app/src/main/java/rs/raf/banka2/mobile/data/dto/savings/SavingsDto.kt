package rs.raf.banka2.mobile.data.dto.savings

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class SavingsDepositDto(
    val id: Long,
    val clientId: Long,
    val clientName: String?,
    val linkedAccountId: Long,
    val linkedAccountNumber: String?,
    val principalAmount: BigDecimal,
    val currencyCode: String,
    val termMonths: Int,
    val annualInterestRate: BigDecimal,
    val startDate: String,
    val maturityDate: String,
    val nextInterestPaymentDate: String,
    val totalInterestPaid: BigDecimal,
    val autoRenew: Boolean,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?,
)

@JsonClass(generateAdapter = true)
data class SavingsTransactionDto(
    val id: Long,
    val depositId: Long,
    val type: String,
    val amount: BigDecimal,
    val currencyCode: String,
    val processedDate: String,
    val resultingTransactionId: Long?,
    val description: String?,
    val createdAt: String?,
)

@JsonClass(generateAdapter = true)
data class SavingsRateDto(
    val id: Long,
    val currencyCode: String,
    val termMonths: Int,
    val annualRate: BigDecimal,
    val active: Boolean,
    val effectiveFrom: String,
)

@JsonClass(generateAdapter = true)
data class OpenDepositRequest(
    val sourceAccountId: Long,
    val linkedAccountId: Long,
    val principalAmount: BigDecimal,
    val termMonths: Int,
    val autoRenew: Boolean,
    val otpCode: String,
)

@JsonClass(generateAdapter = true)
data class ToggleAutoRenewRequest(val autoRenew: Boolean)

@JsonClass(generateAdapter = true)
data class WithdrawEarlyRequest(val otpCode: String)

object SavingsConstants {
    val MIN_DEPOSIT_AMOUNT: Map<String, BigDecimal> = mapOf(
        "RSD" to BigDecimal("10000"),
        "JPY" to BigDecimal("10000"),
        "EUR" to BigDecimal("100"),
        "USD" to BigDecimal("100"),
        "CHF" to BigDecimal("100"),
        "GBP" to BigDecimal("100"),
        "CAD" to BigDecimal("100"),
        "AUD" to BigDecimal("100"),
    )

    fun minDepositFor(currencyCode: String): BigDecimal =
        MIN_DEPOSIT_AMOUNT[currencyCode] ?: BigDecimal("100")

    val TERM_OPTIONS = listOf(3, 6, 12, 24, 36)

    val STATUS_LABEL_SR = mapOf(
        "ACTIVE" to "Aktivan",
        "MATURED" to "Dospelo",
        "WITHDRAWN_EARLY" to "Raskinuto",
        "RENEWED" to "Auto-obnovljen",
    )

    val TRANSACTION_TYPE_LABEL_SR = mapOf(
        "OPEN" to "Otvaranje",
        "INTEREST_PAYMENT" to "Mesecna kamata",
        "PRINCIPAL_RETURN" to "Glavnica vracena",
        "EARLY_WITHDRAWAL_PRINCIPAL" to "Raskid — glavnica",
        "EARLY_WITHDRAWAL_PENALTY" to "Penal raskida",
        "RENEWAL_OPEN" to "Auto-obnova",
    )
}
