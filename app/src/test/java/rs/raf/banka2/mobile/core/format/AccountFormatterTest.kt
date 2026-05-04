package rs.raf.banka2.mobile.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountFormatterTest {

    // ─── formatAccountNumber() ──────────────────────────────────────────

    @Test
    fun formatAccountNumber_18digits_groups3DashTrunkDashLast2() {
        // 18-cifren broj racuna po Celina 2 spec-u: 3 cifre banke + 4 filijale + 9
        // random + 2 tip racuna. Formatter koristi 3-12-{rest} podelu (>=17 chars):
        // "265" + "-" + "000000000123" + "-" + "45678"
        assertEquals(
            "265-000000000123-45678",
            AccountFormatter.formatAccountNumber("26500000000012345678")
        )
    }

    @Test
    fun formatAccountNumber_blank_returnsDash() {
        assertEquals("—", AccountFormatter.formatAccountNumber(""))
        assertEquals("—", AccountFormatter.formatAccountNumber(null))
    }

    @Test
    fun formatAccountNumber_alreadyFormatted_strippedAndReformatted() {
        // Input vec ima dasove — strip-ujemo pa formatiramo (3-12-rest split).
        val input = "265-000000-000-123-456-78"
        val out = AccountFormatter.formatAccountNumber(input)
        assertEquals("265-000000000123-45678", out)
    }

    @Test
    fun formatAccountNumber_short_returnsAsIs() {
        assertEquals("12345", AccountFormatter.formatAccountNumber("12345"))
    }

    // ─── maskCardNumber() ───────────────────────────────────────────────

    @Test
    fun maskCardNumber_16digits_showsLast4() {
        assertEquals("•••• •••• •••• 5571", AccountFormatter.maskCardNumber("5798111122225571"))
    }

    @Test
    fun maskCardNumber_blank_returnsDash() {
        assertEquals("—", AccountFormatter.maskCardNumber(null))
        assertEquals("—", AccountFormatter.maskCardNumber("  "))
    }

    @Test
    fun maskCardNumber_lessThan4_returnsAsIs() {
        assertEquals("123", AccountFormatter.maskCardNumber("123"))
    }

    // ─── routingPrefix() ────────────────────────────────────────────────

    @Test
    fun routingPrefix_validAccount_returns3DigitPrefix() {
        assertEquals("265", AccountFormatter.routingPrefix("265-000000000123456-78"))
        assertEquals("444", AccountFormatter.routingPrefix("4441234567890"))
    }

    @Test
    fun routingPrefix_short_returnsNull() {
        assertNull(AccountFormatter.routingPrefix("12"))
    }

    @Test
    fun routingPrefix_blank_returnsNull() {
        assertNull(AccountFormatter.routingPrefix(""))
        assertNull(AccountFormatter.routingPrefix(null))
    }
}
