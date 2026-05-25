package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.auth.JwtDecoder
import rs.raf.banka2.mobile.core.auth.RoleMapper
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.core.storage.AuthStore
import rs.raf.banka2.mobile.data.api.AuthApi
import rs.raf.banka2.mobile.data.api.ClientApi
import rs.raf.banka2.mobile.data.api.EmployeeApi
import rs.raf.banka2.mobile.data.dto.auth.ActivateAccountRequest
import rs.raf.banka2.mobile.data.dto.auth.ActivationTokenStatusResponse
import rs.raf.banka2.mobile.data.dto.auth.LoginRequest
import rs.raf.banka2.mobile.data.dto.auth.LoginResponse
import rs.raf.banka2.mobile.data.dto.auth.PasswordResetConfirmRequest
import rs.raf.banka2.mobile.data.dto.auth.PasswordResetRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository fasada za auth flow.
 *
 * Login redosled:
 *  1. POST /auth/login → tokeni
 *  2. JWT decode → email + role (ADMIN/EMPLOYEE/CLIENT)
 *  3. Ako role nije CLIENT → GET /employees?email=... za dobijanje permisija
 *     i imena/prezimena (CLIENT-i ne idu kroz employees endpoint)
 *  4. Pakuje [UserProfile] i upisuje u [SessionManager]
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val employeeApi: EmployeeApi,
    private val clientApi: ClientApi,
    private val authStore: AuthStore,
    private val sessionManager: SessionManager
) {

    suspend fun login(email: String, password: String): ApiResult<UserProfile> {
        return when (val result = safeApiCall { authApi.login(LoginRequest(email, password)) }) {
            is ApiResult.Failure -> result
            is ApiResult.Loading -> result
            is ApiResult.Success -> {
                persistTokens(result.data, email)
                buildAndStoreProfile(result.data.accessToken, email)
            }
        }
    }

    suspend fun requestPasswordReset(email: String): ApiResult<Unit> =
        safeApiCall { authApi.requestPasswordReset(PasswordResetRequest(email)) }
            .let { result ->
                when (result) {
                    is ApiResult.Success -> ApiResult.Success(Unit)
                    is ApiResult.Failure -> result
                    is ApiResult.Loading -> result
                }
            }

    suspend fun confirmPasswordReset(token: String, newPassword: String): ApiResult<Unit> =
        safeApiCall { authApi.confirmPasswordReset(PasswordResetConfirmRequest(token, newPassword)) }
            .let { result ->
                when (result) {
                    is ApiResult.Success -> ApiResult.Success(Unit)
                    is ApiResult.Failure -> result
                    is ApiResult.Loading -> result
                }
            }

    suspend fun activateAccount(token: String, password: String): ApiResult<Unit> =
        safeApiCall { authApi.activateEmployee(ActivateAccountRequest(token, password)) }
            .let { result ->
                when (result) {
                    is ApiResult.Success -> ApiResult.Success(Unit)
                    is ApiResult.Failure -> result
                    is ApiResult.Loading -> result
                }
            }

    /**
     * Spec Sc 9 + ad-hoc bug 12.05.2026: pre nego sto Mobile renderuje formu
     * za aktivaciju, proverava stanje tokena. Bez ovog koraka, korisnik koji
     * je vec uspesno aktivirao nalog moze osveziti stranicu (npr. back gesture
     * + open notification link) i ponovo videti formu — submit bi pukao sa
     * BE 400 "Activation token already used or invalidated." Sad UI prikazuje
     * odgovarajuci ekran (USED/EXPIRED/INVALID/ALREADY_ACTIVE) umesto forme.
     */
    suspend fun activationTokenStatus(token: String): ApiResult<ActivationTokenStatusResponse> =
        safeApiCall { authApi.activationTokenStatus(token) }

    suspend fun logout() {
        // Opciono.1: server-side blacklist tokena pre brisanja lokalno.
        // Greske ignorisemo (npr. token vec expired, ili offline) — klijent
        // svakako mora da obrise lokalne tokene jer je korisnik kliknuo logout.
        runCatching { authApi.logout() }
        sessionManager.logout()
    }

    /**
     * Pokusava da rekonstruise sesiju iz lokalno cuvanog tokena.
     * Koristi se na splash-u — ako token postoji i nije expired, vrati profil.
     */
    suspend fun restoreSessionIfPossible(): UserProfile? {
        val access = authStore.accessToken() ?: return null
        if (JwtDecoder.isExpired(access)) {
            // TokenAuthenticator ce pokusati refresh prvog API poziva,
            // ali za splash ne idemo na refresh — saljemo korisnika na login.
            return null
        }
        val email = JwtDecoder.decode(access)?.sub ?: authStore.savedEmail() ?: return null
        return when (val result = buildAndStoreProfile(access, email)) {
            is ApiResult.Success -> result.data
            else -> null
        }
    }

    private suspend fun persistTokens(login: LoginResponse, email: String) {
        authStore.saveTokens(login.accessToken, login.refreshToken)
        authStore.saveEmail(email)
    }

    private suspend fun buildAndStoreProfile(accessToken: String, email: String): ApiResult<UserProfile> {
        val payload = JwtDecoder.decode(accessToken)
        val jwtRole = payload?.role
        val isEmployeeRole = jwtRole.equals("ADMIN", ignoreCase = true)
            || jwtRole.equals("EMPLOYEE", ignoreCase = true)

        // ME-04 fix: za CLIENT-a fetch-uj `/clients?email=...` da bismo procitali
        // `canTradeStocks` polje. Best-effort — ako BE vrati 403/404, default je true
        // (backwards-compat sa legacy seed-om).
        val profileData = if (isEmployeeRole) {
            when (val empResult = safeApiCall { employeeApi.searchByEmail(email) }) {
                is ApiResult.Success -> {
                    val match = empResult.data.content.firstOrNull { it.email.equals(email, ignoreCase = true) }
                        ?: empResult.data.content.firstOrNull()
                    ResolvedProfile(
                        firstName = match?.firstName.orEmpty(),
                        lastName = match?.lastName.orEmpty(),
                        permissions = match?.permissions.orEmpty().toSet(),
                        id = match?.id ?: 0L,
                        canTradeStocks = true // zaposleni uvek mogu da trguju
                    )
                }
                is ApiResult.Failure -> return empResult
                is ApiResult.Loading -> ResolvedProfile()
            }
        } else {
            // CLIENT — fetchuj kompletan client zapis radi canTradeStocks i naseg ime/prezime.
            when (val clientResult = safeApiCall { clientApi.list(email = email, limit = 5) }) {
                is ApiResult.Success -> {
                    val match = clientResult.data.content.firstOrNull { it.email.equals(email, ignoreCase = true) }
                        ?: clientResult.data.content.firstOrNull()
                    ResolvedProfile(
                        firstName = match?.firstName.orEmpty(),
                        lastName = match?.lastName.orEmpty(),
                        permissions = emptySet(),
                        id = match?.id ?: 0L,
                        // BE polje moze biti null kod legacy seed-a — default true
                        canTradeStocks = match?.canTradeStocks ?: true
                    )
                }
                // Best-effort: ako BE nema /clients lookup (403/404), nastavi sa defaults.
                is ApiResult.Failure -> ResolvedProfile(canTradeStocks = true)
                is ApiResult.Loading -> ResolvedProfile()
            }
        }

        val role = RoleMapper.fromJwtRole(jwtRole, profileData.permissions)
        val profile = UserProfile(
            id = profileData.id,
            email = email,
            firstName = profileData.firstName,
            lastName = profileData.lastName,
            role = if (role == UserRole.Unknown && jwtRole.equals("CLIENT", ignoreCase = true)) {
                UserRole.Client
            } else role,
            permissions = profileData.permissions,
            canTradeStocks = profileData.canTradeStocks
        )
        sessionManager.update(SessionState.LoggedIn(profile))
        return ApiResult.Success(profile)
    }

    /** ME-04: rename iz EmployeeProfile zbog client putanje + canTradeStocks polje. */
    private data class ResolvedProfile(
        val firstName: String = "",
        val lastName: String = "",
        val permissions: Set<String> = emptySet(),
        val id: Long = 0L,
        val canTradeStocks: Boolean = true
    )
}
