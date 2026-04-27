package rs.raf.banka2.mobile.data.dto.payment

import com.squareup.moshi.JsonClass

/**
 * Lakši zapis za listu plaćanja — backend vraća redukovani projekat
 * polja kako ne bi opterećivao mobile listu.
 */
@JsonClass(generateAdapter = true)
data class PaymentListItemDto(
    val id: Long,
    val orderNumber: String? = null,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: Double = 0.0,
    val currency: String? = null,
    val status: String? = null,
    val direction: String? = null,
    val description: String? = null,
    val recipientName: String? = null,
    val createdAt: String? = null,
    val paymentCode: String? = null,
    val referenceNumber: String? = null,
    val type: String? = null
)
