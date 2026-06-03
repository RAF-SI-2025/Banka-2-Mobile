package rs.raf.banka2.mobile.data.api

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
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter

/**
 * MockWebServer-bazirani test koji bi uhvatio endpoint drift (P1-11):
 *  - exercise mora biti `POST /otc/contracts/{id}/exercise?buyerAccountId=...`
 *    (buyerAccountId je QUERY param, ne JSON body)
 *  - saga-status mora biti `GET /otc/saga/{sagaId}` (ne `/otc/contracts/{id}/saga-status`)
 *  - DTO-i moraju parsirati STVARNI BE JSON oblik
 *    (OtcExerciseResultDto / SagaStatusDto).
 */
class OtcApiSagaTest {

    private lateinit var server: MockWebServer
    private lateinit var api: OtcApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val moshi = Moshi.Builder()
            .add(BigDecimalAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OtcApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun exerciseIntra_sendsBuyerAccountIdAsQueryParam_andParsesExerciseResult() = runTest {
        // STVARNI BE oblik: OtcExerciseResultDto { sagaId, sagaStatus, currentStep, id, status }
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"sagaId":"saga-abc-123","sagaStatus":"COMPLETED","currentStep":5,"id":42,"status":"EXERCISED"}"""
            )
        )

        val response = api.exerciseIntra(contractId = 42L, buyerAccountId = 777L)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/otc/contracts/42/exercise?buyerAccountId=777", request.path)
        // buyerAccountId NE sme biti u body-ju (BE ga ignorise tamo)
        assertEquals("", request.body.readUtf8())

        assertTrue(response.isSuccessful)
        val body = response.body()!!
        assertEquals("saga-abc-123", body.sagaId)
        assertEquals("COMPLETED", body.sagaStatus)
        assertEquals(5, body.currentStep)
        assertEquals(42L, body.id)
        assertEquals("EXERCISED", body.status)
    }

    @Test
    fun exerciseIntra_withoutBuyerAccountId_omitsQueryParam() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"sagaId":"s1","sagaStatus":"COMPENSATED","currentStep":3,"id":9,"status":"ACTIVE"}"""
            )
        )

        api.exerciseIntra(contractId = 9L, buyerAccountId = null)

        val request = server.takeRequest()
        assertEquals("/otc/contracts/9/exercise", request.path)
    }

    @Test
    fun sagaStatusIntra_hitsSagaIdPath_andParsesSagaStatus() = runTest {
        // STVARNI BE oblik: SagaStatusDto { sagaId, status, currentStep, log[ {phase,kind,outcome,message,at} ] }
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "sagaId":"saga-abc-123",
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

        val response = api.sagaStatusIntra(sagaId = "saga-abc-123")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/otc/saga/saga-abc-123", request.path)

        assertTrue(response.isSuccessful)
        val body = response.body()!!
        assertEquals("saga-abc-123", body.sagaId)
        assertEquals("COMPLETED", body.status)
        assertEquals(5, body.currentStep)
        assertEquals(2, body.log.size)
        assertEquals(1, body.log[0].phase)
        assertEquals("FORWARD", body.log[0].kind)
        assertEquals("ok", body.log[0].outcome)
        assertNull(body.log[0].message)
    }
}
