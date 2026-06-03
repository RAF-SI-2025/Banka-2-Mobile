package rs.raf.banka2.mobile.data.dto.fund

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * P1-mobile-trading-1 (R1-223/224): Fund DTO kontrakt protiv BE
 * `rs.raf.trading.investmentfund.dto.InvestmentFundDtos`.
 *
 *  - Summary/Detail cita `fundValue`/`liquidAmount`/`managerEmployeeId`/
 *    `inceptionDate` (ranije totalValue/liquidFunds/managerId/createdAt → 0/null).
 *  - InvestFundDto serializuje `currency` (BE @NotBlank — ranije izostavljen → 400).
 */
class FundDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun fundSummary_readsBeFundValue() {
        val adapter = moshi.adapter(FundSummaryDto::class.java)
        val json = """
            {
              "id": 1,
              "name": "Alfa",
              "minimumContribution": 1000.00,
              "fundValue": 250000.50,
              "profit": 5000.00,
              "managerName": "Mika",
              "inceptionDate": "2026-01-01"
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(BigDecimal("250000.50"), dto!!.totalValue)
        assertEquals(BigDecimal("5000.00"), dto.profit)
        assertEquals("2026-01-01", dto.createdAt)
    }

    @Test
    fun fundDetail_readsBeLiquidAmountAndManagerId() {
        val adapter = moshi.adapter(FundDetailDto::class.java)
        val json = """
            {
              "id": 1,
              "name": "Alfa",
              "managerName": "Mika",
              "managerEmployeeId": 11,
              "fundValue": 250000.00,
              "liquidAmount": 50000.00,
              "profit": 1000.00,
              "minimumContribution": 1000.00,
              "accountNumber": "222-FUND-1"
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(BigDecimal("250000.00"), dto!!.totalValue)
        assertEquals(BigDecimal("50000.00"), dto.liquidFunds)
        assertEquals(11L, dto.managerId)
        assertEquals("222-FUND-1", dto.accountNumber)
    }

    @Test
    fun investFundDto_serializesCurrency() {
        val adapter = moshi.adapter(FundInvestDto::class.java)
        val json = adapter.toJson(FundInvestDto(sourceAccountId = 9L, amount = BigDecimal("1000.00"), currency = "RSD"))
        assertTrue("got=$json", json.contains("\"currency\":\"RSD\""))
        assertTrue("got=$json", json.contains("\"sourceAccountId\":9"))
    }
}
