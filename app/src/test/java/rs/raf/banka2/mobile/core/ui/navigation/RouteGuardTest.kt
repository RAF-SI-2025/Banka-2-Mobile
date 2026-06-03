package rs.raf.banka2.mobile.core.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.core.auth.UserRole

/**
 * P1-fe-mobile-authz-1 (1755): route-level role guard odluka (defense-in-depth).
 * Verifikuje matricu rola × RouteAccess + TRADE canAccessTrading.
 */
class RouteGuardTest {

    @Test
    fun employeeAccess_allowsAllEmployees_deniesClient() {
        assertTrue(isRouteAllowed(UserRole.Admin, RouteAccess.EMPLOYEE, true))
        assertTrue(isRouteAllowed(UserRole.Supervisor, RouteAccess.EMPLOYEE, true))
        assertTrue(isRouteAllowed(UserRole.Agent, RouteAccess.EMPLOYEE, true))
        assertFalse(isRouteAllowed(UserRole.Client, RouteAccess.EMPLOYEE, true))
        assertFalse(isRouteAllowed(UserRole.Unknown, RouteAccess.EMPLOYEE, true))
    }

    @Test
    fun supervisorAccess_allowsAdminAndSupervisor_deniesAgentAndClient() {
        assertTrue(isRouteAllowed(UserRole.Admin, RouteAccess.SUPERVISOR, true))
        assertTrue(isRouteAllowed(UserRole.Supervisor, RouteAccess.SUPERVISOR, true))
        assertFalse(isRouteAllowed(UserRole.Agent, RouteAccess.SUPERVISOR, true))
        assertFalse(isRouteAllowed(UserRole.Client, RouteAccess.SUPERVISOR, true))
    }

    @Test
    fun adminAccess_onlyAdmin() {
        assertTrue(isRouteAllowed(UserRole.Admin, RouteAccess.ADMIN, true))
        assertFalse(isRouteAllowed(UserRole.Supervisor, RouteAccess.ADMIN, true))
        assertFalse(isRouteAllowed(UserRole.Agent, RouteAccess.ADMIN, true))
        assertFalse(isRouteAllowed(UserRole.Client, RouteAccess.ADMIN, true))
    }

    @Test
    fun noAgentAccess_allowsClientSupervisorAdmin_deniesAgentAndUnknown() {
        assertTrue(isRouteAllowed(UserRole.Admin, RouteAccess.NO_AGENT, true))
        assertTrue(isRouteAllowed(UserRole.Supervisor, RouteAccess.NO_AGENT, true))
        assertTrue(isRouteAllowed(UserRole.Client, RouteAccess.NO_AGENT, true))
        assertFalse(isRouteAllowed(UserRole.Agent, RouteAccess.NO_AGENT, true))
        assertFalse(isRouteAllowed(UserRole.Unknown, RouteAccess.NO_AGENT, true))
    }

    @Test
    fun tradeAccess_clientNeedsCanTrade() {
        // Klijent sa TRADE_STOCKS (canAccessTrading=true) sme.
        assertTrue(isRouteAllowed(UserRole.Client, RouteAccess.TRADE, true))
        // Klijent bez TRADE_STOCKS (canAccessTrading=false) NE sme.
        assertFalse(isRouteAllowed(UserRole.Client, RouteAccess.TRADE, false))
    }

    @Test
    fun tradeAccess_supervisorAndAdminAllowed_agentDenied() {
        assertTrue(isRouteAllowed(UserRole.Supervisor, RouteAccess.TRADE, true))
        assertTrue(isRouteAllowed(UserRole.Admin, RouteAccess.TRADE, true))
        assertFalse(isRouteAllowed(UserRole.Agent, RouteAccess.TRADE, true))
    }
}
