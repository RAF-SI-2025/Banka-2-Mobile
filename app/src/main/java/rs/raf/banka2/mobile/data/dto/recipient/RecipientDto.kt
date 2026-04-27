package rs.raf.banka2.mobile.data.dto.recipient

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecipientDto(
    val id: Long,
    val name: String,
    val accountNumber: String,
    val bankCode: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateRecipientDto(
    val name: String,
    val accountNumber: String,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateRecipientDto(
    val name: String,
    val accountNumber: String,
    val description: String? = null
)
