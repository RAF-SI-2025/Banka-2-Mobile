package rs.raf.banka2.mobile.data.dto.tax

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * P1-mobile-banking-1 (R1-176): TaxRecordDto kontrakt protiv STVARNOG BE oblika
 * (`rs.raf.trading.tax.dto.TaxRecordDto`: userName/totalProfit/taxOwed/taxPaid).
 * Pre fix-a Mobile je citao name/totalGain/taxAmount/paidThisYear → tax portal prazan.
 */
class TaxDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun taxRecord_mapsBeFieldNames() {
        val adapter = moshi.adapter(TaxRecordDto::class.java)
        val json = """
            {
              "id": 3,
              "userId": 42,
              "userName": "Pera Peric",
              "userType": "CLIENT",
              "totalProfit": 12500.00,
              "taxOwed": 1875.00,
              "taxPaid": 500.00,
              "currency": "RSD"
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(42L, dto!!.userId)
        assertEquals("Pera Peric", dto.name)
        assertEquals(BigDecimal("12500.00"), dto.totalGain)
        assertEquals(BigDecimal("1875.00"), dto.taxAmount)
        assertEquals(BigDecimal("500.00"), dto.paidThisYear)
        assertEquals("RSD", dto.currency)
    }
}
