package rs.raf.banka2.mobile.data.dto.card

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardDto(
    val id: Long,
    val cardNumber: String? = null,
    val cardName: String? = null,
    val cardType: String? = null,
    val brand: String? = null,
    val status: String? = null,
    val cardLimit: Double? = null,
    val balance: Double? = null,
    val cvv: String? = null,
    val pin: String? = null,
    val expirationDate: String? = null,
    val accountId: Long? = null,
    val accountNumber: String? = null,
    val ownerName: String? = null
)

@JsonClass(generateAdapter = true)
data class CardLimitUpdateDto(
    val cardLimit: Double
)

@JsonClass(generateAdapter = true)
data class CardRequestCreateDto(
    val accountId: Long,
    val cardLimit: Double,
    val cardType: String
)

@JsonClass(generateAdapter = true)
data class CardRequestResponseDto(
    val id: Long,
    val accountId: Long? = null,
    val accountNumber: String? = null,
    val ownerName: String? = null,
    val cardLimit: Double? = null,
    val cardType: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val rejectionReason: String? = null
)

@JsonClass(generateAdapter = true)
data class CardRequestRejectDto(
    val reason: String
)
