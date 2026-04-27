package rs.raf.banka2.mobile.data.dto.transfer

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TransferInternalRequestDto(
    val fromAccountId: Long,
    val toAccountId: Long? = null,
    val toAccountNumber: String? = null,
    val amount: Double,
    val description: String? = null,
    val otpCode: String
)

@JsonClass(generateAdapter = true)
data class TransferFxRequestDto(
    val fromAccountId: Long,
    val toAccountId: Long? = null,
    val toAccountNumber: String? = null,
    val amount: Double,
    val currency: String,
    val description: String? = null,
    val otpCode: String
)

@JsonClass(generateAdapter = true)
data class TransferResponseDto(
    val id: Long,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: Double = 0.0,
    val currency: String? = null,
    val convertedAmount: Double? = null,
    val convertedCurrency: String? = null,
    val rate: Double? = null,
    val fee: Double? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val description: String? = null,
    val type: String? = null
)
