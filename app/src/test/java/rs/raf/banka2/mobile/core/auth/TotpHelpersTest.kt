package rs.raf.banka2.mobile.core.auth

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TODO_final C2 #3 — TOTP helpers test suite.
 *
 * Pure JUnit, bez Android API zavisnosti (sem [Color]). Pokrecemo
 * sa fiksnim epoch-om u milisekundama da rezultat bude deterministican.
 */
class TotpHelpersTest {

    // ─── getTotpSecondsLeft() ─────────────────────────────────────────────

    @Test
    fun secondsLeft_atEpochZero_returns30() {
        // 0s % 30 = 0 -> 30 - 0 = 30
        assertEquals(30, TotpHelpers.getTotpSecondsLeft(0L))
    }

    @Test
    fun secondsLeft_at1sIntoWindow_returns29() {
        // 1s % 30 = 1 -> 30 - 1 = 29
        assertEquals(29, TotpHelpers.getTotpSecondsLeft(1_000L))
    }

    @Test
    fun secondsLeft_at29sIntoWindow_returns1() {
        assertEquals(1, TotpHelpers.getTotpSecondsLeft(29_000L))
    }

    @Test
    fun secondsLeft_at30sExactRollover_returns30() {
        // 30s % 30 = 0 -> 30 - 0 = 30 (novi prozor)
        assertEquals(30, TotpHelpers.getTotpSecondsLeft(30_000L))
    }

    @Test
    fun secondsLeft_at59s_returns1() {
        // 59s % 30 = 29 -> 30 - 29 = 1
        assertEquals(1, TotpHelpers.getTotpSecondsLeft(59_000L))
    }

    @Test
    fun secondsLeft_at60s_returns30Again() {
        assertEquals(30, TotpHelpers.getTotpSecondsLeft(60_000L))
    }

    @Test
    fun secondsLeft_atArbitraryEpoch_isDeterministic() {
        // Random arbitrary epoch: 1_700_000_015 sekundi
        // 1_700_000_015 % 30 = 5 (5s u prozor), pa secondsLeft = 25
        val nowMs = 1_700_000_015L * 1000L
        assertEquals(25, TotpHelpers.getTotpSecondsLeft(nowMs))
    }

    // ─── getTotpProgressFraction() ────────────────────────────────────────

    @Test
    fun progress_at30s_isOne() {
        assertEquals(1.0f, TotpHelpers.getTotpProgressFraction(30), 0.001f)
    }

    @Test
    fun progress_at15s_isHalf() {
        assertEquals(0.5f, TotpHelpers.getTotpProgressFraction(15), 0.001f)
    }

    @Test
    fun progress_at0s_isZero() {
        assertEquals(0.0f, TotpHelpers.getTotpProgressFraction(0), 0.001f)
    }

    @Test
    fun progress_clampsToZeroForNegative() {
        // coerceIn(0, 30) defenzivno cuva ulaz iz overshoot-a
        assertEquals(0.0f, TotpHelpers.getTotpProgressFraction(-5), 0.001f)
    }

    @Test
    fun progress_clampsToOneForOvershoot() {
        // 100 → coerceIn(0, 30) = 30 → progress = 1.0f
        assertEquals(1.0f, TotpHelpers.getTotpProgressFraction(100), 0.001f)
    }

    // ─── getTotpIndicatorColor() ──────────────────────────────────────────

    @Test
    fun color_above10s_isEmerald() {
        val color = TotpHelpers.getTotpIndicatorColor(15)
        assertEquals(Color(0xFF10B981), color)
    }

    @Test
    fun color_at10sBoundary_isAmber() {
        val color = TotpHelpers.getTotpIndicatorColor(10)
        assertEquals(Color(0xFFF59E0B), color)
    }

    @Test
    fun color_at4s_isAmber() {
        // > 3, <= 10
        val color = TotpHelpers.getTotpIndicatorColor(4)
        assertEquals(Color(0xFFF59E0B), color)
    }

    @Test
    fun color_at3s_isRed() {
        val color = TotpHelpers.getTotpIndicatorColor(3)
        assertEquals(Color(0xFFEF4444), color)
    }

    @Test
    fun color_at0s_isRed() {
        val color = TotpHelpers.getTotpIndicatorColor(0)
        assertEquals(Color(0xFFEF4444), color)
    }

    // ─── Period invariant ────────────────────────────────────────────────

    @Test
    fun period_is30Seconds() {
        // RFC 6238 default — drugi delovi sistema racunaju na ovo.
        assertEquals(30, TotpHelpers.TOTP_PERIOD_SECONDS)
    }

    @Test
    fun secondsLeft_isAlwaysBetween1And30() {
        // Slucajan opseg epoch-ova — invariant.
        for (sec in 0L..120L) {
            val left = TotpHelpers.getTotpSecondsLeft(sec * 1000L)
            assertTrue("secondsLeft=$left van [1,30] za sec=$sec", left in 1..30)
        }
    }
}
