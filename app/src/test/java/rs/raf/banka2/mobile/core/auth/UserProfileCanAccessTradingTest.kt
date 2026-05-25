package rs.raf.banka2.mobile.core.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ME-04 tests: `canAccessTrading` enforces TRADE_STOCKS permisiju samo za CLIENT-a.
 * Employee/Admin/Supervisor uvek mogu da trguju bez obzira na canTradeStocks polje.
 *
 * Spec: Celina 4 §137-141 (T4A-017).
 */
class UserProfileCanAccessTradingTest {

    private fun makeProfile(role: UserRole, canTrade: Boolean): UserProfile =
        UserProfile(
            id = 1L,
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            role = role,
            permissions = emptySet(),
            canTradeStocks = canTrade
        )

    @Test
    fun client_withCanTradeTrue_canAccessTrading() {
        val p = makeProfile(UserRole.Client, canTrade = true)
        assertTrue(p.canAccessTrading)
    }

    @Test
    fun client_withCanTradeFalse_cannotAccessTrading() {
        val p = makeProfile(UserRole.Client, canTrade = false)
        assertFalse(p.canAccessTrading)
    }

    @Test
    fun admin_alwaysCanAccessTrading_evenIfCanTradeFalse() {
        // Admin / Supervisor / Agent — canTradeStocks polje se ignorise.
        val p = makeProfile(UserRole.Admin, canTrade = false)
        assertTrue(p.canAccessTrading)
    }

    @Test
    fun supervisor_alwaysCanAccessTrading() {
        val p = makeProfile(UserRole.Supervisor, canTrade = false)
        assertTrue(p.canAccessTrading)
    }

    @Test
    fun agent_alwaysCanAccessTrading() {
        // Agent moze da pristupi berzi (ali ne OTC-u, to je drugi gate).
        val p = makeProfile(UserRole.Agent, canTrade = false)
        assertTrue(p.canAccessTrading)
    }

    @Test
    fun defaultCanTradeStocks_isTrue() {
        // Backwards-compat: ako BE polje fali, default je true.
        val p = UserProfile(
            id = 1L,
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            role = UserRole.Client,
            permissions = emptySet()
            // canTradeStocks ne setujemo — default true
        )
        assertTrue(p.canTradeStocks)
        assertTrue(p.canAccessTrading)
    }
}
