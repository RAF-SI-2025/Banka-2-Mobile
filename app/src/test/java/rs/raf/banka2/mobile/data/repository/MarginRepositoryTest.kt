package rs.raf.banka2.mobile.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import rs.raf.banka2.mobile.data.api.MarginApi
import java.math.BigDecimal

/**
 * MockWebServer-bazirani test za MarginRepository — validira STVARNE HTTP
 * putanje + JSON oblike protiv BE `margin-accounts` kontrolera.
 *
 * Verifikuje:
 *  - create POST-uje na `/margin-accounts` sa CreateMarginAccountDto telom
 *    (initialMargin/maintenanceMargin/bankParticipation/userId/companyId)
 *  - deposit/withdraw POST-uju na `/margin-accounts/{id}/{deposit|withdraw}`
 *    sa `{"amount":...}` telom
 *  - myAccounts/transactions GET-uju ispravne putanje i parsiraju listu
 *  - 409 / 400 mapiranja
 */
class MarginRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: MarginRepository

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
            .create(MarginApi::class.java)
        repo = MarginRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun create_postsMarginAccountsPath_withBody_andParsesResponse() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":12,"accountNumber":"222-MARGIN-01","initialMargin":100000.0,"maintenanceMargin":50000.0,"loanValue":0.0,"bankParticipation":0.5,"currency":"RSD","active":true,"status":"ACTIVE"}"""
            )
        )

        val result = repo.create(
            accountId = 17L,
            initialMargin = BigDecimal("100000.0"),
            maintenanceMargin = BigDecimal("50000.0"),
            bankParticipation = BigDecimal("0.5"),
            userId = 7L,
            companyId = null
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/margin-accounts", request.path)
        val body = request.body.readUtf8()
        // R1-202: accountId (BE @NotNull) MORA biti u telu, inace 400.
        assertTrue("got=$body", body.contains("\"accountId\":17"))
        assertTrue("got=$body", body.contains("\"initialMargin\":100000.0"))
        assertTrue("got=$body", body.contains("\"maintenanceMargin\":50000.0"))
        assertTrue("got=$body", body.contains("\"bankParticipation\":0.5"))
        assertTrue("got=$body", body.contains("\"userId\":7"))
        // companyId == null -> Moshi izostavlja polje
        assertFalse("companyId should be omitted, got=$body", body.contains("companyId"))

        assertTrue(result is ApiResult.Success)
        val acc = (result as ApiResult.Success).data
        assertEquals(12L, acc.id)
        assertEquals(BigDecimal("100000.0"), acc.initialMargin)
        assertEquals(BigDecimal("0.5"), acc.bankParticipation)
    }

    @Test
    fun deposit_postsDepositPath_withAmountBody_parsesMessage() = runTest {
        // R1-269: BE deposit vraca `{"message":...}` (Map), ne MarginAccountDto.
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"message":"Deposit successful"}"""
            )
        )

        val result = repo.deposit(id = 12L, amount = BigDecimal("5000.0"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/margin-accounts/12/deposit", request.path)
        assertEquals("""{"amount":5000.0}""", request.body.readUtf8())
        assertTrue(result is ApiResult.Success)
        assertEquals("Deposit successful", (result as ApiResult.Success).data.message)
    }

    @Test
    fun withdraw_postsWithdrawPath_withAmountBody_parsesMessage() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"message":"Withdrawal successful"}"""
            )
        )

        val result = repo.withdraw(id = 12L, amount = BigDecimal("4000.0"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/margin-accounts/12/withdraw", request.path)
        assertEquals("""{"amount":4000.0}""", request.body.readUtf8())
        assertTrue(result is ApiResult.Success)
        assertEquals("Withdrawal successful", (result as ApiResult.Success).data.message)
    }

    @Test
    fun myAccounts_getsMyPath_andParsesList() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":1,"initialMargin":1000.0,"maintenanceMargin":500.0,"active":true},{"id":2,"initialMargin":2000.0,"maintenanceMargin":900.0,"active":false}]"""
            )
        )

        val result = repo.myAccounts()

        assertEquals("/margin-accounts/my", server.takeRequest().path)
        assertTrue(result is ApiResult.Success)
        val list = (result as ApiResult.Success).data
        assertEquals(2, list.size)
        assertEquals(2L, list[1].id)
        assertFalse(list[1].active)
    }

    @Test
    fun transactions_getsTransactionsPath_andParsesList() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":1,"type":"DEPOSIT","amount":5000.0,"balanceAfter":5000.0}]"""
            )
        )

        val result = repo.transactions(12L)

        assertEquals("/margin-accounts/12/transactions", server.takeRequest().path)
        assertTrue(result is ApiResult.Success)
        val list = (result as ApiResult.Success).data
        assertEquals(1, list.size)
        assertEquals("DEPOSIT", list[0].type)
    }

    @Test
    fun withdraw_409_mapsToConflict() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"message":"Nedovoljno sredstava na margin racunu"}"""))

        val result = repo.withdraw(id = 12L, amount = BigDecimal("999999.0"))

        assertEquals("/margin-accounts/12/withdraw", server.takeRequest().path)
        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Conflict, error.kind)
        assertEquals("Nedovoljno sredstava na margin racunu", error.message)
    }

    @Test
    fun create_400_mapsToValidation() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"bankParticipation mora biti u [0..1]"}"""))

        val result = repo.create(accountId = 17L, initialMargin = BigDecimal("1.0"), maintenanceMargin = BigDecimal("1.0"), bankParticipation = BigDecimal("2.0"), userId = 1L, companyId = null)

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Validation, (result as ApiResult.Failure).error.kind)
    }
}
