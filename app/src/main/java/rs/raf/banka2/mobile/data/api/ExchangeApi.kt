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

    /**
     * Mobile-bonus #5: 1-mesec istorija deviznog kursa.
     * Endpoint je optimisticki — ako BE jos uvek nema implementaciju,
     * repository hvata 404/501/405 i graceful-no-data fallback (prazna lista).
     *
     * Ocekivani format odgovora: niz `{date, rate}` parova sortiran po datumu
     * ASC; ako BE doda kasnije, ovaj endpoint vec radi.
     */
    @GET("exchange/history")
    suspend fun history(
        @Query("currency") currency: String,
        @Query("days") days: Int = 30
    ): Response<List<rs.raf.banka2.mobile.data.dto.exchange.ExchangeHistoryPointDto>>
}
