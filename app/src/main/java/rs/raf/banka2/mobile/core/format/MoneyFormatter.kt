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

    /**
     * ME-11: BigDecimal overload — koristi se za Card / Payment / Transfer / Account DTO.
     *
     * R1-581: NE radi `amount.toDouble()` — to gubi preciznost na velikim iznosima
     * (Double ima 15-16 znacajnih cifara) i moze proizvesti pogresan zaokruzen
     * prikaz salda. DecimalFormat formatira BigDecimal direktno bez Double koraka.
     */
    fun format(amount: BigDecimal, fractionDigits: Int = 2): String {
        return when (fractionDigits) {
            0 -> withoutDecimals.format(amount)
            else -> withDecimals.format(amount)
        }
    }

    /** "12.345,67 RSD" */
    fun formatWithCurrency(amount: Double, currency: String?, fractionDigits: Int = 2): String {
        val number = format(amount, fractionDigits)
        return if (currency.isNullOrBlank()) number else "$number $currency"
    }

    /** ME-11: BigDecimal overload. R1-581: formatira BigDecimal direktno (bez `toDouble()`). */
    fun formatWithCurrency(amount: BigDecimal, currency: String?, fractionDigits: Int = 2): String {
        val number = format(amount, fractionDigits)
        return if (currency.isNullOrBlank()) number else "$number $currency"
    }

    /** Konvertuje iznos sa zarezom u Double (parser je tolerantan na razmake i razne separatore). */
    fun parse(input: String): Double? = parseBigDecimal(input)?.toDouble()

    /**
     * R7-2033 / R1-2028 [money]: vraca BigDecimal za precizan iznos. Parser je
     * locale-svestan i NE strip-uje slepo svaku tacku (raniji bug: "1234.56" →
     * "123456" = 100× preveliko na EN-locale unosu sa tastature).
     *
     * Heuristika za decimalni separator:
     *  - oba `.` i `,` prisutna → poslednji znak je decimalni separator
     *    (sr-RS "1.234,56" → zarez; EN-sa-hiljadama "1,234.56" → tacka),
     *    drugi separator je grouping i uklanja se.
     *  - samo `,` → decimalni separator (sr-RS).
     *  - samo `.` → ako lici na grouping (vise tacaka, ili tacno 3 cifre posle
     *    poslednje tacke i bar 4 cifre ukupno bez znaka, npr "1.000") → grouping,
     *    inace decimalni separator (EN "1234.56").
     */
    fun parseBigDecimal(input: String): BigDecimal? {
        if (input.isBlank()) return null
        val cleaned = input.trim().replace(" ", "").replace(" ", "")
        if (cleaned.isEmpty()) return null

        val hasDot = cleaned.contains('.')
        val hasComma = cleaned.contains(',')

        val normalized: String = when {
            hasDot && hasComma -> {
                val lastDot = cleaned.lastIndexOf('.')
                val lastComma = cleaned.lastIndexOf(',')
                if (lastComma > lastDot) {
                    // zarez je decimalni (sr-RS): ukloni tacke (grouping), zarez → tacka
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    // tacka je decimalni (EN): ukloni zareze (grouping)
                    cleaned.replace(",", "")
                }
            }
            hasComma -> cleaned.replace(',', '.')
            hasDot -> {
                val dotCount = cleaned.count { it == '.' }
                val digitsAfterLastDot = cleaned.substringAfterLast('.').count { it.isDigit() }
                val digitsBeforeLastDot = cleaned.substringBeforeLast('.').count { it.isDigit() }
                val looksLikeGrouping = dotCount > 1 ||
                    (digitsAfterLastDot == 3 && digitsBeforeLastDot >= 1)
                if (looksLikeGrouping) cleaned.replace(".", "") else cleaned
            }
            else -> cleaned
        }

        return try {
            BigDecimal(normalized)
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
}
