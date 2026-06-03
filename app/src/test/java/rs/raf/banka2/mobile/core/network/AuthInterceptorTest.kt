package rs.raf.banka2.mobile.core.network

import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import rs.raf.banka2.mobile.core.storage.AuthStore

/**
 * SEC-06 / ME-AUTH-01 fix: AuthInterceptor mora da lepi Bearer na /auth/logout
 * (i sve druge zasticene rute) i NE sme da koristi startsWith("/auth/") sto
 * je propustalo logout sa BE blacklist-a.
 */
class AuthInterceptorTest {

    private val store = mockk<AuthStore>(relaxed = true)

    private fun buildChain(path: String): Interceptor.Chain {
        val request = Request.Builder()
            .url("https://example.com$path".toHttpUrl())
            .build()
        return mockk(relaxed = true) {
            io.mockk.every { request() } returns request
            io.mockk.every { proceed(any()) } answers {
                val proceededRequest = arg<Request>(0)
                Response.Builder()
                    .request(proceededRequest)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("ok".toResponseBody())
                    .build()
            }
        }
    }

    private fun proceededRequest(chain: Interceptor.Chain, interceptor: AuthInterceptor): Request {
        val response = interceptor.intercept(chain)
        return response.request
    }

    @Test
    fun bypass_loginPath_doesNotAttachBearer() {
        coEvery { store.accessToken() } returns "TOKEN"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/auth/login"), interceptor)
        assertNull(request.header("Authorization"))
    }

    @Test
    fun bypass_refreshPath_doesNotAttachBearer() {
        coEvery { store.accessToken() } returns "TOKEN"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/auth/refresh"), interceptor)
        assertNull(request.header("Authorization"))
    }

    @Test
    fun bypass_passwordResetPath_doesNotAttachBearer() {
        coEvery { store.accessToken() } returns "TOKEN"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/auth/password_reset/request"), interceptor)
        assertNull(request.header("Authorization"))
    }

    @Test
    fun bypass_activationTokenStatus_doesNotAttachBearer() {
        // Dinamicki token u path-u — mora ga match-ovati startsWith + endsWith helper.
        coEvery { store.accessToken() } returns "TOKEN"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(
            buildChain("/auth-employee/activation-token/abc-uuid-1234/status"),
            interceptor
        )
        assertNull(request.header("Authorization"))
    }

    @Test
    fun logoutPath_attachesBearer() {
        // KRITICNO: /auth/logout MORA da ide sa Bearer-om jer BE radi
        // server-side blacklist po tom tokenu.
        coEvery { store.accessToken() } returns "LOGOUT_TOKEN_42"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/auth/logout"), interceptor)
        assertEquals("Bearer LOGOUT_TOKEN_42", request.header("Authorization"))
    }

    @Test
    fun authMeOrChangePassword_attachesBearer() {
        // Druge /auth/* rute (npr. /auth/me, /auth/change-password) takodje
        // moraju ici sa Bearer-om — exact match bypass setu ih ne ukljucuje.
        coEvery { store.accessToken() } returns "SECRET"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/auth/me"), interceptor)
        assertEquals("Bearer SECRET", request.header("Authorization"))
    }

    @Test
    fun protectedPath_attachesBearer() {
        coEvery { store.accessToken() } returns "TOKEN_XYZ"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/orders/my"), interceptor)
        assertEquals("Bearer TOKEN_XYZ", request.header("Authorization"))
    }

    @Test
    fun protectedPath_noToken_doesNotAttachBearer() {
        coEvery { store.accessToken() } returns null
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/orders/my"), interceptor)
        assertNull(request.header("Authorization"))
    }

    @Test
    fun protectedPath_blankToken_doesNotAttachBearer() {
        coEvery { store.accessToken() } returns "   "
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/accounts/my"), interceptor)
        assertNull(request.header("Authorization"))
    }

    // P1-fe-mobile-authz-1 (265): RELEASE base URL je `.../api/` → encodedPath
    // je `/api/auth/login`. Exact-match na `/auth/login` je promasivao pa se na
    // login/refresh kacio stale Bearer. Suffix-match mora da pokrije i ovo.

    @Test
    fun bypass_apiPrefixedLoginPath_doesNotAttachBearer() {
        coEvery { store.accessToken() } returns "TOKEN"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/api/auth/login"), interceptor)
        assertNull(request.header("Authorization"))
    }

    @Test
    fun bypass_apiPrefixedRefreshPath_doesNotAttachBearer() {
        coEvery { store.accessToken() } returns "TOKEN"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/api/auth/refresh"), interceptor)
        assertNull(request.header("Authorization"))
    }

    @Test
    fun bypass_apiPrefixedActivate_doesNotAttachBearer() {
        coEvery { store.accessToken() } returns "TOKEN"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/api/auth-employee/activate"), interceptor)
        assertNull(request.header("Authorization"))
    }

    @Test
    fun apiPrefixedLogout_stillAttachesBearer() {
        // /api/auth/logout NIJE u bypass listi (nema endsWith match) → mora Bearer.
        coEvery { store.accessToken() } returns "LOGOUT_42"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(buildChain("/api/auth/logout"), interceptor)
        assertEquals("Bearer LOGOUT_42", request.header("Authorization"))
    }

    @Test
    fun bypass_apiPrefixedActivationTokenStatus_doesNotAttachBearer() {
        coEvery { store.accessToken() } returns "TOKEN"
        val interceptor = AuthInterceptor(store)
        val request = proceededRequest(
            buildChain("/api/auth-employee/activation-token/abc-uuid-1234/status"),
            interceptor
        )
        assertNull(request.header("Authorization"))
    }
}
