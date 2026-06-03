package rs.raf.banka2.mobile.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import rs.raf.banka2.mobile.data.api.CardApi
import java.math.BigDecimal

/**
 * TEST-mobile-banking-vm-1 (R4-1364-repos): MockWebServer wire-test za
 * [CardRepository] — validira STVARNE putanje + JSON oblik tela + Idempotency-Key
 * header protiv BE `cards` kontrolera. Verifikuje:
 *  - GET /cards parsira BigDecimal polja (cardLimit/prepaidBalance) bez Double round-off
 *  - PATCH /cards/{id}/block putanju
 *  - PATCH /cards/{id}/limit telo (cardLimit BigDecimal)
 *  - POST /cards/{id}/top-up i /withdraw: telo + obavezan Idempotency-Key header (R5-1849)
 *  - 400/403 mapiranja
 */
class CardRepositoryMockServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: CardRepository

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
            .create(CardApi::class.java)
        repo = CardRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun myCards_getsCardsPath_andParsesBigDecimal() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":7,"cardCategory":"INTERNET_PREPAID","status":"ACTIVE","cardLimit":100000.55,"prepaidBalance":1234.56,"accountId":1}]"""
            )
        )

        val result = repo.myCards()

        assertEquals("/cards", server.takeRequest().path)
        assertTrue(result is ApiResult.Success)
        val card = (result as ApiResult.Success).data[0]
        assertEquals(7L, card.id)
        assertTrue(card.isPrepaid)
        assertEquals(BigDecimal("100000.55"), card.cardLimit)
        assertEquals(BigDecimal("1234.56"), card.prepaidBalance)
    }

    @Test
    fun block_patchesBlockPath() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":7,"status":"BLOCKED"}"""))

        val result = repo.block(7L)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/cards/7/block", request.path)
        assertTrue(result is ApiResult.Success)
        assertEquals("BLOCKED", (result as ApiResult.Success).data.status)
    }

    @Test
    fun updateLimit_patchesLimitPath_withCardLimitBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":7,"cardLimit":250000}"""))

        val result = repo.updateLimit(7L, BigDecimal("250000"))

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/cards/7/limit", request.path)
        val body = request.body.readUtf8()
        assertTrue("got=$body", body.contains("\"cardLimit\":250000"))
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun topUp_postsTopUpPath_withBody_andIdempotencyKeyHeader() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":7,"prepaidBalance":1500}"""))

        val result = repo.topUp(cardId = 7L, sourceAccountId = 3L, amount = BigDecimal("500.25"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/cards/7/top-up", request.path)
        val body = request.body.readUtf8()
        assertTrue("got=$body", body.contains("\"sourceAccountId\":3"))
        assertTrue("got=$body", body.contains("\"amount\":500.25"))
        // R5-1849: Idempotency-Key header mora postojati (UUID po pozivu)
        assertNotNull("Idempotency-Key header mora biti poslat", request.getHeader("Idempotency-Key"))
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun topUp_explicitIdempotencyKey_isForwardedVerbatim() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":7}"""))

        repo.topUp(7L, 3L, BigDecimal("100"), idempotencyKey = "fixed-key-123")

        assertEquals("fixed-key-123", server.takeRequest().getHeader("Idempotency-Key"))
    }

    @Test
    fun withdrawFromCard_postsWithdrawPath_withTargetAccount_andIdempotencyKey() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":7,"prepaidBalance":400}"""))

        val result = repo.withdrawFromCard(cardId = 7L, targetAccountId = 9L, amount = BigDecimal("100"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/cards/7/withdraw", request.path)
        val body = request.body.readUtf8()
        assertTrue("got=$body", body.contains("\"targetAccountId\":9"))
        assertNotNull(request.getHeader("Idempotency-Key"))
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun topUp_400_mapsToValidation() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"Kartica nije prepaid."}"""))

        val result = repo.topUp(7L, 3L, BigDecimal("100"))

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Validation, error.kind)
        assertEquals("Kartica nije prepaid.", error.message)
    }

    @Test
    fun block_403_mapsToForbidden() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Nemate dozvolu."}"""))

        val result = repo.block(7L)

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Forbidden, (result as ApiResult.Failure).error.kind)
    }
}
