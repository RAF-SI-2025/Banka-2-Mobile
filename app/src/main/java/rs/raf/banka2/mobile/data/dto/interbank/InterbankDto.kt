package rs.raf.banka2.mobile.data.dto.interbank

import com.squareup.moshi.JsonClass

/**
 * Inicijalizacija inter-bank placanja. Backend prepoznaje koja banka je
 * primalac iz prefiksa broja racuna (prve 3 cifre = routing number) i
 * pokrece 2-Phase Commit transakciju.
 */
@JsonClass(generateAdapter = true)
data class InitiateInterbankPaymentDto(
    val fromAccountId: Long,
    val toAccountNumber: String,
    val amount: Double,
    val recipientName: String,
    val paymentPurpose: String,
    val paymentCode: String = "289",
    val referenceNumber: String? = null,
    val otpCode: String
)

@JsonClass(generateAdapter = true)
data class InterbankTransactionDto(
    val transactionId: String,
    val status: String,                  // INITIATED / PREPARED / READY / NOT_READY / COMMITTED / ABORTED / STUCK
    val routingNumber: String? = null,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val convertedAmount: Double? = null,
    val convertedCurrency: String? = null,
    val rate: Double? = null,
    val fee: Double? = null,
    val message: String? = null,
    val updatedAt: String? = null
)
