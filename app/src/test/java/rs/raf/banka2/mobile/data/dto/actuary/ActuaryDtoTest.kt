package rs.raf.banka2.mobile.data.dto.actuary

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * P1-mobile-banking-1 (R1-185): ActuaryDto kontrakt protiv STVARNOG BE oblika
 * (`rs.raf.trading.actuary.dto.ActuaryInfoDto`:
 * employeeName/employeeEmail/employeePosition). Pre fix-a Mobile je citao
 * firstName/lastName/email/position → ekran Aktuari je bio prazan.
 */
class ActuaryDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun actuary_mapsBeFieldNames_andDisplayName() {
        val adapter = moshi.adapter(ActuaryDto::class.java)
        val json = """
            {
              "id": 9,
              "employeeId": 11,
              "employeeName": "Mika Mikic",
              "employeeEmail": "mika@banka.rs",
              "employeePosition": "Agent",
              "actuaryType": "AGENT",
              "dailyLimit": 100000.00,
              "usedLimit": 25000.00,
              "needApproval": true
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(11L, dto!!.employeeId)
        assertEquals("Mika Mikic", dto.name)
        assertEquals("mika@banka.rs", dto.email)
        assertEquals("Agent", dto.position)
        assertEquals(BigDecimal("100000.00"), dto.dailyLimit)
        assertEquals(BigDecimal("25000.00"), dto.usedLimit)
        assertEquals(true, dto.needApproval)
        // displayName preferira ime, fallback email
        assertEquals("Mika Mikic", dto.displayName)
    }

    @Test
    fun displayName_fallsBackToEmail_whenNameMissing() {
        val dto = ActuaryDto(employeeId = 1L, email = "x@banka.rs")
        assertEquals("x@banka.rs", dto.displayName)
    }
}
