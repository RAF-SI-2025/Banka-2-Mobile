package rs.raf.banka2.mobile.data.dto.transfer

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * ME-11: novcana polja prebacena sa Double na BigDecimal (spec C2 §255).
 * `rate` ostaje Double (FX kurs koeficijent — ne predstavlja novac).
 */
@JsonClass(generateAdapter = true)
data class TransferInternalRequestDto(
    val fromAccountId: Long,
    val toAccountId: Long? = null,
    val toAccountNumber: String? = null,
    val amount: BigDecimal,
    val description: String? = null,
    val otpCode: String
)

@JsonClass(generateAdapter = true)
data class TransferFxRequestDto(
    val fromAccountId: Long,
    val toAccountId: Long? = null,
    val toAccountNumber: String? = null,
    val amount: BigDecimal,
    val currency: String,
    val description: String? = null,
    val otpCode: String
)

@JsonClass(generateAdapter = true)
data class TransferResponseDto(
    val id: Long,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String? = null,
    val convertedAmount: BigDecimal? = null,
    val convertedCurrency: String? = null,
    val rate: Double? = null,             // FX kurs ostaje Double
    val fee: BigDecimal? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val description: String? = null,
    val type: String? = null
)
