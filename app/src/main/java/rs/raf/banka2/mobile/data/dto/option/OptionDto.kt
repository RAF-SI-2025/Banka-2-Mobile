package rs.raf.banka2.mobile.data.dto.option

import com.squareup.moshi.JsonClass

/**
 * Jedna opcija (Call ili Put) — kombinuje strike, premium, IV, volume.
 */
@JsonClass(generateAdapter = true)
data class OptionDto(
    val id: Long,
    val type: String,                 // "CALL" | "PUT"
    val strikePrice: Double,
    val premium: Double? = null,
    val bid: Double? = null,
    val ask: Double? = null,
    val last: Double? = null,
    val volume: Long? = null,
    val openInterest: Long? = null,
    val impliedVolatility: Double? = null,
    val expirationDate: String? = null,
    val settlementDate: String? = null,
    val itm: Boolean? = null
)

/**
 * Opcioni "lanac" za jednu akciju — grupisan po settlement datumu i strike-u.
 */
@JsonClass(generateAdapter = true)
data class OptionChainEntryDto(
    val strikePrice: Double,
    val call: OptionDto? = null,
    val put: OptionDto? = null
)

@JsonClass(generateAdapter = true)
data class OptionChainDto(
    val settlementDate: String? = null,
    val entries: List<OptionChainEntryDto> = emptyList()
)
