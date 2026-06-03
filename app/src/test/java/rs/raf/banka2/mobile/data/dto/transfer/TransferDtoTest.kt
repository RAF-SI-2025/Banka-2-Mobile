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
    fun internalRequest_serializes_be_account_numbers_and_amount() {
        // P1-mobile-banking-1 (R1-125): BE TransferInternalRequestDto trazi
        // fromAccountNumber/toAccountNumber (NE *Id) — inace 400.
        val adapter = moshi.adapter(TransferInternalRequestDto::class.java)
        val req = TransferInternalRequestDto(
            fromAccountNumber = "222-AAA",
            toAccountNumber = "222-BBB",
            amount = BigDecimal("500.25"),
            otpCode = "123456"
        )
        val json = adapter.toJson(req)
        assertEquals(true, json.contains("\"fromAccountNumber\":\"222-AAA\""))
        assertEquals(true, json.contains("\"toAccountNumber\":\"222-BBB\""))
        assertEquals(true, json.contains("\"amount\":500.25"))
    }

    @Test
    fun fxRequest_serializes_be_account_numbers_no_currency() {
        // R1-125: BE TransferFxRequestDto ima istu strukturu kao internal — NEMA currency.
        val adapter = moshi.adapter(TransferFxRequestDto::class.java)
        val req = TransferFxRequestDto(
            fromAccountNumber = "222-EUR",
            toAccountNumber = "222-RSD",
            amount = BigDecimal("1234.5678"),
            otpCode = "987654"
        )
        val json = adapter.toJson(req)
        assertEquals(BigDecimal("1234.5678"), req.amount)
        assertEquals(true, json.contains("\"fromAccountNumber\":\"222-EUR\""))
        assertEquals(false, json.contains("\"currency\""))
    }

    @Test
    fun response_maps_be_field_names_to_kotlin_aliases() {
        // P1-mobile-banking-1 (R1-126): BE salje fromAccountNumber/toAccountNumber/
        // toAmount/fromCurrency/exchangeRate/commission — Kotlin polja
        // fromAccount/toAccount/convertedAmount/currency/rate/fee se vezuju aliasima.
        val adapter = moshi.adapter(TransferResponseDto::class.java)
        val json = """
            {
              "id": 7,
              "fromAccountNumber": "222-EUR",
              "toAccountNumber": "222-RSD",
              "amount": 1000.00,
              "toAmount": 8523.45,
              "exchangeRate": 117.2345,
              "commission": 25.00,
              "fromCurrency": "EUR",
              "toCurrency": "RSD",
              "status": "COMPLETED"
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals("222-EUR", dto!!.fromAccount)
        assertEquals("222-RSD", dto.toAccount)
        assertEquals(BigDecimal("1000.00"), dto.amount)
        assertEquals(BigDecimal("8523.45"), dto.convertedAmount)
        assertEquals(BigDecimal("25.00"), dto.fee)
        assertEquals("EUR", dto.currency)
        assertEquals("RSD", dto.convertedCurrency)
        assertEquals(BigDecimal("117.2345"), dto.rate)
    }

    @Test
    fun transfer_response_defaults_zero_amount() {
        val dto = TransferResponseDto(id = 1L)
        assertEquals(BigDecimal.ZERO, dto.amount)
        assertEquals(null, dto.fee)
        assertEquals(null, dto.convertedAmount)
    }
}
