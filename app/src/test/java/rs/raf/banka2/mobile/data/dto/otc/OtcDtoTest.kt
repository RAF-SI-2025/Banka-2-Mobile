package rs.raf.banka2.mobile.data.dto.otc

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter

/**
 * P1-mobile-trading-1 (R1-211/212/213): intra OTC DTO kontrakt protiv BE
 * `rs.raf.trading.otc.dto.{OtcListingDto,OtcOfferDto,OtcContractDto,CreateOtcOfferDto}`.
 *
 *  - Listing/Offer cita `sellerId`/`listingTicker`/`listingCurrency`/
 *    `lastModifiedAt`/`lastModifiedByName` (ranije sellerUserId/ticker/currency/
 *    lastModified/modifiedBy → null → discovery prazan, create 400).
 *  - Contract cita `profit` (server-side, ranije profitEstimate → null).
 *  - CreateOtcOfferDto serializuje `sellerId` (BE @NotNull).
 */
class OtcDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun otcListing_readsBeFieldNames() {
        val adapter = moshi.adapter(OtcListingDto::class.java)
        val json = """
            {
              "listingId": 5,
              "listingTicker": "AAPL",
              "listingName": "Apple Inc.",
              "sellerId": 42,
              "sellerName": "Pera",
              "sellerRole": "CLIENT",
              "availablePublicQuantity": 30,
              "listingCurrency": "USD",
              "currentPrice": 190.5
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals("AAPL", dto!!.ticker)
        assertEquals("Apple Inc.", dto.name)
        assertEquals(42L, dto.sellerUserId)
        assertEquals(30, dto.publicQuantity)
        assertEquals("USD", dto.currency)
    }

    @Test
    fun createOtcOffer_serializesSellerId() {
        val adapter = moshi.adapter(CreateOtcOfferDto::class.java)
        val json = adapter.toJson(
            CreateOtcOfferDto(
                listingId = 5L,
                sellerUserId = 42L,
                quantity = 10,
                pricePerStock = 100.0,
                premium = 5.0,
                settlementDate = "2026-12-01"
            )
        )
        assertTrue("got=$json", json.contains("\"sellerId\":42"))
        assertTrue("got=$json", !json.contains("sellerUserId"))
    }

    @Test
    fun otcOffer_readsBeCurrencyAndLastModified() {
        val adapter = moshi.adapter(OtcOfferDto::class.java)
        val json = """
            {
              "id": 1,
              "listingId": 5,
              "listingTicker": "AAPL",
              "listingCurrency": "USD",
              "quantity": 10,
              "pricePerStock": 100.0,
              "premium": 5.0,
              "buyerId": 7,
              "sellerId": 42,
              "status": "ACTIVE",
              "lastModifiedAt": "2026-06-01T10:30:00",
              "lastModifiedByName": "Pera"
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals("USD", dto!!.currency)
        assertEquals("2026-06-01T10:30:00", dto.lastModified)
        assertEquals("Pera", dto.modifiedBy)
        assertEquals(7L, dto.buyerId)
        assertEquals(42L, dto.sellerId)
    }

    @Test
    fun otcContract_readsBeProfit() {
        val adapter = moshi.adapter(OtcContractDto::class.java)
        val json = """
            {
              "id": 1,
              "listingId": 5,
              "quantity": 10,
              "strikePrice": 100.0,
              "premium": 5.0,
              "status": "ACTIVE",
              "buyerId": 7,
              "sellerId": 42,
              "currentPrice": 125.0,
              "profit": 1350.0
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(1350.0, dto!!.profitEstimate!!, 0.0001)
        assertEquals(7L, dto.buyerId)
        assertEquals(42L, dto.sellerId)
    }

    /**
     * R6-2045 regression-pin: BE (Nastavak2) salje server-authoritativni `profit`
     * (Celina4 §149) — NE legacy `profitEstimate`. Ako bi se `@Json(name="profit")`
     * regresiralo nazad na citanje `profitEstimate` kljuca, ovaj test bi pao:
     * placanje BE `profit` JSON kljuca mora da napuni polje, a BE NE salje
     * `profitEstimate` kljuc pa odsustvo `profit`-a → null (server profit prazan).
     */
    @Test
    fun otcContract_readsProfitKey_notLegacyProfitEstimateKey() {
        val adapter = moshi.adapter(OtcContractDto::class.java)
        // Telo nosi STARI kljuc `profitEstimate` (koji BE vise NE salje) — mora se ignorisati.
        val legacyKeyJson = """
            {
              "id": 2,
              "listingId": 5,
              "quantity": 10,
              "strikePrice": 100.0,
              "premium": 5.0,
              "status": "EXERCISED",
              "profitEstimate": 1350.0
            }
        """.trimIndent()
        val dto = adapter.fromJson(legacyKeyJson)
        assertNotNull(dto)
        // citamo BE `profit` kljuc — legacy `profitEstimate` u telu se NE mapira → null.
        assertNull("legacy profitEstimate kljuc se ne sme citati (BE salje 'profit')", dto!!.profitEstimate)
    }
}
