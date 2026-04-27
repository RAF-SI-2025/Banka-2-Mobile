package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.ExchangeApi
import rs.raf.banka2.mobile.data.dto.exchange.CalculateExchangeResponseDto
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeRateDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRepository @Inject constructor(
    private val api: ExchangeApi
) {
    suspend fun rates(): ApiResult<List<ExchangeRateDto>> = safeApiCall { api.getExchangeRates() }

    suspend fun calculate(
        amount: Double,
        fromCurrency: String?,
        toCurrency: String
    ): ApiResult<CalculateExchangeResponseDto> = safeApiCall {
        api.calculate(amount, toCurrency, fromCurrency)
    }
}
