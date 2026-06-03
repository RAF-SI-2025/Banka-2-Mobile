package rs.raf.banka2.mobile.core.network

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.storage.AuthStore

/**
 * P0-M1 N3 — TokenAuthenticator idempotency (paritet sa FE F1).
 *
 * OkHttp Authenticator automatski RE-SALJE zahtev posle refresh-a. Za mutacione
 * (POST/PUT/PATCH/DELETE) to moze duplo izvrsiti operaciju. Fix: ne re-saljemo
 * non-GET zahtev — refresh-ujemo token (za sledeci rucni pokusaj) ali vracamo
 * null. GET se i dalje re-salje normalno.
 *
 * Pokrivamo:
 *  - GET 401 + uspesan refresh → vraca retried request sa novim Bearer-om
 *  - POST 401 + uspesan refresh → token sacuvan, ALI vraca null (bez retry-a)
 *  - parallel-refreshed branch: GET → retry sa saved token; POST → null
 *  - refresh fail → clear session + null (oba metoda)
 *  - PUT/PATCH/DELETE → null (bez retry-a)
 */
class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer
    private val store = mockk<AuthStore>(relaxed = true)

    /**
     * Refresh klijent koji prepisuje host/port fiksnog BuildConfig.API_BASE_URL
     * refresh URL-a na MockWebServer, tako da mozemo da testiramo pravi refresh path.
     */
    private lateinit var refreshClient: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val redirectToMockServer = Interceptor { chain ->
            val original = chain.request()
            val rewritten = original.url.newBuilder()
                .scheme("http")
                .host(server.hostName)
                .port(server.port)
                .build()
            chain.proceed(original.newBuilder().url(rewritten).build())
        }
        refreshClient = OkHttpClient.Builder()
            .addInterceptor(redirectToMockServer)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun authenticator() = TokenAuthenticator(store, refreshClient)

    /** Sintetisan 401 response za zahtev sa datim metodom + path-om. */
    private fun response401(method: String, path: String, sentBearer: String): Response {
        val builder = Request.Builder()
            .url("http://api.example.com/$path")
            .header("Authorization", "Bearer $sentBearer")
        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post("{}".toRequestBody())
            "PUT" -> builder.put("{}".toRequestBody())
            "PATCH" -> builder.patch("{}".toRequestBody())
            "DELETE" -> builder.delete("{}".toRequestBody())
        }
        val request = builder.build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .build()
    }

    private fun enqueueRefreshSuccess(newAccess: String, newRefresh: String) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"accessToken":"$newAccess","refreshToken":"$newRefresh"}"""
            )
        )
    }

    // ─── Refresh path ─────────────────────────────────────────────────────

    @Test
    fun getRequest_401_refreshes_andReturnsRetriedRequest() {
        coEvery { store.accessToken() } returns "OLD"
        coEvery { store.refreshToken() } returns "REFRESH"
        enqueueRefreshSuccess("NEW_ACCESS", "NEW_REFRESH")

        val retried = authenticator().authenticate(null, response401("GET", "orders/my", "OLD"))

        assertNotNull("GET mora da se re-salje posle refresh-a", retried)
        assertEquals("Bearer NEW_ACCESS", retried!!.header("Authorization"))
        coVerify { store.saveTokens("NEW_ACCESS", "NEW_REFRESH") }
    }

    @Test
    fun postRequest_401_refreshesToken_butDoesNotRetry() {
        coEvery { store.accessToken() } returns "OLD"
        coEvery { store.refreshToken() } returns "REFRESH"
        enqueueRefreshSuccess("NEW_ACCESS", "NEW_REFRESH")

        val retried = authenticator().authenticate(null, response401("POST", "payments", "OLD"))

        assertNull("POST se NE sme automatski re-slati", retried)
        // token je ipak osvezen za sledeci rucni pokusaj
        coVerify { store.saveTokens("NEW_ACCESS", "NEW_REFRESH") }
    }

    @Test
    fun putPatchDelete_401_doNotRetry() {
        for (method in listOf("PUT", "PATCH", "DELETE")) {
            coEvery { store.accessToken() } returns "OLD"
            coEvery { store.refreshToken() } returns "REFRESH"
            enqueueRefreshSuccess("NEW_ACCESS_$method", "NEW_REFRESH_$method")

            val retried = authenticator().authenticate(null, response401(method, "resource/1", "OLD"))

            assertNull("$method se NE sme automatski re-slati", retried)
        }
    }

    @Test
    fun refreshFailure_clearsSession_andReturnsNull_forGet() {
        coEvery { store.accessToken() } returns "OLD"
        coEvery { store.refreshToken() } returns "REFRESH"
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"invalid refresh"}"""))

        val retried = authenticator().authenticate(null, response401("GET", "orders/my", "OLD"))

        assertNull(retried)
        coVerify { store.clear() }
    }

    // ─── Parallel-refreshed branch (drugi zahtev je vec refreshovao) ──────

    @Test
    fun getRequest_alreadyRefreshedByParallel_retriesWithSavedToken_noNetwork() {
        // saved != sent → parallel je vec refreshovao; ne sme da pozove /auth/refresh
        coEvery { store.accessToken() } returns "FRESH_FROM_PARALLEL"

        val retried = authenticator().authenticate(null, response401("GET", "orders/my", "OLD"))

        assertNotNull(retried)
        assertEquals("Bearer FRESH_FROM_PARALLEL", retried!!.header("Authorization"))
        assertEquals("nije smelo da hitne refresh", 0, server.requestCount)
    }

    @Test
    fun postRequest_alreadyRefreshedByParallel_returnsNull_noRetry() {
        coEvery { store.accessToken() } returns "FRESH_FROM_PARALLEL"

        val retried = authenticator().authenticate(null, response401("POST", "payments", "OLD"))

        assertNull("mutacija se ne re-salje cak ni kad je token vec svez", retried)
        assertEquals(0, server.requestCount)
    }

    // ─── Guard: refresh sam vraca 401 ne sme da ulancava ──────────────────

    @Test
    fun refreshEndpoint401_returnsNull() {
        val retried = authenticator().authenticate(
            null,
            response401("POST", "auth/refresh", "OLD")
        )
        assertNull(retried)
        assertEquals(0, server.requestCount)
    }
}
