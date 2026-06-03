package rs.raf.banka2.mobile.data.dto.payment

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * ME-11: novcana polja prebacena sa Double na BigDecimal (spec C2 §255).
 * Moshi KotlinJsonAdapterFactory podrzava BigDecimal nativno (vec se koristi
 * u CardDto.kt i SavingsDto.kt).
 */
@JsonClass(generateAdapter = true)
data class CreatePaymentRequestDto(
    val fromAccountId: Long? = null,
    val fromAccountNumber: String? = null,
    val toAccountNumber: String,
    val amount: BigDecimal,
    val currency: String? = null,
    val recipientName: String,
    val paymentCode: String = "289",
    val paymentPurpose: String,
    val referenceNumber: String? = null,
    val description: String? = null,
    val otpCode: String
)

@JsonClass(generateAdapter = true)
data class PaymentResponseDto(
    val id: Long,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String? = null,
    val status: String? = null,
    val description: String? = null,
    val recipientName: String? = null,
    val paymentCode: String? = null,
    val referenceNumber: String? = null,
    val direction: String? = null,
    val fee: BigDecimal? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class OtpResponseDto(
    val active: Boolean = false,
    val code: String? = null,
    val expiresIn: Int? = null,
    val expiresInSeconds: Int? = null,
    val attempts: Int? = null,
    val maxAttempts: Int? = null,
    val message: String? = null
) {
    /** Backend ima dva imena za isto polje — uzimamo prvo nenull. */
    val secondsLeft: Int?
        get() = expiresIn ?: expiresInSeconds
}

@JsonClass(generateAdapter = true)
data class OtpRequestStatusDto(
    val sent: Boolean = false,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class OtpVerifyRequest(
    val code: String
)

/**
 * Request body za `POST /payments/{id}/approve` (TODO_final Mobile bonus #7,
 * Quick Approve). Matchuje BE `ApprovePaymentRequest { otpCode }` — TOTP iz
 * Google Authenticator-a, tacno 6 cifara (BE validira `\d{6}`).
 */
@JsonClass(generateAdapter = true)
data class ApprovePaymentRequest(
    val otpCode: String
)
