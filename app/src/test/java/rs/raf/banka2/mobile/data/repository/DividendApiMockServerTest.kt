package rs.raf.banka2.mobile.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import rs.raf.banka2.mobile.data.api.DividendApi

/**
 * TEST-mobile-trading-vm-1 (R4-1104): MockWebServer wire-test za [DividendApi]
 * preko [DividendRepository]. Pokriva grane koje mockk-DividendRepositoryTest NE
 * pokriva:
 *  - STVARNE putanje `GET /dividends/my` i `GET /dividends/by-position/{id}`
 *  - 403 (agent nema dividend pristup) → ApiError.Kind.Forbidden
 *  - prazno 2xx telo na tipiziranom List endpoint-u → Failure (kontrakt-greska,
 *    NE crash; R1-584/R1 mob4 "Server je vratio prazan odgovor")
 *  - `[]` (legitimno prazan JSON array) → Success(emptyList)
 *  - BigDecimal novcana polja se parsiraju bez Double round-off
 */
class DividendApiMockServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: DividendRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val moshi = Moshi.Builder()
            .add(BigDecimalAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DividendApi::class.java)
        repo = DividendRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getMy_getsDividendsMyPath_andParsesBigDecimal() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":1,"stockTicker":"AAPL","grossAmount":100.55,"tax":15.08,"netAmount":85.47,"currencyCode":"USD","taxExempt":false}]"""
            )
        )

        val result = repo.getMy()

        assertEquals("/dividends/my", server.takeRequest().path)
        assertTrue(result is ApiResult.Success)
        val row = (result as ApiResult.Success).data[0]
        assertEquals("AAPL", row.stockTicker)
        assertEquals(java.math.BigDecimal("85.47"), row.netAmount)
        assertEquals(java.math.BigDecimal("15.08"), row.tax)
    }

    @Test
    fun getByPosition_getsByPositionPath() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""[]"""))

        val result = repo.getByPosition(42L)

        assertEquals("/dividends/by-position/42", server.takeRequest().path)
        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    @Test
    fun getMy_403_agentForbidden() = runTest {
        // Agent nema pristup tudjim/svim dividendama → BE 403.
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Nemate dozvolu."}"""))

        val result = repo.getMy()

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Forbidden, error.kind)
        assertEquals("Nemate dozvolu.", error.message)
    }

    @Test
    fun getByPosition_403_agentForbidden() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Nemate dozvolu."}"""))

        val result = repo.getByPosition(7L)

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Forbidden, (result as ApiResult.Failure).error.kind)
    }

    @Test
    fun getMy_emptyBodyOnTypedList_isFailure_notCrash() = runTest {
        // R1-584 / R1 mob4: 200 sa PRAZNIM telom (ne "[]") na List endpoint-u je
        // krsenje kontrakta — mora vratiti Failure, NE crash. Karakterizacija:
        // Moshi pokusa da deserijalizuje prazno telo kao List i baca EOF/parse
        // izuzetak koji safeApiCall hvata u IOException grani → Kind.Network
        // (httpCode == null = "nismo dobili upotrebljiv odgovor"). Kljucno je da
        // NE crash-uje aplikaciju (graceful Failure), ne tacan podtip greske.
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))

        val result = repo.getMy()

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Network, error.kind)
        assertNull("httpCode mora biti null (parse/IO fail, ne HTTP status)", error.httpCode)
    }
}
