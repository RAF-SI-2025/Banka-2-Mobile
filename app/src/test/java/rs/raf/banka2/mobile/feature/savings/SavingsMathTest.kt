package rs.raf.banka2.mobile.feature.savings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * Testovi za SavingsMath utility (pure math, bez Android API dep-a).
 * Pokreces preko `./gradlew testDebugUnitTest`.
 *
 * BigDecimal.equals() proverava i vrednost i scale — sve ocekivane vrednosti
 * imaju eksplicitno scale=4 da se poklapaju sa rezultatima iz SavingsMath.
 */
class SavingsMathTest {

    // ─── monthlyInterest() ───────────────────────────────────────────────

    @Test
    fun monthlyInterest_rsd4percent_200k() {
        // 200000 * 4 / 1200 = 666.6666... → HALF_UP scale 4 = 666.6667
        assertEquals(
            BigDecimal("666.6667"),
            SavingsMath.monthlyInterest(BigDecimal("200000"), BigDecimal("4.00"))
        )
    }

    @Test
    fun monthlyInterest_eur2percent_1000() {
        // 1000 * 2 / 1200 = 1.6666... → HALF_UP scale 4 = 1.6667
        assertEquals(
            BigDecimal("1.6667"),
            SavingsMath.monthlyInterest(BigDecimal("1000"), BigDecimal("2.00"))
        )
    }

    @Test
    fun monthlyInterest_zeroPrincipal_returnsZero() {
        assertEquals(
            BigDecimal("0.0000"),
            SavingsMath.monthlyInterest(BigDecimal.ZERO, BigDecimal("4.00"))
        )
    }

    // ─── totalInterestOverTerm() ─────────────────────────────────────────

    @Test
    fun totalInterest_12months_4percent_200k() {
        // monthlyInterest = 666.6667; * 12 = 8000.0004
        assertEquals(
            BigDecimal("8000.0004"),
            SavingsMath.totalInterestOverTerm(BigDecimal("200000"), BigDecimal("4.00"), 12)
        )
    }

    @Test
    fun totalInterest_6months_3percent_50k() {
        // 50000 * 3 / 1200 = 125.0000; * 6 = 750.0000
        assertEquals(
            BigDecimal("750.0000"),
            SavingsMath.totalInterestOverTerm(BigDecimal("50000"), BigDecimal("3.00"), 6)
        )
    }

    // ─── earlyWithdrawalPenalty() ────────────────────────────────────────

    @Test
    fun earlyWithdrawalPenalty_200k_is1percent() {
        // 200000 * 0.01 = 2000.0000
        assertEquals(
            BigDecimal("2000.0000"),
            SavingsMath.earlyWithdrawalPenalty(BigDecimal("200000"))
        )
    }

    @Test
    fun earlyWithdrawalPenalty_100eur_equals1() {
        // 100 * 0.01 = 1.0000
        assertEquals(
            BigDecimal("1.0000"),
            SavingsMath.earlyWithdrawalPenalty(BigDecimal("100"))
        )
    }

    @Test
    fun earlyWithdrawalPenalty_zeroPrincipal_returnsZero() {
        assertEquals(
            BigDecimal("0.0000"),
            SavingsMath.earlyWithdrawalPenalty(BigDecimal.ZERO)
        )
    }
}
