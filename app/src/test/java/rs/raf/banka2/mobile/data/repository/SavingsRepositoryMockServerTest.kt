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
import rs.raf.banka2.mobile.data.api.SavingsApi
import rs.raf.banka2.mobile.data.dto.savings.OpenDepositRequest
import java.math.BigDecimal

/**
 * TEST-mobile-banking-vm-1 (R4-1364-repos): MockWebServer wire-test za
 * [SavingsRepository] — validira STVARNE putanje + JSON oblik protiv BE `savings`
 * kontrolera. Verifikuje:
 *  - GET /savings/deposits/my parsira SavingsDepositDto (BigDecimal principal/interest)
 *  - POST /savings/deposits telo (OpenDepositRequest sa otpCode)
 *  - GET /savings/rates?currency=... query param (per-currency)
 *  - POST /savings/deposits/{id}/withdraw-early telo (otpCode)
 *  - 400/409 mapiranja
 */
class SavingsRepositoryMockServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: SavingsRepository

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
            .create(SavingsApi::class.java)
        repo = SavingsRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val depositJson = """
        {
          "id": 5,
          "clientId": 1,
          "clientName": "Pera",
          "linkedAccountId": 10,
          "linkedAccountNumber": "222-RSD",
          "principalAmount": 250000.00,
          "currencyCode": "RSD",
          "termMonths": 12,
          "annualInterestRate": 0.06,
          "startDate": "2026-01-01",
          "maturityDate": "2027-01-01",
          "nextInterestPaymentDate": "2026-02-01",
          "totalInterestPaid": 1250.50,
          "autoRenew": false,
          "status": "ACTIVE",
          "createdAt": null,
          "updatedAt": null
        }
    """.trimIndent()

    @Test
    fun listMy_getsMyPath_andParsesBigDecimal() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$depositJson]"))

        val result = repo.listMy()

        assertEquals("/savings/deposits/my", server.takeRequest().path)
        assertTrue(result is ApiResult.Success)
        val d = (result as ApiResult.Success).data[0]
        assertEquals(5L, d.id)
        assertEquals(BigDecimal("250000.00"), d.principalAmount)
        assertEquals(BigDecimal("1250.50"), d.totalInterestPaid)
        assertEquals("RSD", d.currencyCode)
    }

    @Test
    fun openDeposit_postsDepositsPath_withRequestBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(depositJson))

        val result = repo.openDeposit(
            OpenDepositRequest(
                sourceAccountId = 1L,
                linkedAccountId = 10L,
                principalAmount = BigDecimal("250000.00"),
                termMonths = 12,
                autoRenew = false,
                otpCode = "654321"
            )
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/savings/deposits", request.path)
        val body = request.body.readUtf8()
        assertTrue("got=$body", body.contains("\"sourceAccountId\":1"))
        assertTrue("got=$body", body.contains("\"linkedAccountId\":10"))
        assertTrue("got=$body", body.contains("\"principalAmount\":250000.00"))
        assertTrue("got=$body", body.contains("\"otpCode\":\"654321\""))
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun getRates_sendsCurrencyQueryParam() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":1,"currencyCode":"EUR","termMonths":12,"annualRate":0.03,"active":true,"effectiveFrom":"2026-01-01"}]"""
            )
        )

        val result = repo.getRates("EUR")

        val url = server.takeRequest().requestUrl!!
        assertEquals(listOf("savings", "rates"), url.encodedPathSegments)
        assertEquals("EUR", url.queryParameter("currency"))
        assertTrue(result is ApiResult.Success)
        assertEquals("EUR", (result as ApiResult.Success).data[0].currencyCode)
    }

    @Test
    fun withdrawEarly_postsWithdrawEarlyPath_withOtp() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(depositJson))

        val result = repo.withdrawEarly(5L, "111222")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/savings/deposits/5/withdraw-early", request.path)
        assertTrue(request.body.readUtf8().contains("\"otpCode\":\"111222\""))
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun openDeposit_400_mapsToValidation() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"Iznos je ispod minimuma."}"""))

        val result = repo.openDeposit(
            OpenDepositRequest(1L, 10L, BigDecimal("1"), 12, false, "654321")
        )

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Validation, error.kind)
        assertEquals("Iznos je ispod minimuma.", error.message)
    }

    @Test
    fun withdrawEarly_409_mapsToConflict() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"message":"Depozit je vec raskinut."}"""))

        val result = repo.withdrawEarly(5L, "111222")

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Conflict, (result as ApiResult.Failure).error.kind)
    }
}
