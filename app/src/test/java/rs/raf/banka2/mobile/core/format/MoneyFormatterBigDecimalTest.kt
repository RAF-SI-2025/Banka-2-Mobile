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
        // 100.50 - sr-RS koristi tacku kao hiljaditi separator, pa "100.50" parsira kao "10050"
        // (postojeci behavior parse-a). Za decimale koristi se zarez.
        assertEquals(BigDecimal("100.50"), MoneyFormatter.parseBigDecimal("100,50"))
    }
}
