package rs.raf.banka2.mobile.core.format

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formatiranje novca u srpski locale (sr-RS): tacka kao separator hiljada,
 * zarez za decimale. Sve aplikacije koriste ovaj util — ne formatiraj rucno.
 *
 * ME-11: dodate BigDecimal overload-e jer DTO-ovi sada koriste BigDecimal
 * (spec C2 §255). Postojeci Double-call sites ostaju funkcionalni zbog
 * type-spec overload-a.
 */
object MoneyFormatter {

    private val srLocale: Locale = Locale.Builder().setLanguage("sr").setRegion("RS").build()
    private val symbols = DecimalFormatSymbols(srLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val withDecimals = DecimalFormat("#,##0.00", symbols)
    private val withoutDecimals = DecimalFormat("#,##0", symbols)

    /** "12.345,67" */
    fun format(amount: Double, fractionDigits: Int = 2): String {
        return when (fractionDigits) {
            0 -> withoutDecimals.format(amount)
            else -> withDecimals.format(amount)
        }
    }

    /** ME-11: BigDecimal overload — koristi se za Card / Payment / Transfer / Account DTO. */
    fun format(amount: BigDecimal, fractionDigits: Int = 2): String =
        format(amount.toDouble(), fractionDigits)

    /** "12.345,67 RSD" */
    fun formatWithCurrency(amount: Double, currency: String?, fractionDigits: Int = 2): String {
        val number = format(amount, fractionDigits)
        return if (currency.isNullOrBlank()) number else "$number $currency"
    }

    /** ME-11: BigDecimal overload. */
    fun formatWithCurrency(amount: BigDecimal, currency: String?, fractionDigits: Int = 2): String =
        formatWithCurrency(amount.toDouble(), currency, fractionDigits)

    /** Konvertuje iznos sa zarezom u Double (parser je tolerantan na razmake i razne separatore). */
    fun parse(input: String): Double? {
        if (input.isBlank()) return null
        val sanitized = input.trim()
            .replace(" ", "")
            .replace(".", "")
            .replace(',', '.')
        return sanitized.toDoubleOrNull()
    }

    /** ME-11: vraca BigDecimal za precizan price/quantity prikaz. */
    fun parseBigDecimal(input: String): BigDecimal? {
        if (input.isBlank()) return null
        val sanitized = input.trim()
            .replace(" ", "")
            .replace(".", "")
            .replace(',', '.')
        return try {
            BigDecimal(sanitized)
        } catch (_: NumberFormatException) {
            null
        }
    }
}

/**
 * Mapa valuta na flag emoji + boju za UI dekoraciju.
 */
object CurrencyVisuals {

    private val flags = mapOf(
        "RSD" to "🇷🇸",
        "EUR" to "🇪🇺",
        "USD" to "🇺🇸",
        "GBP" to "🇬🇧",
        "CHF" to "🇨🇭",
        "JPY" to "🇯🇵",
        "AUD" to "🇦🇺",
        "CAD" to "🇨🇦",
        "CNY" to "🇨🇳",
        "RUB" to "🇷🇺"
    )

    fun flag(currency: String?): String = flags[currency?.uppercase()] ?: "🏳️"

    fun symbol(currency: String?): String = when (currency?.uppercase()) {
        "RSD" -> "RSD"
        "EUR" -> "€"
        "USD" -> "$"
        "GBP" -> "£"
        "JPY" -> "¥"
        else -> currency.orEmpty()
    }
}
