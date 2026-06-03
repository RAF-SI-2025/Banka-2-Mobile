package rs.raf.banka2.mobile.feature.pricealerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertCondition
import java.math.BigDecimal

/**
 * Unit testovi za pure PriceAlertDialogViewModel validacionu logiku.
 *
 * `validate(threshold, currentPrice, condition)` vraca user-facing poruku ako
 * threshold nije validan, inace null. `percentDifference()` vraca procenat razlike
 * threshold-a od trenutne cene.
 */
class PriceAlertDialogValidationTest {

    private val ABOVE = PriceAlertCondition.ABOVE
    private val BELOW = PriceAlertCondition.BELOW

    // ─── validate() ───────────────────────────────────────────

    @Test
    fun validate_nullThreshold_returnsError() {
        val result = PriceAlertDialogViewModel.validate(threshold = null, currentPrice = 100.0, condition = ABOVE)
        assertNotNull(result)
        assertTrue(result!!.contains("pozitivan"))
    }

    @Test
    fun validate_zeroThreshold_returnsError() {
        val result = PriceAlertDialogViewModel.validate(BigDecimal.ZERO, 100.0, ABOVE)
        assertNotNull(result)
    }

    @Test
    fun validate_negativeThreshold_returnsError() {
        val result = PriceAlertDialogViewModel.validate(BigDecimal("-5"), 100.0, ABOVE)
        assertNotNull(result)
    }

    @Test
    fun validate_thresholdEqualsCurrentPrice_returnsError() {
        // Alarm bi se okinuo odmah na sledecem scheduler tick-u
        val result = PriceAlertDialogViewModel.validate(BigDecimal("100.00"), 100.0, ABOVE)
        assertNotNull(result)
        assertTrue(result!!.contains("razlicit"))
    }

    @Test
    fun validate_above_thresholdAboveCurrentPrice_returnsNull() {
        val result = PriceAlertDialogViewModel.validate(BigDecimal("150.00"), 100.0, ABOVE)
        assertNull(result)
    }

    @Test
    fun validate_below_thresholdBelowCurrentPrice_returnsNull() {
        val result = PriceAlertDialogViewModel.validate(BigDecimal("50.00"), 100.0, BELOW)
        assertNull(result)
    }

    // ─── R1-598: SMER mora biti konzistentan sa pragom ───

    @Test
    fun validate_above_thresholdBelowCurrentPrice_returnsError() {
        // ABOVE alarm sa pragom ISPOD cene → bi se okinuo odmah → mora biti greska.
        val result = PriceAlertDialogViewModel.validate(BigDecimal("50.00"), 100.0, ABOVE)
        assertNotNull(result)
        assertTrue(result!!.contains("iznad"))
    }

    @Test
    fun validate_below_thresholdAboveCurrentPrice_returnsError() {
        // BELOW alarm sa pragom IZNAD cene → bi se okinuo odmah → mora biti greska.
        val result = PriceAlertDialogViewModel.validate(BigDecimal("150.00"), 100.0, BELOW)
        assertNotNull(result)
        assertTrue(result!!.contains("ispod"))
    }

    @Test
    fun validate_currentPriceNull_skipsDirectionCheck() {
        // Kad listing nema cenu (FOREX edge case), prihvatamo bilo koji pozitivan prag.
        val result = PriceAlertDialogViewModel.validate(BigDecimal("100.00"), null, ABOVE)
        assertNull(result)
    }

    @Test
    fun validate_currentPriceZero_skipsDirectionCheck() {
        val result = PriceAlertDialogViewModel.validate(BigDecimal("100.00"), 0.0, BELOW)
        assertNull(result)
    }

    // ─── percentDifference() ──────────────────────────────────

    @Test
    fun percentDifference_thresholdAboveCurrentPrice_returnsPositive() {
        // (150 - 100) / 100 * 100 = 50%
        val result = PriceAlertDialogViewModel.percentDifference(BigDecimal("150"), 100.0)
        assertEquals(50.0, result!!, 0.001)
    }

    @Test
    fun percentDifference_thresholdBelowCurrentPrice_returnsNegative() {
        // (75 - 100) / 100 * 100 = -25%
        val result = PriceAlertDialogViewModel.percentDifference(BigDecimal("75"), 100.0)
        assertEquals(-25.0, result!!, 0.001)
    }

    @Test
    fun percentDifference_thresholdEqualsCurrentPrice_returnsZero() {
        val result = PriceAlertDialogViewModel.percentDifference(BigDecimal("100"), 100.0)
        assertEquals(0.0, result!!, 0.001)
    }

    @Test
    fun percentDifference_currentPriceZero_returnsNull() {
        // Sprecava deljenje sa nulom — UI prikazuje "—".
        val result = PriceAlertDialogViewModel.percentDifference(BigDecimal("100"), 0.0)
        assertNull(result)
    }

    @Test
    fun percentDifference_currentPriceNull_returnsNull() {
        val result = PriceAlertDialogViewModel.percentDifference(BigDecimal("100"), null)
        assertNull(result)
    }

    @Test
    fun percentDifference_thresholdNull_returnsNull() {
        val result = PriceAlertDialogViewModel.percentDifference(null, 100.0)
        assertNull(result)
    }
}
