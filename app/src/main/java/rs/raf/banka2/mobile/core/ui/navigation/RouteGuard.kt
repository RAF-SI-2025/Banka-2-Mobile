package rs.raf.banka2.mobile.core.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserRole
import javax.inject.Inject

/**
 * P1-fe-mobile-authz-1 (1755): klijentski-bocni route guard (defense-in-depth).
 *
 * AppNavHost je RANIJE registrovao SVE rute globalno bez ikakve provere role —
 * deep-link / notifikacioni-target / restored navigacija je mogla da montira
 * employee/supervisor ekran klijentu (BE 403, ali ekran se montira sa
 * praznim/error stanjem; lose UX + curi struktura). Ovaj guard montira sadrzaj
 * SAMO ako trenutna sesija ima dozvoljenu rolu. FAIL-CLOSED: dok je sesija
 * `Loading` prikazuje spinner; ako je `LoggedOut` ili rola nije dozvoljena
 * prikazuje "nemate pristup" placeholder umesto ekrana.
 *
 * BE ostaje autoritativan — ovo je samo UI/UX sloj.
 */
enum class RouteAccess {
    /** Bilo koji zaposleni (ADMIN/SUPERVISOR/AGENT). */
    EMPLOYEE,

    /** Samo SUPERVISOR ili ADMIN (Orderi/Aktuari/Porez/ProfitBank/Audit/OTC istorija). */
    SUPERVISOR,

    /** Samo ADMIN (npr. lista zaposlenih, kreiranje zaposlenog). */
    ADMIN,

    /** OTC/trgovinski klijentski tok — agenti iskljuceni (§137-141). */
    NO_AGENT,

    /** Plasiranje naloga — admin/supervizor (u ime fonda) ILI klijent koji moze da trguje. */
    TRADE
}

private fun UserRole.satisfies(access: RouteAccess): Boolean = when (access) {
    RouteAccess.EMPLOYEE -> isEmployee
    RouteAccess.SUPERVISOR -> isSupervisor
    RouteAccess.ADMIN -> isAdmin
    RouteAccess.NO_AGENT -> this != UserRole.Agent && this != UserRole.Unknown
    // canAccessTrading se dodatno proverava na nivou profila (canTradeStocks).
    RouteAccess.TRADE -> isSupervisor || isClient
}

/**
 * Pure, testabilna odluka da li rola (+ canAccessTrading za TRADE) sme da
 * pristupi datoj ruti. FAIL-CLOSED je u pozivaocu (RouteGuard ne montira sadrzaj
 * dok sesija nije LoggedIn).
 */
internal fun isRouteAllowed(
    role: UserRole,
    access: RouteAccess,
    canAccessTrading: Boolean
): Boolean = role.satisfies(access) &&
    (access != RouteAccess.TRADE || canAccessTrading)

@HiltViewModel
class RouteGuardViewModel @Inject constructor(
    sessionManager: SessionManager
) : ViewModel() {
    val state: StateFlow<SessionState> = sessionManager.state
}

@Composable
fun RouteGuard(
    access: RouteAccess,
    viewModel: RouteGuardViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val session by viewModel.state.collectAsStateWithLifecycle()

    when (val s = session) {
        is SessionState.Loading -> GuardLoading()
        is SessionState.LoggedOut -> GuardDenied()
        is SessionState.LoggedIn -> {
            // Za TRADE dodatno proveravamo canAccessTrading (klijent bez
            // TRADE_STOCKS ne sme da plasira nalog). FAIL-CLOSED.
            val allowed = isRouteAllowed(s.profile.role, access, s.profile.canAccessTrading)
            if (allowed) content() else GuardDenied()
        }
    }
}

@Composable
private fun GuardLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun GuardDenied() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nemate pristup ovom delu aplikacije.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}
