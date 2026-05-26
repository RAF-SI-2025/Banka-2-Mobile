package rs.raf.banka2.mobile.data.dto.payment

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Lakši zapis za listu placanja — backend vraca redukovani projekat
 * polja kako ne bi opterecivao mobile listu.
 *
 * ME-11: `amount` je BigDecimal (spec C2 §255).
 */
@JsonClass(generateAdapter = true)
data class PaymentListItemDto(
    val id: Long,
    val orderNumber: String? = null,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
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
