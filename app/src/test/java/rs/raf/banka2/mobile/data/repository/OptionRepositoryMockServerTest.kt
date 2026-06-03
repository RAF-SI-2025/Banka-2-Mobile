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
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import rs.raf.banka2.mobile.data.api.OptionApi
import java.math.BigDecimal

/**
 * P0-M1 N2 — Opcije su bile mrtve na Mobile: stari [OptionChainDto] je citao
 * `entries`, ALI trading-service `OptionChainDto.java` salje odvojene
 * `calls`/`puts` liste (+ `OptionDto` koristi `optionType`/`price`/`inTheMoney`,
 * NE `type`/`premium`/`itm`). Rezultat: svaki settlement-tab je imao 0 redova.
 *
 * Ovaj test gadja STVARNI BE option-chain oblik kroz MockWebServer i validira
 * da se `calls`/`puts` parsiraju i da derived `entries` view nije prazan.
 *
 * Pre fix-a: `entries` u JSON-u nema → lista entries je prazna (0 redova).
 */
class OptionRepositoryMockServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: OptionRepository

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
            .create(OptionApi::class.java)
        repo = OptionRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** Tacan BE List<OptionChainDto> oblik: `calls`/`puts`, `optionType`/`price`/`inTheMoney`. */
    private val beOptionChain = """
        [
          {
            "settlementDate": "2026-06-30",
            "currentStockPrice": 102.50,
            "calls": [
              {
                "id": 11,
                "optionType": "CALL",
                "strikePrice": 100.00,
                "price": 5.25,
                "inTheMoney": true,
                "settlementDate": "2026-06-30",
                "currentStockPrice": 102.50
              },
              {
                "id": 12,
                "optionType": "CALL",
                "strikePrice": 110.00,
                "price": 1.10,
                "inTheMoney": false,
                "settlementDate": "2026-06-30"
              }
            ],
            "puts": [
              {
                "id": 21,
                "optionType": "PUT",
                "strikePrice": 100.00,
                "price": 2.75,
                "inTheMoney": false,
                "settlementDate": "2026-06-30"
              }
            ]
          }
        ]
    """.trimIndent()

    @Test
    fun chainFor_parsesCallsAndPuts_andPairsIntoEntries() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(beOptionChain))

        val result = repo.chainFor(88L)

        // poziva /options?stockListingId=88
        val request = server.takeRequest()
        assertEquals("/options?stockListingId=88", request.path)

        assertTrue("BE oblik mora da se parsira, got=$result", result is ApiResult.Success)
        result as ApiResult.Success
        assertEquals(1, result.data.size)

        val chain = result.data[0]
        assertEquals(2, chain.calls.size)
        assertEquals(1, chain.puts.size)
        assertEquals(BigDecimal("102.50"), chain.currentStockPrice)

        // optionType -> type, price -> premium, inTheMoney -> itm
        val firstCall = chain.calls[0]
        assertEquals("CALL", firstCall.type)
        assertEquals(BigDecimal("5.25"), firstCall.premium)
        assertEquals(true, firstCall.itm)

        // derived `entries` view NIJE prazan (pre fix-a bi bio 0 redova)
        val entries = chain.entries
        assertTrue("entries ne sme da bude prazan", entries.isNotEmpty())
        // strike 100 ima i CALL i PUT spojene u jedan red; strike 110 ima samo CALL
        assertEquals(2, entries.size)

        val strike100 = entries.first { it.strikePrice.compareTo(BigDecimal("100.00")) == 0 }
        assertNotNull("strike 100 mora imati CALL", strike100.call)
        assertNotNull("strike 100 mora imati PUT", strike100.put)
        assertEquals(11L, strike100.call!!.id)
        assertEquals(21L, strike100.put!!.id)

        val strike110 = entries.first { it.strikePrice.compareTo(BigDecimal("110.00")) == 0 }
        assertNotNull("strike 110 mora imati CALL", strike110.call)
        assertEquals(null, strike110.put)
    }

    @Test
    fun chainFor_entriesSortedByStrikeAscending() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(beOptionChain))

        val result = repo.chainFor(88L)
        result as ApiResult.Success
        val strikes = result.data[0].entries.map { it.strikePrice }
        assertEquals(listOf(BigDecimal("100.00"), BigDecimal("110.00")), strikes)
    }
}
