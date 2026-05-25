package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.ExchangeApi
import rs.raf.banka2.mobile.data.dto.exchange.CalculateExchangeResponseDto
import rs.raf.banka2.mobile.data.dto.exchange.ExchangeHistoryPointDto
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

    /**
     * Mobile-bonus #5: 1-mesec istorija deviznog kursa za jednu valutu.
     * BE endpoint je optimisticki — ako vrati 404/501/405, repository
     * vraca Success(prazna lista) tako da UI graceful-fallback ne renderuje sparkline.
     */
    suspend fun history(currency: String, days: Int = 30): ApiResult<List<ExchangeHistoryPointDto>> {
        val result = safeApiCall { api.history(currency, days) }
        return when (result) {
            is ApiResult.Failure -> {
                val code = result.error.httpCode
                if (code == 404 || code == 501 || code == 405) {
                    ApiResult.Success(emptyList())
                } else {
                    result
                }
            }
            else -> result
        }
    }
}
