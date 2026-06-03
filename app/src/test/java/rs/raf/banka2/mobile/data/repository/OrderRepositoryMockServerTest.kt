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
import rs.raf.banka2.mobile.data.api.OrderApi
import rs.raf.banka2.mobile.data.dto.order.CreateOrderDto
import java.math.BigDecimal

/**
 * MockWebServer test za OrderRepository.create — validira STVARNI HTTP path +
 * CreateOrderDto JSON oblik protiv BE `orders` kontrolera.
 *
 * Verifikuje:
 *  - POST `/orders` sa CreateOrderDto telom (listingId/orderType/direction/
 *    quantity/limitPrice/stopPrice/margin/accountId/onBehalfOfFundId/otpCode)
 *  - MARKET order izostavlja null limit/stop cene
 *  - LIMIT order salje limitPrice
 *  - parsiranje OrderDto odgovora (status, filledQuantity)
 *  - 403 (no TRADE_STOCKS) i 409 mapiranja
 */
class OrderRepositoryMockServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: OrderRepository

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
            .create(OrderApi::class.java)
        repo = OrderRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun create_marketOrder_postsOrdersPath_omitsNullPrices_andParsesResponse() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":501,"orderType":"MARKET","direction":"BUY","quantity":10,"filledQuantity":0,"status":"PENDING"}"""
            )
        )

        val result = repo.create(
            CreateOrderDto(
                listingId = 88L,
                orderType = "MARKET",
                direction = "BUY",
                quantity = 10,
                limitPrice = null,
                stopPrice = null,
                allOrNone = false,
                margin = false,
                accountId = 3L,
                onBehalfOfFundId = null,
                otpCode = "123456"
            )
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/orders", request.path)
        val body = request.body.readUtf8()
        assertTrue("got=$body", body.contains("\"listingId\":88"))
        assertTrue("got=$body", body.contains("\"orderType\":\"MARKET\""))
        assertTrue("got=$body", body.contains("\"direction\":\"BUY\""))
        assertTrue("got=$body", body.contains("\"quantity\":10"))
        assertTrue("got=$body", body.contains("\"accountId\":3"))
        assertTrue("got=$body", body.contains("\"otpCode\":\"123456\""))
        // null cene se izostavljaju (BE wire imena: limitValue/stopValue/fundId)
        assertFalse("limitValue should be omitted, got=$body", body.contains("limitValue"))
        assertFalse("stopValue should be omitted, got=$body", body.contains("stopValue"))
        assertFalse("fundId should be omitted, got=$body", body.contains("fundId"))

        assertTrue(result is ApiResult.Success)
        val order = (result as ApiResult.Success).data
        assertEquals(501L, order.id)
        assertEquals("PENDING", order.status)
    }

    @Test
    fun create_limitOrder_sendsLimitValue_andFundId() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":7,"orderType":"LIMIT","direction":"SELL","quantity":5,"status":"PENDING","fundId":42}"""
            )
        )

        val result = repo.create(
            CreateOrderDto(
                listingId = 2L,
                orderType = "LIMIT",
                direction = "SELL",
                quantity = 5,
                limitPrice = BigDecimal("123.45"),
                stopPrice = null,
                allOrNone = true,
                margin = true,
                accountId = null,
                onBehalfOfFundId = 42L,
                otpCode = "999999"
            )
        )

        val body = server.takeRequest().body.readUtf8()
        assertTrue("got=$body", body.contains("\"orderType\":\"LIMIT\""))
        // R1-162: BE wire ime za limit cenu je `limitValue` (NE `limitPrice`)
        assertTrue("got=$body", body.contains("\"limitValue\":123.45"))
        assertTrue("got=$body", body.contains("\"allOrNone\":true"))
        assertTrue("got=$body", body.contains("\"margin\":true"))
        assertTrue("got=$body", body.contains("\"fundId\":42"))
        // R6-1987: read OrderDto mapira BE `fundId` -> onBehalfOfFundId
        assertTrue(result is ApiResult.Success)
        assertEquals(42L, (result as ApiResult.Success).data.onBehalfOfFundId)
    }

    @Test
    fun create_403_noTradePermission_mapsToForbidden() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Nemate dozvolu za trgovanje"}"""))

        val result = repo.create(
            CreateOrderDto(
                listingId = 1L, orderType = "MARKET", direction = "BUY",
                quantity = 1, otpCode = "000000"
            )
        )

        assertEquals("/orders", server.takeRequest().path)
        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Forbidden, error.kind)
        assertEquals("Nemate dozvolu za trgovanje", error.message)
    }

    @Test
    fun create_409_mapsToConflict() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"message":"Berza je zatvorena"}"""))

        val result = repo.create(
            CreateOrderDto(listingId = 1L, orderType = "MARKET", direction = "BUY", quantity = 1, otpCode = "000000")
        )

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Kind.Conflict, (result as ApiResult.Failure).error.kind)
    }
}
