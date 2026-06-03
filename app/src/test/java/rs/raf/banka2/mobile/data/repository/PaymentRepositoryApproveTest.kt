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
import rs.raf.banka2.mobile.data.api.PaymentApi

/**
 * P2-8: Quick Approve wiring — real PaymentApi preko MockWebServer-a + real
 * PaymentRepository. Validira da `approveQuick` gadja STVARNI BE path
 * `POST /payments/{id}/approve` sa telom `{"otpCode":"..."}` (matchuje BE
 * `ApprovePaymentRequest`) i da DTO parsira BE `PaymentResponseDto`.
 */
class PaymentRepositoryApproveTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: PaymentRepository

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
        repo = PaymentRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun approveQuick_postsApprovePathWithOtpBody_andParsesResponse() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":42,"toAccount":"265000001234567890","amount":1500.0,"currency":"RSD","status":"COMPLETED","recipientName":"Pera"}"""
            )
        )

        val result = repo.approveQuick(id = 42L, otpCode = "123456")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/payments/42/approve", request.path)
        assertEquals("""{"otpCode":"123456"}""", request.body.readUtf8())

        assertTrue(result is ApiResult.Success)
        val body = (result as ApiResult.Success).data
        assertEquals(42L, body.id)
        assertEquals("COMPLETED", body.status)
    }

    @Test
    fun approveQuick_401_mapsToUnauthorized() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Pogresan OTP"}"""))

        val result = repo.approveQuick(id = 7L, otpCode = "000000")

        assertEquals("/payments/7/approve", server.takeRequest().path)
        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Unauthorized, error.kind)
        assertEquals("Pogresan OTP", error.message)
    }
}
