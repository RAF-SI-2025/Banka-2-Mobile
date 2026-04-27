package rs.raf.banka2.mobile.data.dto.exchange

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeRateDto(
    val currency: String? = null,
    val fromCurrency: String? = null,
    val toCurrency: String? = null,
    val rate: Double? = null,
    val buyRate: Double? = null,
    val sellRate: Double? = null,
    val middleRate: Double? = null,
    val date: String? = null,
    val lastUpdated: String? = null
)

@JsonClass(generateAdapter = true)
data class CalculateExchangeResponseDto(
    val fromCurrency: String,
    val toCurrency: String,
    val amount: Double,
    val convertedAmount: Double,
    val rate: Double? = null,
    val exchangeRate: Double? = null,
    val fee: Double? = null
)
