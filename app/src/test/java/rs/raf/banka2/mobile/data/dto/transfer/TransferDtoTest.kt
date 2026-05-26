package rs.raf.banka2.mobile.data.dto.transfer

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * ME-11: BigDecimal migracija za Transfer DTO familiju (spec C2 §255).
 * `rate` ostaje Double jer je FX koeficijent (ne novac).
 */
class TransferDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun internalRequest_serializes_bigdecimal_amount() {
        val adapter = moshi.adapter(TransferInternalRequestDto::class.java)
        val req = TransferInternalRequestDto(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = BigDecimal("500.25"),
            otpCode = "123456"
        )
        val json = adapter.toJson(req)
        assertEquals(true, json.contains("\"amount\":500.25"))
    }

    @Test
    fun fxRequest_preserves_decimal_precision() {
        val req = TransferFxRequestDto(
            fromAccountId = 1L,
            amount = BigDecimal("1234.5678"),
            currency = "EUR",
            otpCode = "987654"
        )
        assertEquals(BigDecimal("1234.5678"), req.amount)
    }

    @Test
    fun response_parses_amount_fee_convertedAmount_as_bigdecimal_keeps_rate_as_double() {
        val adapter = moshi.adapter(TransferResponseDto::class.java)
        val json = """
            {
              "id": 7,
              "amount": 1000.00,
              "convertedAmount": 8523.45,
              "rate": 117.2345,
              "fee": 25.00,
              "currency": "EUR",
              "convertedCurrency": "RSD",
              "status": "COMPLETED"
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        // Money fields → BigDecimal
        assertEquals(BigDecimal("1000.00"), dto!!.amount)
        assertEquals(BigDecimal("8523.45"), dto.convertedAmount)
        assertEquals(BigDecimal("25.00"), dto.fee)
        // FX rate ostaje Double
        assertEquals(117.2345, dto.rate!!, 0.0001)
    }

    @Test
    fun transfer_response_defaults_zero_amount() {
        val dto = TransferResponseDto(id = 1L)
        assertEquals(BigDecimal.ZERO, dto.amount)
        assertEquals(null, dto.fee)
        assertEquals(null, dto.convertedAmount)
    }
}
