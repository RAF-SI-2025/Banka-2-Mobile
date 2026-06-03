package rs.raf.banka2.mobile.data.dto.listing

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

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
    val price: BigDecimal = BigDecimal.ZERO,
    val ask: BigDecimal? = null,
    val bid: BigDecimal? = null,
    val priceChange: BigDecimal? = null,
    val changePercent: Double? = null,
    val high: BigDecimal? = null,
    val low: BigDecimal? = null,
    val volume: Long? = null,
    // R1-172: BE `marketCap` je BigDecimal (outstandingShares×price) — Long bi
    // odbacio frakciju / mogao pasti Moshi parse na decimalnom broju.
    val marketCap: BigDecimal? = null,
    val outstandingShares: Long? = null,
    val dividendYield: Double? = null,
    val contractSize: Int? = null,
    val contractUnit: String? = null,
    val maintenanceMargin: BigDecimal? = null,
    val settlementDate: String? = null,
    val isTestMode: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ListingDailyPriceDto(
    val date: String,
    val openPrice: BigDecimal? = null,
    val closePrice: BigDecimal? = null,
    val price: BigDecimal? = null,
    val highPrice: BigDecimal? = null,
    val lowPrice: BigDecimal? = null,
    val high: BigDecimal? = null,
    val low: BigDecimal? = null,
    val volume: Long? = null
) {
    /** Backend salje `closePrice` u nekim odgovorima i `price` u drugim — uzimamo ono sto postoji. */
    val resolvedClose: BigDecimal
        get() = closePrice ?: price ?: BigDecimal.ZERO
}

@JsonClass(generateAdapter = true)
data class ExchangeManagementDto(
    val id: Long? = null,
    val acronym: String,
    val name: String? = null,
    // R1-189: BE `ExchangeDto.isCurrentlyOpen` serijalizuje se kao `currentlyOpen`
    // (Jackson skida `is` prefix). Ranije citan kao `isOpen` → uvek null → berza
    // prikazana "ZATVORENO" + after-hours upozorenje nikad.
    @param:Json(name = "currentlyOpen") val isOpen: Boolean? = null,
    val testMode: Boolean? = null,
    val currentLocalTime: String? = null,
    val nextOpenTime: String? = null,
    @param:Json(name = "timeZone") val timezone: String? = null
)

@JsonClass(generateAdapter = true)
data class ToggleTestModeDto(
    val enabled: Boolean
)
