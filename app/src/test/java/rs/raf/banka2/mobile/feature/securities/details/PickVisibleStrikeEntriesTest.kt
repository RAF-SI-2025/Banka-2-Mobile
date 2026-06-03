package rs.raf.banka2.mobile.feature.securities.details

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * Spec Celina 3 §467: prikaz strike redova oko Shared Price (currentPrice).
 * Ovi testovi pokrivaju pure filter funkciju iz SecuritiesDetailsViewModel.kt.
 *
 * ME-11: strike-ovi su sada BigDecimal (money) — helper je generican nad
 * `(T) -> BigDecimal` selectorom.
 */
class PickVisibleStrikeEntriesTest {

    private fun bd(v: String) = BigDecimal(v)

    private val strikes = listOf(
        bd("100"), bd("105"), bd("110"), bd("115"), bd("120"), bd("125"), bd("130")
    )

    @Test
    fun rowsAroundPrice_two_picksTwoBelowAndTwoAbovePivot() {
        // currentPrice = 117 -> pivot na 120 (prvi >= 117, indeks 4).
        // Sa rows=2 ocekujemo indekse [2..6] = 110, 115, 120, 125, 130.
        val result = pickVisibleStrikeEntries(strikes, 2, { it }, bd("117"))
        assertEquals(listOf(bd("110"), bd("115"), bd("120"), bd("125"), bd("130")), result)
    }

    @Test
    fun rowsAroundPrice_one_picksOneBelowAndOneAbove() {
        // currentPrice = 112 -> pivot na 115 (indeks 3). Sa rows=1: [2..4] = 110, 115, 120.
        val result = pickVisibleStrikeEntries(strikes, 1, { it }, bd("112"))
        assertEquals(listOf(bd("110"), bd("115"), bd("120")), result)
    }

    @Test
    fun pivotAtEnd_currentPriceAboveAllStrikes_clampsToLastIndex() {
        // currentPrice = 200 — vise od svih strike-ova. Pivot ide na last index.
        // Sa rows=3: [3..6] = 115, 120, 125, 130 (last 4 zbog clamp).
        val result = pickVisibleStrikeEntries(strikes, 3, { it }, bd("200"))
        assertEquals(listOf(bd("115"), bd("120"), bd("125"), bd("130")), result)
    }

    @Test
    fun pivotAtStart_currentPriceBelowAllStrikes_picksFirstRowsAround() {
        // currentPrice = 50 — manje od svih. Pivot na 100 (indeks 0).
        // Sa rows=2: [0..2] = 100, 105, 110 (clamp na pocetak).
        val result = pickVisibleStrikeEntries(strikes, 2, { it }, bd("50"))
        assertEquals(listOf(bd("100"), bd("105"), bd("110")), result)
    }

    @Test
    fun rowsLargerThanList_returnsAll() {
        // 7 strike-ova, rows=10 -> 2*10 > 7 pa se vrati cela lista.
        val result = pickVisibleStrikeEntries(strikes, 10, { it }, bd("115"))
        assertEquals(strikes, result)
    }

    @Test
    fun rowsZero_returnsEmpty() {
        val result = pickVisibleStrikeEntries(strikes, 0, { it }, bd("115"))
        assertEquals(emptyList<BigDecimal>(), result)
    }

    @Test
    fun rowsNegative_returnsEmpty() {
        val result = pickVisibleStrikeEntries(strikes, -3, { it }, bd("115"))
        assertEquals(emptyList<BigDecimal>(), result)
    }

    @Test
    fun emptyInput_returnsEmpty() {
        val result = pickVisibleStrikeEntries(emptyList<BigDecimal>(), 5, { it }, bd("115"))
        assertEquals(emptyList<BigDecimal>(), result)
    }

    @Test
    fun currentPriceExactlyOnStrike_pivotPicksThatStrike() {
        // currentPrice = 115 -> pivot je 115 (prvi >= 115, indeks 3).
        // Sa rows=2: [1..5] = 105, 110, 115, 120, 125.
        val result = pickVisibleStrikeEntries(strikes, 2, { it }, bd("115"))
        assertEquals(listOf(bd("105"), bd("110"), bd("115"), bd("120"), bd("125")), result)
    }

    @Test
    fun customSelector_worksWithDataClass() {
        // Verifikacija da generic selector radi sa proizvoljnim entitetom.
        data class Opt(val ticker: String, val strike: BigDecimal)
        val opts = listOf(
            Opt("A", bd("10")), Opt("B", bd("20")), Opt("C", bd("30")),
            Opt("D", bd("40")), Opt("E", bd("50"))
        )
        val result = pickVisibleStrikeEntries(opts, 1, { it.strike }, bd("35"))
        // pivot = D (indeks 3, prvi >= 35), rows=1: [2..4] = C, D, E.
        assertEquals(listOf("C", "D", "E"), result.map { it.ticker })
    }
}
