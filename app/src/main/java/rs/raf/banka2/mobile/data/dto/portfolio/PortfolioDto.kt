package rs.raf.banka2.mobile.data.dto.portfolio

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PortfolioItemDto(
    val id: Long,
    val listingId: Long? = null,
    val listingTicker: String? = null,
    val listingName: String? = null,
    val listingType: String? = null,         // STOCK / FUTURES / OPTION
    val optionType: String? = null,          // CALL / PUT (samo OPTION)
    val optionId: Long? = null,              // direktan ID opcije za exercise
    val strikePrice: Double? = null,
    val premium: Double? = null,
    val settlementDate: String? = null,
    val itm: Boolean? = null,
    val quantity: Int = 0,
    val averageBuyPrice: Double = 0.0,
    val currentPrice: Double? = null,
    val totalValue: Double? = null,
    val profit: Double? = null,
    val profitPercent: Double? = null,
    val publicQuantity: Int? = null,
    val reservedQuantity: Int? = null,
    val inOrderQuantity: Int? = null,
    val currency: String? = null
) {
    val isOption: Boolean get() = listingType.equals("OPTION", true) || optionId != null
}

@JsonClass(generateAdapter = true)
data class PortfolioSummaryDto(
    val totalValue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalProfitPercent: Double? = null,
    val taxOwed: Double? = null,
    val currency: String? = null
)

@JsonClass(generateAdapter = true)
data class PublicQuantityUpdateDto(
    val quantity: Int
)
