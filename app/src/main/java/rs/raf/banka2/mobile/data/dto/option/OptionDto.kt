package rs.raf.banka2.mobile.data.dto.option

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Jedna opcija (Call ili Put) — kombinuje strike, premium, IV, volume.
 *
 * KONTRAKT (P0-M1 N2): trading-service `OptionDto`
 * (`rs.raf.trading.option.dto.OptionDto`) salje:
 *  - `optionType` (NE `type`), npr. "CALL" | "PUT"
 *  - `price` (premija po Black-Scholes, NE `premium`)
 *  - `inTheMoney` (NE `itm`)
 *  - `settlementDate` (NE `expirationDate`)
 *  - `currentStockPrice` umesto `last`.
 * Zadrzavamo Kotlin imena koja UI cita (`type`/`premium`/`itm`) ali ih vezujemo
 * za BE JSON imena preko [Json] aliasa. `type` je nullable da nedostajuci
 * `optionType` ne pukne Moshi parser.
 */
@JsonClass(generateAdapter = true)
data class OptionDto(
    val id: Long,
    @param:Json(name = "optionType") val type: String? = null,   // "CALL" | "PUT"
    val ticker: String? = null,
    val stockTicker: String? = null,
    val stockName: String? = null,
    val stockListingId: Long? = null,
    val strikePrice: BigDecimal,
    /** BE polje `price` — premija po Black-Scholes modelu. */
    @param:Json(name = "price") val premium: BigDecimal? = null,
    val bid: BigDecimal? = null,
    val ask: BigDecimal? = null,
    val volume: Long? = null,
    val openInterest: Long? = null,
    val impliedVolatility: Double? = null,
    val settlementDate: String? = null,
    val contractSize: Int? = null,
    val maintenanceMargin: BigDecimal? = null,
    /** BE polje `inTheMoney`. */
    @param:Json(name = "inTheMoney") val itm: Boolean? = null,
    val currentStockPrice: BigDecimal? = null,
    val createdAt: String? = null
)

/**
 * Opcioni "lanac" za jednu akciju — grupisan po settlement datumu.
 *
 * KONTRAKT (P0-M1 N2): trading-service `OptionChainDto` salje odvojene liste
 * `calls` i `puts` (NE jedinstvenu `entries` listu paira po strike-u). Stara
 * Mobile mapa je citala `entries` koje BE nikad ne salje → svaki settlement-tab
 * je imao 0 redova (Opcije mrtve na Mobile). Parsiramo `calls`/`puts` direktno,
 * a [entries] je derived view koja ih spaja po strike ceni radi UI prikaza
 * (CALL levo / Strike / PUT desno).
 */
@JsonClass(generateAdapter = true)
data class OptionChainEntryDto(
    val strikePrice: BigDecimal,
    val call: OptionDto? = null,
    val put: OptionDto? = null
)

@JsonClass(generateAdapter = true)
data class OptionChainDto(
    val settlementDate: String? = null,
    val calls: List<OptionDto> = emptyList(),
    val puts: List<OptionDto> = emptyList(),
    val currentStockPrice: BigDecimal? = null
) {
    /**
     * Derived: spaja `calls` i `puts` po strike ceni u redove koje UI iscrtava.
     * Sortirano po strike-u rastuce. Strike koji ima samo CALL ili samo PUT
     * dobija odgovarajuci null. Nije serijalizovano (BE ovo polje ne salje).
     */
    val entries: List<OptionChainEntryDto>
        get() {
            val byStrike = LinkedHashMap<BigDecimal, OptionChainEntryDto>()
            (calls + puts)
                .sortedBy { it.strikePrice }
                .forEach { opt ->
                    val key = opt.strikePrice
                    val existing = byStrike[key]
                    val isCall = opt.type?.equals("CALL", ignoreCase = true) == true
                    byStrike[key] = OptionChainEntryDto(
                        strikePrice = key,
                        call = if (isCall) opt else existing?.call,
                        put = if (!isCall) opt else existing?.put
                    )
                }
            return byStrike.values.sortedBy { it.strikePrice }
        }
}
