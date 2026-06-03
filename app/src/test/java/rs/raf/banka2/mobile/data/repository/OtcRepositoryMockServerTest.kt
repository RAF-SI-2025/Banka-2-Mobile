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
import rs.raf.banka2.mobile.data.api.OtcApi
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto

/**
 * MockWebServer test za OtcRepository — repo-level wrapping iznad OtcApi
 * (OtcApiSagaTest vec pokriva direktan OtcApi sloj). Fokus na:
 *  - exerciseIntra(contract, buyerAccountId) -> POST /otc/contracts/{id}/exercise
 *    sa buyerAccountId kao QUERY parametrom + parsiranje OtcExerciseResultDto
 *  - sagaStatusIntra -> GET /otc/saga/{sagaId} + parsiranje SagaStatusDto log-a
 *  - pollInterContractStatus -> ekstrakcija status-a iz liste po foreignId
 *  - negotiationHistory -> "ALL"/blank filteri se izostavljaju iz query-ja
 *  - 409 mapiranje (double-exercise)
 */
class OtcRepositoryMockServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: OtcRepository

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
            .create(OtcApi::class.java)
        repo = OtcRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun contract(id: Long, foreignId: String? = null) = OtcContractDto(
        id = id,
        listingId = 1L,
        listingTicker = "AAPL",
        quantity = 10,
        strikePrice = 100.0,
        premium = 5.0,
        status = "ACTIVE",
        foreignId = foreignId
    )

    @Test
    fun exerciseIntra_postsExercisePath_withBuyerAccountIdQuery_andParsesResult() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"sagaId":"saga-1","sagaStatus":"COMPLETED","currentStep":5,"id":42,"status":"EXERCISED"}"""
            )
        )

        val result = repo.exerciseIntra(contract(42L), buyerAccountId = 777L)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/otc/contracts/42/exercise?buyerAccountId=777", request.path)
        assertEquals("", request.body.readUtf8())   // buyerAccountId je query, ne body

        assertTrue(result is ApiResult.Success)
        val dto = (result as ApiResult.Success).data
        assertEquals("saga-1", dto.sagaId)
        assertEquals("COMPLETED", dto.sagaStatus)
        assertEquals(5, dto.currentStep)
        assertEquals(42L, dto.id)
        assertEquals("EXERCISED", dto.status)
    }

    @Test
    fun exerciseIntra_withoutBuyerAccount_omitsQueryParam() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"sagaId":"s2","sagaStatus":"COMPENSATED","currentStep":3,"id":9,"status":"ACTIVE"}"""
            )
        )

        repo.exerciseIntra(contract(9L), buyerAccountId = null)

        assertEquals("/otc/contracts/9/exercise", server.takeRequest().path)
    }

    @Test
    fun exerciseIntra_409_mapsToConflict_doubleExercise() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"message":"Ugovor je vec iskoriscen"}"""))

        val result = repo.exerciseIntra(contract(42L), buyerAccountId = 1L)

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Conflict, error.kind)
        assertEquals("Ugovor je vec iskoriscen", error.message)
    }

    @Test
    fun sagaStatusIntra_getsSagaPath_andParsesLog() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "sagaId":"saga-1",
                  "status":"COMPLETED",
                  "currentStep":5,
                  "log":[
                    {"phase":1,"kind":"FORWARD","outcome":"ok","message":null,"at":"2026-05-30T10:00:00"},
                    {"phase":5,"kind":"FORWARD","outcome":"ok","message":null,"at":"2026-05-30T10:00:05"}
                  ]
                }
                """.trimIndent()
            )
        )

        val result = repo.sagaStatusIntra("saga-1")

        assertEquals("/otc/saga/saga-1", server.takeRequest().path)
        assertTrue(result is ApiResult.Success)
        val saga = (result as ApiResult.Success).data
        assertEquals("COMPLETED", saga.status)
        assertEquals(2, saga.log.size)
        assertEquals(1, saga.log[0].phase)
        assertEquals("FORWARD", saga.log[0].kind)
        assertNull(saga.log[0].message)
    }

    @Test
    fun pollInterContractStatus_extractsStatusForMatchingForeignId() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[
                  {"id":"111:aaa","listingTicker":"AAPL","quantity":10.0,"strikePrice":100.0,"premium":5.0,"status":"ACTIVE"},
                  {"id":"111:bbb","listingTicker":"MSFT","quantity":2.0,"strikePrice":200.0,"premium":3.0,"status":"EXERCISED"}
                ]"""
            )
        )

        val result = repo.pollInterContractStatus("111:bbb")

        assertEquals("/interbank/otc/contracts/my", server.takeRequest().path)
        assertTrue(result is ApiResult.Success)
        assertEquals("EXERCISED", (result as ApiResult.Success).data)
    }

    @Test
    fun pollInterContractStatus_unknownForeignId_returnsUnknown() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""[]"""))

        val result = repo.pollInterContractStatus("nope")
        assertTrue(result is ApiResult.Success)
        assertEquals("UNKNOWN", (result as ApiResult.Success).data)
    }

    @Test
    fun negotiationHistory_dropsAllAndBlankFilters() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"content":[],"totalElements":0,"totalPages":0,"number":0,"size":20,"first":true,"last":true,"empty":true}"""
            )
        )

        repo.negotiationHistory(status = "ALL", from = "", to = "  ", page = 1, size = 10)

        val url = server.takeRequest().requestUrl!!
        assertEquals(listOf("otc", "negotiation-history"), url.encodedPathSegments)
        assertNull(url.queryParameter("status"))
        assertNull(url.queryParameter("from"))
        assertNull(url.queryParameter("to"))
        assertEquals("1", url.queryParameter("page"))
        assertEquals("10", url.queryParameter("size"))
    }

    @Test
    fun negotiationHistory_passesRealFilters() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"content":[],"totalElements":0,"totalPages":0,"number":0,"size":20,"first":true,"last":true,"empty":true}"""
            )
        )

        repo.negotiationHistory(status = "ACTIVE", modifiedById = 5L, from = "2026-05-01", to = "2026-05-30")

        val url = server.takeRequest().requestUrl!!
        assertEquals("ACTIVE", url.queryParameter("status"))
        assertEquals("5", url.queryParameter("modifiedById"))
        // mobile-trading date-only filter normalizovan na pun ISO LocalDateTime
        // (BE radi LocalDateTime.parse → bare YYYY-MM-DD baca → 400).
        assertEquals("2026-05-01T00:00:00", url.queryParameter("from"))
        assertEquals("2026-05-30T23:59:59", url.queryParameter("to"))
    }

    @Test
    fun negotiationHistory_fullDateTimeFilter_passedAsIs() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"content":[],"totalElements":0,"totalPages":0,"number":0,"size":20,"first":true,"last":true,"empty":true}"""
            )
        )

        repo.negotiationHistory(from = "2026-05-01T08:00:00", to = "2026-05-30T18:00:00")

        val url = server.takeRequest().requestUrl!!
        assertEquals("2026-05-01T08:00:00", url.queryParameter("from"))
        assertEquals("2026-05-30T18:00:00", url.queryParameter("to"))
    }

    @Test
    fun acceptIntra_sendsBuyerAccountIdAsQuery_notBody() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":1,"listingId":5,"quantity":10,"pricePerStock":100.0,"premium":5.0,"status":"ACCEPTED"}"""
            )
        )

        val offer = OtcOfferDto(
            id = 1L, listingId = 5L, quantity = 10, pricePerStock = 100.0, premium = 5.0, status = "ACTIVE"
        )
        val result = repo.accept(inter = false, offer = offer, buyerAccountId = 777L)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/otc/offers/1/accept?buyerAccountId=777", request.path)
        assertEquals("", request.body.readUtf8())  // BE cita query, ne body
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun negotiationHistory_403_mapsToForbidden() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("Forbidden"))

        val result = repo.negotiationHistory()
        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Forbidden, (result as ApiResult.Failure).error.kind)
    }
}
