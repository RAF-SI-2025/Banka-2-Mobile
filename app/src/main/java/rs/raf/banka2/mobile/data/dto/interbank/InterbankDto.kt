package rs.raf.banka2.mobile.data.dto.interbank

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Inicijalizacija inter-bank placanja. Backend prepoznaje koja banka je
 * primalac iz prefiksa broja racuna (prve 3 cifre = routing number) i
 * pokrece 2-Phase Commit transakciju.
 *
 * ME-11: novcana polja prebacena sa Double na BigDecimal (spec C2 §255).
 * Polje `rate` (FX kurs) ostaje Double — to je decimal koeficijent koji
 * ne predstavlja iznos pa precizija nije kritican zahtev.
 */
@JsonClass(generateAdapter = true)
data class InitiateInterbankPaymentDto(
    val fromAccountId: Long,
    val toAccountNumber: String,
    val amount: BigDecimal,
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
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val convertedAmount: BigDecimal? = null,
    val convertedCurrency: String? = null,
    val rate: Double? = null,            // FX kurs — Double je adekvatan
    val fee: BigDecimal? = null,
    val message: String? = null,
    val updatedAt: String? = null
)
