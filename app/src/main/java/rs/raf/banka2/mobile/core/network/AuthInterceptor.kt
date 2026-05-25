package rs.raf.banka2.mobile.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import rs.raf.banka2.mobile.core.storage.AuthStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lepi `Authorization: Bearer <accessToken>` na svaki zahtev osim auth ruta.
 *
 * Refresh ovde NE pokusavamo — to je posao [TokenAuthenticator].
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authStore: AuthStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // SEC-06 / ME-AUTH-01: exact-match bypass listu — NIKAD ne sme da koristimo
        // startsWith("/auth/") jer to puca server-side JWT blacklist na /auth/logout
        // (logout mora ici sa Bearer header-om da BE moze da ga blacklist-uje).
        // Slicno, /auth-employee/activation-token/{token}/status je javni pre-check
        // a /auth-employee/activate prima token kroz body, ne kroz Authorization.
        if (path in BYPASS_PATHS || isActivationTokenStatusPath(path)) {
            return chain.proceed(original)
        }

        val token = runBlocking { authStore.accessToken() }
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }

    /**
     * Match-uje `/auth-employee/activation-token/{token}/status` (Sc 9 pre-check
     * iz 12.05.2026 — token status check pre aktivacije). Token je dinamicki
     * UUID pa ne moze biti u BYPASS_PATHS setu.
     */
    private fun isActivationTokenStatusPath(path: String): Boolean {
        return path.startsWith("/auth-employee/activation-token/") && path.endsWith("/status")
    }

    private companion object {
        // Eksplicitan, exact-match bypass — sve sto NIJE ovde dobija Bearer.
        // /auth/logout, /auth/me, /auth/change-password itd. MORAJU imati Bearer.
        val BYPASS_PATHS = setOf(
            "/auth/login",
            "/auth/refresh",
            "/auth/register",
            "/auth/password_reset/request",
            "/auth/password_reset/confirm",
            "/auth-employee/activate"
            // /auth/logout NAMERNO NIJE ovde — server-side blacklist mora da vidi token
        )
    }
}
