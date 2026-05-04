package rs.raf.banka2.mobile.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testovi za sr-RS locale formatiranje novca + parser.
 * Pure JVM unit testovi (nema Android API dep-a) — pokreces preko `./gradlew testDebugUnitTest`.
 */
class MoneyFormatterTest {

    // ─── format() tests ─────────────────────────────────────────────────

    @Test
    fun format_withDecimals_useCommaSeparator() {
        // sr-RS: zarez za decimale, tacka za hiljade
        assertEquals("12.345,67", MoneyFormatter.format(12345.67))
    }

    @Test
    fun format_zeroFraction_roundsToInteger() {
        // 12345.67 → "12.346" (HALF_EVEN/HALF_UP rounding default)
        assertEquals("12.346", MoneyFormatter.format(12345.67, fractionDigits = 0))
    }

    @Test
    fun format_zeroFraction_roundsDown() {
        assertEquals("12.345", MoneyFormatter.format(12345.4, fractionDigits = 0))
    }

    @Test
    fun format_smallNumber_noThousandsSeparator() {
        assertEquals("99,99", MoneyFormatter.format(99.99))
    }

    @Test
    fun format_negativeAmount_keepsSign() {
        assertEquals("-1.234,50", MoneyFormatter.format(-1234.50))
    }

    @Test
    fun format_zero_returnsZeroDecimal() {
        assertEquals("0,00", MoneyFormatter.format(0.0))
    }

    // ─── formatWithCurrency() tests ─────────────────────────────────────

    @Test
    fun formatWithCurrency_appendsCurrencyCode() {
        assertEquals("1.234,56 RSD", MoneyFormatter.formatWithCurrency(1234.56, "RSD"))
    }

    @Test
    fun formatWithCurrency_nullCurrency_omitsSuffix() {
        assertEquals("1.234,56", MoneyFormatter.formatWithCurrency(1234.56, null))
    }

    @Test
    fun formatWithCurrency_blankCurrency_omitsSuffix() {
        assertEquals("1.234,56", MoneyFormatter.formatWithCurrency(1234.56, "  "))
    }

    // ─── parse() tests ──────────────────────────────────────────────────

    @Test
    fun parse_srStyle_handlesDotAsThousandsAndCommaAsDecimal() {
        assertEquals(12345.67, MoneyFormatter.parse("12.345,67")!!, 0.001)
    }

    @Test
    fun parse_blank_returnsNull() {
        assertNull(MoneyFormatter.parse(""))
        assertNull(MoneyFormatter.parse("   "))
    }

    @Test
    fun parse_invalid_returnsNull() {
        assertNull(MoneyFormatter.parse("abc"))
    }

    @Test
    fun parse_withSpaces_strips() {
        assertEquals(1234.5, MoneyFormatter.parse(" 1 234,5 ")!!, 0.001)
    }

    // ─── CurrencyVisuals tests ──────────────────────────────────────────

    @Test
    fun currencyFlag_knownCurrency_returnsEmoji() {
        assertEquals("🇷🇸", CurrencyVisuals.flag("RSD"))
    }

    @Test
    fun currencyFlag_unknown_returnsWhiteFlag() {
        assertEquals("🏳️", CurrencyVisuals.flag("XYZ"))
    }

    @Test
    fun currencySymbol_eur_returnsEuroSign() {
        assertEquals("€", CurrencyVisuals.symbol("EUR"))
    }

    @Test
    fun currencySymbol_unknown_passthrough() {
        assertEquals("XYZ", CurrencyVisuals.symbol("XYZ"))
    }
}
