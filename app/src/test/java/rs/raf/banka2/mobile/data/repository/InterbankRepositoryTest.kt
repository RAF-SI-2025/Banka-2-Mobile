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
import rs.raf.banka2.mobile.data.api.PaymentApi
import rs.raf.banka2.mobile.data.dto.interbank.InitiateInterbankPaymentDto
import java.math.BigDecimal

/**
 * Inter-bank 2PC wiring — real PaymentApi preko MockWebServer-a + real
 * InterbankRepository. Posle protocol-refactor-a (PR #96) inter-bank placanja
 * idu kroz standardni `/payments` endpoint, a status se prati pollovanjem
 * `GET /payments/{id}`.
 *
 * Verifikuje:
 *  - `initiate` POST-uje na `/payments` sa stvarnim CreatePaymentRequest telom
 *    (BE prepoznaje inter-bank iz routing prefiksa, ne iz posebnog DTO-a)
 *  - `status` GET-uje na `/payments/{id}` (transactionId je stringifikovan id)
 *  - `toInterbankTransaction()` status mapping:
 *      PENDING->INITIATED, PROCESSING->COMMITTING, COMPLETED->COMMITTED,
 *      REJECTED/CANCELLED->ABORTED, ostali (npr. ROLLED_BACK)->STUCK
 *  - nevalidan (ne-numericki) transactionId pada lokalno bez HTTP poziva
 */
class InterbankRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: InterbankRepository

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
            .create(PaymentApi::class.java)
        repo = InterbankRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun sampleInitiate() = InitiateInterbankPaymentDto(
        fromAccountId = 5L,
        toAccountNumber = "111000001234567890",
        amount = BigDecimal("2500"),
        recipientName = "Marko",
        paymentPurpose = "Racun",
        paymentCode = "289",
        referenceNumber = "97-123",
        otpCode = "123456"
    )

    @Test
    fun initiate_postsPaymentsPath_withInterbankPayload_andMapsPendingToInitiated() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":77,"toAccount":"111000001234567890","amount":2500.0,"currency":"RSD","status":"PENDING","recipientName":"Marko","fee":12.0,"createdAt":"2026-05-30T10:00:00"}"""
            )
        )

        val result = repo.initiate(sampleInitiate())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/payments", request.path)
        val body = request.body.readUtf8()
        // BE prepoznaje inter-bank iz routing prefiksa; payload je standardni CreatePaymentRequest.
        assertTrue("expected toAccountNumber in body, got=$body", body.contains("\"toAccountNumber\":\"111000001234567890\""))
        assertTrue("expected otpCode in body, got=$body", body.contains("\"otpCode\":\"123456\""))
        assertTrue("expected fromAccountId in body, got=$body", body.contains("\"fromAccountId\":5"))

        assertTrue(result is ApiResult.Success)
        val tx = (result as ApiResult.Success).data
        assertEquals("77", tx.transactionId)            // stringifikovan payment id
        assertEquals("INITIATED", tx.status)            // PENDING -> INITIATED
        assertEquals(BigDecimal("2500.0"), tx.amount)
        assertEquals(BigDecimal("12.0"), tx.fee)
    }

    @Test
    fun status_getsPaymentByIdPath_andMapsCompletedToCommitted() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":42,"amount":1000.0,"currency":"RSD","status":"COMPLETED"}"""
            )
        )

        val result = repo.status("42")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/payments/42", request.path)

        assertTrue(result is ApiResult.Success)
        val tx = (result as ApiResult.Success).data
        assertEquals("42", tx.transactionId)
        assertEquals("COMMITTED", tx.status)            // COMPLETED -> COMMITTED
        assertEquals("Placanje uspesno izvrseno.", tx.message)
    }

    @Test
    fun status_processing_mapsToCommitting() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":9,"status":"PROCESSING"}"""
            )
        )

        val tx = (repo.status("9") as ApiResult.Success).data
        assertEquals("/payments/9", server.takeRequest().path)
        assertEquals("COMMITTING", tx.status)
    }

    @Test
    fun status_rejected_mapsToAborted_withReason() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":3,"status":"REJECTED"}"""
            )
        )

        val tx = (repo.status("3") as ApiResult.Success).data
        assertEquals("ABORTED", tx.status)
        assertEquals("Placanje odbijeno.", tx.message)
    }

    @Test
    fun status_cancelled_mapsToAborted() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":4,"status":"CANCELLED"}"""
            )
        )

        val tx = (repo.status("4") as ApiResult.Success).data
        assertEquals("ABORTED", tx.status)
        assertEquals("Placanje otkazano.", tx.message)
    }

    @Test
    fun status_unknownStatus_mapsToStuck_defensively() = runTest {
        // BE PaymentStatus nema ROLLED_BACK; defensive else grana mapira sve
        // nepoznate statuse u STUCK da polling ne stane prerano.
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":8,"status":"ROLLED_BACK"}"""
            )
        )

        val tx = (repo.status("8") as ApiResult.Success).data
        assertEquals("STUCK", tx.status)
        assertNull(tx.message)
    }

    @Test
    fun status_nonNumericTransactionId_failsLocally_withoutHttpCall() = runTest {
        val result = repo.status("not-a-number")

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Validation, error.kind)
        assertEquals(0, server.requestCount)            // nikad nije gadjao server
    }

    @Test
    fun initiate_serverError_propagatesFailure() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"Nedovoljno sredstava"}"""))

        val result = repo.initiate(sampleInitiate())

        assertEquals("/payments", server.takeRequest().path)
        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Validation, error.kind)
        assertEquals("Nedovoljno sredstava", error.message)
    }
}
