package rs.raf.banka2.mobile.data.dto.order

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateOrderDto(
    val listingId: Long,
    val orderType: String,            // MARKET / LIMIT / STOP / STOP_LIMIT
    val direction: String,            // BUY / SELL
    val quantity: Int,
    val limitPrice: Double? = null,
    val stopPrice: Double? = null,
    val allOrNone: Boolean = false,
    val margin: Boolean = false,
    val accountId: Long? = null,
    val onBehalfOfFundId: Long? = null,
    val otpCode: String
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    val id: Long,
    val listingId: Long? = null,
    val listingTicker: String? = null,
    val listingName: String? = null,
    val listingType: String? = null,
    val orderType: String,
    val direction: String,
    val quantity: Int,
    val filledQuantity: Int? = null,
    val remainingPortions: Int? = null,
    val limitPrice: Double? = null,
    val stopPrice: Double? = null,
    val averageExecutionPrice: Double? = null,
    val totalValue: Double? = null,
    val fee: Double? = null,
    val fxCommission: Double? = null,
    val exchangeRate: Double? = null,
    val accountCurrency: String? = null,
    val status: String,               // PENDING / APPROVED / DONE / DECLINED / CANCELLED
    val allOrNone: Boolean? = null,
    val approvedBy: String? = null,
    val onBehalfOfFundId: Long? = null,
    val onBehalfOfFundName: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
