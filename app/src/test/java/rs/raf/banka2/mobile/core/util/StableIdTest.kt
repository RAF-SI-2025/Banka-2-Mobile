package rs.raf.banka2.mobile.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * P1-mobile-trading-1 (R2-1482): stableLongId — determinisicki 64-bit id iz
 * String-a (inter-bank OTC UUID → Compose list key). Mora biti stabilan i imati
 * nizu sansu kolizije od `String.hashCode().toLong()`.
 */
class StableIdTest {

    @Test
    fun sameStringYieldsSameId() {
        assertEquals("111:abc".stableLongId(), "111:abc".stableLongId())
    }

    @Test
    fun differentStringsYieldDifferentIds() {
        assertNotEquals("111:abc".stableLongId(), "111:abd".stableLongId())
        assertNotEquals("222:x:AAPL".stableLongId(), "222:x:MSFT".stableLongId())
    }

    @Test
    fun emptyStringIsDeterministic() {
        assertEquals("".stableLongId(), "".stableLongId())
    }
}
