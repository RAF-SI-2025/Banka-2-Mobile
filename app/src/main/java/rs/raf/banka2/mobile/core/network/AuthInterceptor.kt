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

        if (path in BYPASS_PATHS || path.startsWith("/auth/")) {
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

    private companion object {
        val BYPASS_PATHS = setOf(
            "/auth/login",
            "/auth/refresh",
            "/auth/password_reset/request",
            "/auth/password_reset/confirm",
            "/auth-employee/activate"
        )
    }
}
