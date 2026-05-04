package rs.raf.banka2.mobile.feature.securities.details

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec Celina 3 §467: prikaz strike redova oko Shared Price (currentPrice).
 * Ovi testovi pokrivaju pure filter funkciju iz SecuritiesDetailsViewModel.kt.
 */
class PickVisibleStrikeEntriesTest {

    private val strikes = listOf(100.0, 105.0, 110.0, 115.0, 120.0, 125.0, 130.0)

    @Test
    fun rowsAroundPrice_two_picksTwoBelowAndTwoAbovePivot() {
        // currentPrice = 117 -> pivot na 120 (prvi >= 117, indeks 4).
        // Sa rows=2 ocekujemo indekse [2..6] = 110, 115, 120, 125, 130.
        val result = pickVisibleStrikeEntries(strikes, 2, { it }, 117.0)
        assertEquals(listOf(110.0, 115.0, 120.0, 125.0, 130.0), result)
    }

    @Test
    fun rowsAroundPrice_one_picksOneBelowAndOneAbove() {
        // currentPrice = 112 -> pivot na 115 (indeks 3). Sa rows=1: [2..4] = 110, 115, 120.
        val result = pickVisibleStrikeEntries(strikes, 1, { it }, 112.0)
        assertEquals(listOf(110.0, 115.0, 120.0), result)
    }

    @Test
    fun pivotAtEnd_currentPriceAboveAllStrikes_clampsToLastIndex() {
        // currentPrice = 200 — vise od svih strike-ova. Pivot ide na last index.
        // Sa rows=3: [3..6] = 115, 120, 125, 130 (last 4 zbog clamp).
        val result = pickVisibleStrikeEntries(strikes, 3, { it }, 200.0)
        assertEquals(listOf(115.0, 120.0, 125.0, 130.0), result)
    }

    @Test
    fun pivotAtStart_currentPriceBelowAllStrikes_picksFirstRowsAround() {
        // currentPrice = 50 — manje od svih. Pivot na 100 (indeks 0).
        // Sa rows=2: [0..2] = 100, 105, 110 (clamp na pocetak).
        val result = pickVisibleStrikeEntries(strikes, 2, { it }, 50.0)
        assertEquals(listOf(100.0, 105.0, 110.0), result)
    }

    @Test
    fun rowsLargerThanList_returnsAll() {
        // 7 strike-ova, rows=10 -> 2*10 > 7 pa se vrati cela lista.
        val result = pickVisibleStrikeEntries(strikes, 10, { it }, 115.0)
        assertEquals(strikes, result)
    }

    @Test
    fun rowsZero_returnsEmpty() {
        val result = pickVisibleStrikeEntries(strikes, 0, { it }, 115.0)
        assertEquals(emptyList<Double>(), result)
    }

    @Test
    fun rowsNegative_returnsEmpty() {
        val result = pickVisibleStrikeEntries(strikes, -3, { it }, 115.0)
        assertEquals(emptyList<Double>(), result)
    }

    @Test
    fun emptyInput_returnsEmpty() {
        val result = pickVisibleStrikeEntries(emptyList<Double>(), 5, { it }, 115.0)
        assertEquals(emptyList<Double>(), result)
    }

    @Test
    fun currentPriceExactlyOnStrike_pivotPicksThatStrike() {
        // currentPrice = 115 -> pivot je 115 (prvi >= 115, indeks 3).
        // Sa rows=2: [1..5] = 105, 110, 115, 120, 125.
        val result = pickVisibleStrikeEntries(strikes, 2, { it }, 115.0)
        assertEquals(listOf(105.0, 110.0, 115.0, 120.0, 125.0), result)
    }

    @Test
    fun customSelector_worksWithDataClass() {
        // Verifikacija da generic selector radi sa proizvoljnim entitetom.
        data class Opt(val ticker: String, val strike: Double)
        val opts = listOf(
            Opt("A", 10.0), Opt("B", 20.0), Opt("C", 30.0),
            Opt("D", 40.0), Opt("E", 50.0)
        )
        val result = pickVisibleStrikeEntries(opts, 1, { it.strike }, 35.0)
        // pivot = D (indeks 3, prvi >= 35), rows=1: [2..4] = C, D, E.
        assertEquals(listOf("C", "D", "E"), result.map { it.ticker })
    }
}
