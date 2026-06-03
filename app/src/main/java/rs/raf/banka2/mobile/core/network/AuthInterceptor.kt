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

        // SEC-06 / ME-AUTH-01: bypass listu match-ujemo po SUFFIX-u, ne po exact
        // path-u. NIKAD ne koristimo startsWith("/auth/") jer to puca server-side
        // JWT blacklist na /auth/logout (logout mora ici sa Bearer header-om da BE
        // moze da ga blacklist-uje). Slicno, /auth-employee/activation-token/{token}/
        // status je javni pre-check a /auth-employee/activate prima token kroz body.
        //
        // P1-fe-mobile-authz-1 (265): RELEASE base URL je `.../api/` pa encodedPath
        // postaje `/api/auth/login` — exact-match na `/auth/login` PROMASUJE → na
        // login/refresh se kaci (stale) Bearer header. Suffix-match
        // (`endsWith`) pokriva i `/api/`-prefiksovan i debug `/`-prefiksovan path.
        if (isBypassPath(path) || isActivationTokenStatusPath(path)) {
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
     * P1-fe-mobile-authz-1 (265): suffix-match da bi i `/api/`-prefiksovan
     * (release) i `/`-prefiksovan (debug) path bili pokriveni. Eksaktan path
     * (`/auth/login`) ili `.../auth/login` oba prolaze; bilo koji drugi auth
     * endpoint (npr. `/auth/logout`, `/auth/me`) i dalje dobija Bearer.
     */
    private fun isBypassPath(path: String): Boolean {
        return BYPASS_PATHS.any { suffix -> path == suffix || path.endsWith(suffix) }
    }

    /**
     * Match-uje `/auth-employee/activation-token/{token}/status` (Sc 9 pre-check
     * iz 12.05.2026 — token status check pre aktivacije). Token je dinamicki
     * UUID pa ne moze biti u BYPASS_PATHS setu. Koristi `contains` umesto
     * `startsWith` radi `/api/`-prefiksa u release build-u.
     */
    private fun isActivationTokenStatusPath(path: String): Boolean {
        return path.contains("/auth-employee/activation-token/") && path.endsWith("/status")
    }

    private companion object {
        // Eksplicitan, exact-match bypass — sve sto NIJE ovde dobija Bearer.
        // /auth/logout, /auth/me, /auth/change-password itd. MORAJU imati Bearer.
        val BYPASS_PATHS = setOf(
            "/auth/login",
            "/auth/refresh",
            "/auth/password_reset/request",
            "/auth/password_reset/confirm",
            "/auth-employee/activate"
            // /auth/logout NAMERNO NIJE ovde — server-side blacklist mora da vidi token
        )
    }
}
