package rs.raf.banka2.mobile.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rs.raf.banka2.mobile.core.storage.AuthStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth za "ko je trenutno prijavljen i sta sme."
 * UI sluha [state] kao StateFlow i renderuje navigaciju po roli.
 *
 * AuthStore je odgovoran samo za persistenciju tokena/email-a;
 * SessionManager dodatno cuva profile (id, ime, prezime, role, permissions)
 * koji dolazi iz backend `/employees` ili `/clients` endpoint-a.
 */
@Singleton
class SessionManager @Inject constructor(
    private val authStore: AuthStore
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun update(state: SessionState) {
        _state.value = state
    }

    suspend fun logout() {
        authStore.clear()
        _state.value = SessionState.LoggedOut
    }
}

sealed interface SessionState {
    /** Splash render + decision pending. */
    data object Loading : SessionState

    /** Nema tokena ili je sesija istekla. */
    data object LoggedOut : SessionState

    /** Aktivan user. UI bira start destination na osnovu role. */
    data class LoggedIn(val profile: UserProfile) : SessionState
}

/**
 * Profil koji se prikazuje u sidebar-u i koji koriste guards.
 */
data class UserProfile(
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val permissions: Set<String>
) {
    val fullName: String get() = "$firstName $lastName".trim().ifBlank { email }

    fun has(permission: String): Boolean =
        permissions.any { it.equals(permission, ignoreCase = true) }
}
