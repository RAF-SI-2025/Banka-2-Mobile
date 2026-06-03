package rs.raf.banka2.mobile.data.dto.profitbank

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * P1-mobile-trading-1 (R1-230/231): ProfitBank DTO kontrakt protiv BE
 * `rs.raf.trading.profitbank.dto.ProfitBankDtos`.
 *
 *  - ActuaryProfitDto cita `name`/`totalProfitRsd`/`ordersDone` (ranije
 *    firstName/lastName/realizedProfitRsd → profit 0, ime prazno).
 *  - BankFundPositionDto cita `percentShare`/`rsdValue`/`profitRsd` (ranije
 *    sharePercent/shareAmountRsd → prazno/0).
 */
class ProfitBankDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun actuaryProfit_readsBeFieldNames() {
        val adapter = moshi.adapter(ActuaryProfitDto::class.java)
        val json = """
            {
              "employeeId": 11,
              "name": "Mika Mikic",
              "position": "AGENT",
              "totalProfitRsd": 125000.50,
              "ordersDone": 7
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals("Mika Mikic", dto!!.name)
        assertEquals("Mika Mikic", dto.displayName)
        assertEquals(BigDecimal("125000.50"), dto.realizedProfitRsd)
        assertEquals(7, dto.ordersDone)
    }

    @Test
    fun bankFundPosition_readsBeFieldNames() {
        val adapter = moshi.adapter(BankFundPositionDto::class.java)
        val json = """
            {
              "fundId": 3,
              "fundName": "Alfa",
              "managerName": "Mika",
              "percentShare": 12.5,
              "rsdValue": 500000.00,
              "profitRsd": 25000.00
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(12.5, dto!!.sharePercent!!, 0.0001)
        assertEquals(BigDecimal("500000.00"), dto.shareAmountRsd)
        assertEquals(BigDecimal("25000.00"), dto.profitRsd)
    }
}
