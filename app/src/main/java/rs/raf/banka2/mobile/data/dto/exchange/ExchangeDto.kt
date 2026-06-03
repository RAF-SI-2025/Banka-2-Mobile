package rs.raf.banka2.mobile.data.dto.exchange

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class ExchangeRateDto(
    val currency: String? = null,
    val fromCurrency: String? = null,
    val toCurrency: String? = null,
    val rate: BigDecimal? = null,
    val buyRate: BigDecimal? = null,
    val sellRate: BigDecimal? = null,
    val middleRate: BigDecimal? = null,
    val date: String? = null,
    val lastUpdated: String? = null
)

@JsonClass(generateAdapter = true)
data class CalculateExchangeResponseDto(
    val fromCurrency: String,
    val toCurrency: String,
    val amount: BigDecimal,
    val convertedAmount: BigDecimal,
    val rate: BigDecimal? = null,
    val exchangeRate: BigDecimal? = null,
    val fee: BigDecimal? = null
)

/**
 * Mobile-bonus #5: jedna tacka u 1-mesec istoriji deviznog kursa.
 * BE ce verovatno dodati `GET /exchange/history` u sledecoj rundi —
 * dok ne postoji, repository graceful-fallback prazna lista. Sparkline
 * se ne renderuje ako lista prazna.
 */
@JsonClass(generateAdapter = true)
data class ExchangeHistoryPointDto(
    val date: String,
    /** Srednji kurs na taj dan (RSD baza, kompatibilno sa ExchangeRateDto.middleRate). */
    val rate: BigDecimal
)
