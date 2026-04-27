package rs.raf.banka2.mobile.data.dto.listing

import com.squareup.moshi.JsonClass

/**
 * Hartija od vrednosti — STOCK / FUTURES / FOREX. Backend popunjava
 * podskup polja u zavisnosti od tipa (npr. dividendYield samo za STOCK,
 * contractSize samo za FUTURES). Sve nullable.
 */
@JsonClass(generateAdapter = true)
data class ListingDto(
    val id: Long,
    val ticker: String,
    val name: String,
    val listingType: String,            // "STOCK" | "FUTURES" | "FOREX"
    val exchangeAcronym: String? = null,
    val currency: String? = null,
    val baseCurrency: String? = null,
    val quoteCurrency: String? = null,
    val price: Double = 0.0,
    val ask: Double? = null,
    val bid: Double? = null,
    val priceChange: Double? = null,
    val changePercent: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val volume: Long? = null,
    val marketCap: Long? = null,
    val outstandingShares: Long? = null,
    val dividendYield: Double? = null,
    val contractSize: Int? = null,
    val contractUnit: String? = null,
    val maintenanceMargin: Double? = null,
    val settlementDate: String? = null,
    val isTestMode: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ListingDailyPriceDto(
    val date: String,
    val openPrice: Double? = null,
    val closePrice: Double? = null,
    val price: Double? = null,
    val highPrice: Double? = null,
    val lowPrice: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val volume: Long? = null
) {
    /** Backend salje `closePrice` u nekim odgovorima i `price` u drugim — uzimamo ono sto postoji. */
    val resolvedClose: Double
        get() = closePrice ?: price ?: 0.0
}

@JsonClass(generateAdapter = true)
data class ExchangeManagementDto(
    val id: Long? = null,
    val acronym: String,
    val name: String? = null,
    val isOpen: Boolean? = null,
    val testMode: Boolean? = null,
    val currentLocalTime: String? = null,
    val nextOpenTime: String? = null,
    val timezone: String? = null
)

@JsonClass(generateAdapter = true)
data class ToggleTestModeDto(
    val enabled: Boolean
)
