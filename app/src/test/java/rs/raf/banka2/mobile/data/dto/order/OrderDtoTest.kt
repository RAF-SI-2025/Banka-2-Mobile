package rs.raf.banka2.mobile.data.dto.order

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * P1-mobile-trading-1 (R1-162 / R6-1987): Order DTO kontrakt protiv BE
 * `rs.raf.trading.order.dto.{CreateOrderDto,OrderDto}`.
 *
 *  - WRITE: BE cita `limitValue`/`stopValue`/`fundId` (NE `limitPrice`/
 *    `stopPrice`/`onBehalfOfFundId`) → LIMIT/STOP/fund order pre fix-a bio 400/bez cene.
 *  - READ: BE salje `limitValue`/`stopValue`/`pricePerUnit`/`fundId`/
 *    `lastModification` → polja pre fix-a uvek null.
 */
class OrderDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun createOrder_serializesBeWireNames_limitValue_stopValue_fundId() {
        val adapter = moshi.adapter(CreateOrderDto::class.java)
        val json = adapter.toJson(
            CreateOrderDto(
                listingId = 5L,
                orderType = "STOP_LIMIT",
                direction = "BUY",
                quantity = 3,
                limitPrice = BigDecimal("123.45"),
                stopPrice = BigDecimal("130.00"),
                onBehalfOfFundId = 77L,
                otpCode = "123456"
            )
        )
        assertTrue("got=$json", json.contains("\"limitValue\":123.45"))
        assertTrue("got=$json", json.contains("\"stopValue\":130.00"))
        assertTrue("got=$json", json.contains("\"fundId\":77"))
        // stara (pogresna) imena se NE smeju pojaviti
        assertTrue("got=$json", !json.contains("limitPrice"))
        assertTrue("got=$json", !json.contains("stopPrice"))
        assertTrue("got=$json", !json.contains("onBehalfOfFundId"))
    }

    @Test
    fun orderDto_readsBeWireNames() {
        val adapter = moshi.adapter(OrderDto::class.java)
        val json = """
            {
              "id": 1,
              "listingId": 5,
              "listingTicker": "AAPL",
              "orderType": "LIMIT",
              "direction": "BUY",
              "quantity": 10,
              "remainingPortions": 4,
              "limitValue": 150.00,
              "stopValue": 140.00,
              "pricePerUnit": 149.50,
              "approximatePrice": 151.00,
              "status": "APPROVED",
              "fundId": 88,
              "lastModification": "2026-06-01T10:30:00"
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(BigDecimal("150.00"), dto!!.limitPrice)
        assertEquals(BigDecimal("140.00"), dto.stopPrice)
        assertEquals(BigDecimal("149.50"), dto.pricePerUnit)
        assertEquals(BigDecimal("151.00"), dto.approximatePrice)
        assertEquals(88L, dto.onBehalfOfFundId)
        assertEquals("2026-06-01T10:30:00", dto.updatedAt)
    }
}
