package rs.raf.banka2.mobile.data.dto.fund

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FundSummaryDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val managerName: String? = null,
    val managerId: Long? = null,
    val totalValue: Double = 0.0,
    val profit: Double = 0.0,
    val profitPercent: Double? = null,
    val minimumContribution: Double = 0.0,
    val currency: String? = "RSD",
    val accountNumber: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class FundDetailDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val managerName: String? = null,
    val managerId: Long? = null,
    val totalValue: Double = 0.0,
    val liquidFunds: Double = 0.0,
    val profit: Double = 0.0,
    val profitPercent: Double? = null,
    val minimumContribution: Double = 0.0,
    val currency: String? = "RSD",
    val accountNumber: String? = null,
    val holdings: List<FundHoldingDto> = emptyList(),
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class FundHoldingDto(
    val listingId: Long? = null,
    val ticker: String? = null,
    val name: String? = null,
    val quantity: Int = 0,
    val currentPrice: Double? = null,
    val totalValue: Double? = null
)

@JsonClass(generateAdapter = true)
data class FundPerformancePointDto(
    val date: String,
    val value: Double
)

@JsonClass(generateAdapter = true)
data class FundTransactionDto(
    val id: Long,
    val clientName: String? = null,
    val amount: Double,
    val timestamp: String? = null,
    val inflow: Boolean = true,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class FundPositionDto(
    val id: Long,
    val fundId: Long,
    val fundName: String? = null,
    val totalInvested: Double = 0.0,
    val currentValue: Double? = null,
    val percentOfFund: Double? = null,
    val profit: Double? = null,
    val profitPercent: Double? = null,
    val currency: String? = "RSD"
)

@JsonClass(generateAdapter = true)
data class CreateFundDto(
    val name: String,
    val description: String? = null,
    val minimumContribution: Double
)

@JsonClass(generateAdapter = true)
data class FundInvestDto(
    val sourceAccountId: Long,
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class FundWithdrawDto(
    val destinationAccountId: Long,
    val amount: Double? = null,
    val withdrawAll: Boolean = false
)
