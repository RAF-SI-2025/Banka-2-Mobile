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
import rs.raf.banka2.mobile.data.api.EmployeeApi
import rs.raf.banka2.mobile.data.dto.auth.ActivateAccountRequest
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

        val (firstName, lastName, permissions, id) = if (isEmployeeRole) {
            when (val empResult = safeApiCall { employeeApi.searchByEmail(email) }) {
                is ApiResult.Success -> {
                    val match = empResult.data.content.firstOrNull { it.email.equals(email, ignoreCase = true) }
                        ?: empResult.data.content.firstOrNull()
                    EmployeeProfile(
                        firstName = match?.firstName.orEmpty(),
                        lastName = match?.lastName.orEmpty(),
                        permissions = match?.permissions.orEmpty().toSet(),
                        id = match?.id ?: 0L
                    )
                }
                is ApiResult.Failure -> return empResult
                is ApiResult.Loading -> EmployeeProfile()
            }
        } else {
            // CLIENT — nema /clients me endpoint-a u mobile flow-u za sada.
            // Ime/prezime ce se popuniti iz `/accounts/my` u Faza 2 dashboard-u.
            EmployeeProfile()
        }

        val role = RoleMapper.fromJwtRole(jwtRole, permissions)
        val profile = UserProfile(
            id = id,
            email = email,
            firstName = firstName,
            lastName = lastName,
            role = if (role == UserRole.Unknown && jwtRole.equals("CLIENT", ignoreCase = true)) {
                UserRole.Client
            } else role,
            permissions = permissions
        )
        sessionManager.update(SessionState.LoggedIn(profile))
        return ApiResult.Success(profile)
    }

    private data class EmployeeProfile(
        val firstName: String = "",
        val lastName: String = "",
        val permissions: Set<String> = emptySet(),
        val id: Long = 0L
    )
}
