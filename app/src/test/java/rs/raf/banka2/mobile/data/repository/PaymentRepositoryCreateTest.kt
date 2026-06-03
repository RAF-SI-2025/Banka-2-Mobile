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
import rs.raf.banka2.mobile.data.dto.payment.CreatePaymentRequestDto
import java.math.BigDecimal

/**
 * MockWebServer test za PaymentRepository.create + getMyPayments — validira
 * STVARNE putanje + CreatePaymentRequest JSON oblik protiv BE `payments`
 * kontrolera.
 *
 * Verifikuje:
 *  - POST `/payments` sa CreatePaymentRequestDto telom (otpCode, amount,
 *    toAccountNumber, recipientName, paymentCode, paymentPurpose)
 *  - getMyPayments GET `/payments` sa page/limit/status/accountNumber query
 *  - parsiranje PaymentResponseDto + PageResponse wrapping
 *  - 422 (validation) i 401 (OTP) mapiranja
 */
class PaymentRepositoryCreateTest {

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

    private fun sampleRequest() = CreatePaymentRequestDto(
        fromAccountId = 3L,
        fromAccountNumber = "222000001111222233",
        toAccountNumber = "222000004444555566",
        amount = BigDecimal("1500"),
        currency = "RSD",
        recipientName = "Pera Peric",
        paymentCode = "289",
        paymentPurpose = "Racun za struju",
        referenceNumber = "97-1234",
        description = "Racun za struju",
        otpCode = "654321"
    )

    @Test
    fun create_postsPaymentsPath_withRequestBody_andParsesResponse() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":900,"toAccount":"222000004444555566","amount":1500.0,"currency":"RSD","status":"COMPLETED","recipientName":"Pera Peric"}"""
            )
        )

        val result = repo.create(sampleRequest())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/payments", request.path)
        val body = request.body.readUtf8()
        assertTrue("got=$body", body.contains("\"toAccountNumber\":\"222000004444555566\""))
        assertTrue("got=$body", body.contains("\"recipientName\":\"Pera Peric\""))
        assertTrue("got=$body", body.contains("\"paymentCode\":\"289\""))
        assertTrue("got=$body", body.contains("\"otpCode\":\"654321\""))

        assertTrue(result is ApiResult.Success)
        val payment = (result as ApiResult.Success).data
        assertEquals(900L, payment.id)
        assertEquals("COMPLETED", payment.status)
        assertEquals(BigDecimal("1500.0"), payment.amount)
    }

    @Test
    fun getMyPayments_sendsPagingAndFilterQuery_andUnwrapsContent() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"content":[{"id":1,"amount":500.0,"status":"COMPLETED"}],"totalElements":1,"totalPages":1,"number":0,"size":20,"first":true,"last":true,"empty":false}"""
            )
        )

        val result = repo.getMyPayments(page = 2, limit = 10, accountNumber = "222111", status = "PENDING")

        val url = server.takeRequest().requestUrl!!
        assertEquals(listOf("payments"), url.encodedPathSegments)
        assertEquals("2", url.queryParameter("page"))
        assertEquals("10", url.queryParameter("limit"))
        assertEquals("222111", url.queryParameter("accountNumber"))
        assertEquals("PENDING", url.queryParameter("status"))

        assertTrue(result is ApiResult.Success)
        val list = (result as ApiResult.Success).data
        assertEquals(1, list.size)
        assertEquals(1L, list[0].id)
    }

    @Test
    fun create_422_mapsToValidation() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"message":"Iznos prelazi dnevni limit"}"""))

        val result = repo.create(sampleRequest())

        assertEquals("/payments", server.takeRequest().path)
        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Validation, error.kind)
        assertEquals("Iznos prelazi dnevni limit", error.message)
    }

    @Test
    fun create_401_badOtp_mapsToUnauthorized() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Pogresan OTP kod"}"""))

        val result = repo.create(sampleRequest())

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Unauthorized, error.kind)
        assertEquals("Pogresan OTP kod", error.message)
    }
}
