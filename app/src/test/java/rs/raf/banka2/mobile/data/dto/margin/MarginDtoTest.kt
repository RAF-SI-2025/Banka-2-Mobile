package rs.raf.banka2.mobile.data.dto.margin

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * P1-mobile-trading-1 (R1-202/269/270): Margin DTO kontrakt protiv BE
 * `rs.raf.trading.margin.dto.CreateMarginAccountDto` + deposit/withdraw response.
 *
 *  - CreateMarginAccountDto MORA slati `accountId` (BE @NotNull) — ranije izostavljen → 400.
 *  - deposit/withdraw vracaju `{"message":...}` (Map), NE MarginAccountDto.
 */
class MarginDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun createMargin_serializesAccountId() {
        val adapter = moshi.adapter(CreateMarginAccountDto::class.java)
        val json = adapter.toJson(
            CreateMarginAccountDto(
                accountId = 17L,
                initialMargin = BigDecimal("100000.00"),
                maintenanceMargin = BigDecimal("50000.00"),
                bankParticipation = BigDecimal("0.5")
            )
        )
        assertTrue("got=$json", json.contains("\"accountId\":17"))
        assertTrue("got=$json", json.contains("\"initialMargin\":100000.00"))
    }

    @Test
    fun marginMessage_parsesMessageBody() {
        val adapter = moshi.adapter(MarginMessageDto::class.java)
        val dto = adapter.fromJson("""{"message":"Deposit successful"}""")
        assertNotNull(dto)
        assertEquals("Deposit successful", dto!!.message)
    }
}
