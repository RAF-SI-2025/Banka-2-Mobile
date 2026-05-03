package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.PaymentApi
import rs.raf.banka2.mobile.data.dto.interbank.InitiateInterbankPaymentDto
import rs.raf.banka2.mobile.data.dto.interbank.InterbankTransactionDto
import rs.raf.banka2.mobile.data.dto.payment.CreatePaymentRequestDto
import rs.raf.banka2.mobile.data.dto.payment.PaymentResponseDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inter-bank placanja idu kroz standardni `/payments` endpoint posle BE
 * protocol-refactor-a (PR #96, 28.04.2026). Stari `/interbank/payments`
 * endpoint je uklonjen jer protokol §2 rezervise `/interbank` URL prefix
 * STROGO za bank-to-bank poruke.
 *
 * BE prepoznaje inter-bank placanje preko routing prefiksa broja racuna
 * (prve 3 cifre) i interno pokrece 2-Phase Commit transakciju. FE/Mobile
 * koriste isti payload kao za intra-bank, samo prate status kroz polling.
 *
 * Status mapping (BE -> Mobile):
 *  PENDING    -> INITIATED
 *  PROCESSING -> COMMITTING
 *  COMPLETED  -> COMMITTED
 *  REJECTED   -> ABORTED (sa reason)
 *  CANCELLED  -> ABORTED (sa reason)
 *  ostali     -> STUCK (defensive)
 */
@Singleton
class InterbankRepository @Inject constructor(
    private val paymentApi: PaymentApi
) {
    suspend fun initiate(request: InitiateInterbankPaymentDto): ApiResult<InterbankTransactionDto> {
        val payload = CreatePaymentRequestDto(
            fromAccountId = request.fromAccountId,
            fromAccountNumber = null,
            toAccountNumber = request.toAccountNumber,
            amount = request.amount,
            currency = null,
            recipientName = request.recipientName,
            paymentCode = request.paymentCode,
            paymentPurpose = request.paymentPurpose,
            referenceNumber = request.referenceNumber,
            description = request.paymentPurpose,
            otpCode = request.otpCode
        )
        return safeApiCall { paymentApi.createPayment(payload) }.map { it.toInterbankTransaction() }
    }

    suspend fun status(transactionId: String): ApiResult<InterbankTransactionDto> {
        val paymentId = transactionId.toLongOrNull()
            ?: return ApiResult.Failure(
                rs.raf.banka2.mobile.core.network.ApiError(
                    httpCode = null,
                    message = "Neispravan ID transakcije: $transactionId",
                    kind = rs.raf.banka2.mobile.core.network.ApiError.Kind.Validation
                )
            )
        return safeApiCall { paymentApi.getPaymentById(paymentId) }.map { it.toInterbankTransaction() }
    }
}

/**
 * Mapira `PaymentResponseDto` u `InterbankTransactionDto` po istom pattern-u
 * kao FE `interbankPaymentService.mapPaymentToInterbank()`. Preserves payment ID
 * kao stringifikovan transactionId.
 */
private fun PaymentResponseDto.toInterbankTransaction(): InterbankTransactionDto {
    val mappedStatus = when (status?.uppercase()) {
        "PENDING" -> "INITIATED"
        "PROCESSING" -> "COMMITTING"
        "COMPLETED" -> "COMMITTED"
        "REJECTED" -> "ABORTED"
        "CANCELLED" -> "ABORTED"
        null -> "STUCK"
        else -> "STUCK"
    }
    val message = when (status?.uppercase()) {
        "REJECTED" -> "Placanje odbijeno."
        "CANCELLED" -> "Placanje otkazano."
        "COMPLETED" -> "Placanje uspesno izvrseno."
        "PROCESSING" -> "Placanje u obradi (commit faza)."
        "PENDING" -> "Placanje je iniciralo 2-Phase Commit transakciju."
        else -> null
    }
    return InterbankTransactionDto(
        transactionId = id.toString(),
        status = mappedStatus,
        routingNumber = null,
        fromAccount = fromAccount,
        toAccount = toAccount,
        amount = amount,
        currency = currency,
        convertedAmount = null,
        convertedCurrency = null,
        rate = null,
        fee = fee,
        message = message,
        updatedAt = createdAt
    )
}
