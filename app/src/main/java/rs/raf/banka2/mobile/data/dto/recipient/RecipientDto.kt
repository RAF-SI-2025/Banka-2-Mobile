package rs.raf.banka2.mobile.data.dto.recipient

import com.squareup.moshi.JsonClass

// R1-646: `description` je uklonjen iz svih recipient DTO-ova. BE ugovor
// (CreatePaymentRecipientRequestDto + PaymentRecipientResponseDto) NE poznaje
// to polje — slanje je tiho odbacivano, a odgovor ga nikad ne sadrzi. Polje je
// bilo cisti drift koji je sugerisao funkcionalnost koja ne postoji.
@JsonClass(generateAdapter = true)
data class RecipientDto(
    val id: Long,
    val name: String,
    val accountNumber: String,
    val bankCode: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateRecipientDto(
    val name: String,
    val accountNumber: String
)

@JsonClass(generateAdapter = true)
data class UpdateRecipientDto(
    val name: String,
    val accountNumber: String
)
