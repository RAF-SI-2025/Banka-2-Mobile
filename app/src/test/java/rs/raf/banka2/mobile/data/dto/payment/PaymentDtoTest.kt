package rs.raf.banka2.mobile.data.dto.payment

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * ME-11: BigDecimal migracija za Payment DTO familiju (spec C2 §255).
 */
class PaymentDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun createPaymentRequest_serializes_bigdecimal_amount() {
        val adapter = moshi.adapter(CreatePaymentRequestDto::class.java)
        val req = CreatePaymentRequestDto(
            toAccountNumber = "265000000000000001",
            amount = BigDecimal("1500.75"),
            recipientName = "Test",
            paymentPurpose = "Racun",
            otpCode = "123456"
        )
        val json = adapter.toJson(req)
        // BigDecimal mora biti serijalizovan kao number (ne string sa quotes).
        assertEquals(true, json.contains("\"amount\":1500.75"))
    }

    @Test
    fun paymentResponse_parses_precise_amount_and_fee() {
        val adapter = moshi.adapter(PaymentResponseDto::class.java)
        val json = """
            {
              "id": 42,
              "amount": 9999999.99,
              "fee": 250.50,
              "currency": "RSD",
              "status": "COMPLETED"
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(BigDecimal("9999999.99"), dto!!.amount)
        assertEquals(BigDecimal("250.50"), dto.fee)
    }

    @Test
    fun paymentListItem_default_amount_is_zero() {
        val item = PaymentListItemDto(id = 1L)
        assertEquals(BigDecimal.ZERO, item.amount)
    }

    @Test
    fun paymentListItem_arithmetic_running_balance_is_precise() {
        // Simulira HomeViewModel.balanceTrend racunanje running balance-a
        // sa Payment.amount koji su sad BigDecimal — ne sme biti 0.1 + 0.2 bug.
        val p1 = PaymentListItemDto(id = 1L, amount = BigDecimal("0.10"), direction = "OUTGOING")
        val p2 = PaymentListItemDto(id = 2L, amount = BigDecimal("0.20"), direction = "OUTGOING")
        val initial = BigDecimal("100.00")
        val afterP1 = initial + p1.amount
        val afterP2 = afterP1 + p2.amount
        assertEquals(BigDecimal("100.30"), afterP2)
    }

    @Test
    fun paymentResponse_fee_nullable() {
        val adapter = moshi.adapter(PaymentResponseDto::class.java)
        val json = """{ "id": 5, "amount": 100.00 }"""
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals(null, dto!!.fee)
        assertEquals(BigDecimal("100.00"), dto.amount)
    }
}
