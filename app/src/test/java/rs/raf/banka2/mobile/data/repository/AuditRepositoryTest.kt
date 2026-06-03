package rs.raf.banka2.mobile.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import rs.raf.banka2.mobile.data.api.AuditApi

/**
 * MockWebServer-bazirani test koji validira STVARNI HTTP path + query parametre
 * (P1-10): prethodni MockK test je mockovao Api pa nikad nije uhvatio da Mobile
 * gadja `/audit-logs` umesto BE-ovog `/audit` (404 na zivo).
 *
 * Verifikuje:
 *  - path je `/audit` (ne `/audit-logs`)
 *  - actorEmail se NE prosledjuje (BE nema email filter — samo actorId)
 *  - datum filteri se konvertuju u ISO LocalDateTime (`from`=pocetak dana, `to`=kraj dana)
 *  - blank filteri se izostavljaju
 *  - 403 mapira na ApiError.Kind.Forbidden
 */
class AuditRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: AuditRepository

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
            .create(AuditApi::class.java)
        repo = AuditRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueEmptyPage() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"content":[],"totalElements":0,"totalPages":0,"number":0,"size":20,"first":true,"last":true,"empty":true}"""
            )
        )
    }

    @Test
    fun query_hitsAuditPath_withMappedParams() = runTest {
        enqueueEmptyPage()

        val result = repo.query(
            actionType = "ORDER_APPROVED",
            actorId = 100L,
            dateFrom = "2026-05-01",
            dateTo = "2026-05-30",
            page = 2,
            size = 50
        )

        assertTrue(result is ApiResult.Success)
        val request = server.takeRequest()
        val url = request.requestUrl!!

        // Path mora biti /audit (NE /audit-logs)
        assertEquals(listOf("audit"), url.encodedPathSegments)
        assertEquals("GET", request.method)

        // Mapirani query parametri
        assertEquals("ORDER_APPROVED", url.queryParameter("actionType"))
        assertEquals("100", url.queryParameter("actorId"))
        assertEquals("2026-05-01T00:00:00", url.queryParameter("from"))
        assertEquals("2026-05-30T23:59:59", url.queryParameter("to"))
        assertEquals("2", url.queryParameter("page"))
        assertEquals("50", url.queryParameter("size"))

        // actorEmail / dateFrom / dateTo se NE prosledjuju kao takvi
        assertEquals(null, url.queryParameter("actorEmail"))
        assertEquals(null, url.queryParameter("dateFrom"))
        assertEquals(null, url.queryParameter("dateTo"))
    }

    @Test
    fun query_actorName_isMappedToActorNameQueryParam() = runTest {
        // Sc45 — filter po IMENU aktera (supervizora). BE razresi ime -> actorId-eve.
        enqueueEmptyPage()

        repo.query(actorName = "Nikola Milenkovic")

        val url = server.takeRequest().requestUrl!!
        assertEquals(listOf("audit"), url.encodedPathSegments)
        assertEquals("Nikola Milenkovic", url.queryParameter("actorName"))
    }

    @Test
    fun query_blankActorName_isOmitted() = runTest {
        enqueueEmptyPage()

        repo.query(actorName = "")

        val url = server.takeRequest().requestUrl!!
        assertEquals(null, url.queryParameter("actorName"))
    }

    @Test
    fun query_blankFilters_areOmitted() = runTest {
        enqueueEmptyPage()

        repo.query(actionType = "", dateFrom = "", dateTo = "")

        val url = server.takeRequest().requestUrl!!
        assertEquals(listOf("audit"), url.encodedPathSegments)
        assertEquals(null, url.queryParameter("actionType"))
        assertEquals(null, url.queryParameter("from"))
        assertEquals(null, url.queryParameter("to"))
        assertEquals(null, url.queryParameter("actorEmail"))
    }

    @Test
    fun query_parsesSuccessPage() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "content":[{"id":1,"actionType":"ORDER_APPROVED","actorId":100,"createdAt":"2026-05-25T10:00:00"}],
                  "totalElements":1,"totalPages":1,"number":0,"size":20,"first":true,"last":true,"empty":false
                }
                """.trimIndent()
            )
        )

        val result = repo.query()
        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(1, data.content.size)
        assertEquals("ORDER_APPROVED", data.content[0].actionType)
    }

    @Test
    fun query_403_mapsToForbidden() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("Forbidden"))

        val result = repo.query()
        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Forbidden, (result as ApiResult.Failure).error.kind)
    }
}
