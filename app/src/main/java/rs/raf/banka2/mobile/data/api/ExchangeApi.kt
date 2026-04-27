package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.exchange.CalculateExchangeResponseDto
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeRateDto

interface ExchangeApi {

    @GET("exchange-rates")
    suspend fun getExchangeRates(): Response<List<ExchangeRateDto>>

    @GET("exchange/calculate")
    suspend fun calculate(
        @Query("amount") amount: Double,
        @Query("toCurrency") toCurrency: String,
        @Query("fromCurrency") fromCurrency: String? = null
    ): Response<CalculateExchangeResponseDto>
}
