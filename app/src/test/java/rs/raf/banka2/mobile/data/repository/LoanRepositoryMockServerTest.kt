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
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import rs.raf.banka2.mobile.data.api.LoanApi
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationDto
import java.math.BigDecimal

/**
 * P1-mobile-banking-1: LoanApi/LoanRepository kontrakt protiv STVARNOG BE oblika
 * (MockWebServer). Pokriva:
 *  - R1-263: early-repay salje `X-OTP-Code` header (bez njega BE-PAY-06 → 403).
 *  - R1-131: loan apply body sadrzi BE @NotNull `interestType`/`repaymentPeriod` +
 *    @NotBlank `accountNumber`/`currency` (inace 400).
 */
class LoanRepositoryMockServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: LoanRepository

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
            .create(LoanApi::class.java)
        repo = LoanRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun earlyRepay_sendsOtpHeader() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":5,"amount":1000.0,"currency":"RSD","status":"ACTIVE"}"""))

        val result = repo.earlyRepay(id = 5L, otpCode = "123456")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/loans/5/early-repayment", request.path)
        assertEquals("123456", request.getHeader("X-OTP-Code"))
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun apply_sendsBeRequiredFields() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":1,"status":"PENDING"}"""))

        repo.apply(
            LoanApplicationDto(
                loanType = "CASH",
                interestType = "FIXED",
                amount = BigDecimal("500000"),
                currency = "RSD",
                repaymentPeriod = 24,
                accountNumber = "222-ACC",
                loanPurpose = "Renoviranje",
                otpCode = "654321"
            )
        )

        val body = server.takeRequest().body.readUtf8()
        assertTrue("interestType nedostaje: $body", body.contains("\"interestType\":\"FIXED\""))
        assertTrue("repaymentPeriod nedostaje: $body", body.contains("\"repaymentPeriod\":24"))
        assertTrue("accountNumber nedostaje: $body", body.contains("\"accountNumber\":\"222-ACC\""))
        assertTrue("currency nedostaje: $body", body.contains("\"currency\":\"RSD\""))
    }
}
