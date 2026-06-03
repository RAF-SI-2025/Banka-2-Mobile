package rs.raf.banka2.mobile.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * ME-11 tests: BigDecimal overload-i + parseBigDecimal — koriste se za Card / Savings
 * DTO-ove koji su BE-side BigDecimal (spec C2 §255 — preciznost u monetarnim operacijama).
 */
class MoneyFormatterBigDecimalTest {

    @Test
    fun format_bigDecimal_matchesDoubleFormat() {
        val bd = BigDecimal("12345.67")
        assertEquals("12.345,67", MoneyFormatter.format(bd))
    }

    @Test
    fun formatWithCurrency_bigDecimal_appendsCurrency() {
        val bd = BigDecimal("1000.50")
        assertEquals("1.000,50 RSD", MoneyFormatter.formatWithCurrency(bd, "RSD"))
    }

    @Test
    fun formatWithCurrency_bigDecimal_nullCurrency_omitsSuffix() {
        val bd = BigDecimal("250.00")
        assertEquals("250,00", MoneyFormatter.formatWithCurrency(bd, null))
    }

    @Test
    fun format_bigDecimal_zeroFractionRounds() {
        val bd = BigDecimal("12345.67")
        assertEquals("12.346", MoneyFormatter.format(bd, fractionDigits = 0))
    }

    @Test
    fun parseBigDecimal_validInput_returnsBigDecimal() {
        val parsed = MoneyFormatter.parseBigDecimal("1.234,56")
        assertEquals(BigDecimal("1234.56"), parsed)
    }

    @Test
    fun parseBigDecimal_blankInput_returnsNull() {
        assertNull(MoneyFormatter.parseBigDecimal(""))
        assertNull(MoneyFormatter.parseBigDecimal("   "))
    }

    @Test
    fun parseBigDecimal_invalidInput_returnsNull() {
        assertNull(MoneyFormatter.parseBigDecimal("abc"))
    }

    @Test
    fun parseBigDecimal_handlesSpaces() {
        val parsed = MoneyFormatter.parseBigDecimal(" 1 234,56 ")
        assertEquals(BigDecimal("1234.56"), parsed)
    }

    @Test
    fun parseBigDecimal_handlesPlainNumber() {
        assertEquals(BigDecimal("100"), MoneyFormatter.parseBigDecimal("100"))
        assertEquals(BigDecimal("100.50"), MoneyFormatter.parseBigDecimal("100,50"))
    }

    // ─── R7-2033 / R1-581 [money]: locale-svestan parser ───────────────────

    @Test
    fun parseBigDecimal_srRsThousandsAndComma_parsesExactly() {
        // "1.234,56" → 1234.56 (NE 0, NE 1234560)
        assertEquals(BigDecimal("1234.56"), MoneyFormatter.parseBigDecimal("1.234,56"))
    }

    @Test
    fun parseBigDecimal_enDotDecimal_notStrippedTo100x() {
        // R7-2033: ranije je "1234.56" gubilo tacku → "123456" (100× preveliko).
        // Sada tacka sa 2 cifre posle = decimalni separator.
        assertEquals(BigDecimal("1234.56"), MoneyFormatter.parseBigDecimal("1234.56"))
    }

    @Test
    fun parseBigDecimal_enThousandsCommaDotDecimal_parsesExactly() {
        // "1,234.56" (EN sa hiljadama) → 1234.56
        assertEquals(BigDecimal("1234.56"), MoneyFormatter.parseBigDecimal("1,234.56"))
    }

    @Test
    fun parseBigDecimal_srRsThousandsDotOnly_treatedAsGrouping() {
        // "1.000" (sr-RS hiljade) → 1000, NE 1.0
        assertEquals(BigDecimal("1000"), MoneyFormatter.parseBigDecimal("1.000"))
        assertEquals(BigDecimal("250000"), MoneyFormatter.parseBigDecimal("250.000"))
    }

    @Test
    fun parseBigDecimal_largeAmount_noPrecisionLoss() {
        // Veliki iznos koji bi Double zaokruzio — BigDecimal mora ostati egzaktan.
        assertEquals(BigDecimal("12345678.99"), MoneyFormatter.parseBigDecimal("12.345.678,99"))
    }

    // ─── R1-581 [money]: format(BigDecimal) bez toDouble() preciznost ──────

    @Test
    fun format_bigDecimal_largeValue_noDoublePrecisionLoss() {
        // 9_999_999_999_999.99 ima 15 znacajnih cifara — Double bi izgubio
        // poslednju decimalu; DecimalFormat nad BigDecimal je egzaktan.
        val bd = BigDecimal("9999999999999.99")
        assertEquals("9.999.999.999.999,99", MoneyFormatter.format(bd))
    }
}
