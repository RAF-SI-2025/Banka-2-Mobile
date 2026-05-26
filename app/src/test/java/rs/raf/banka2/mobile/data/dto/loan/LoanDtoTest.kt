package rs.raf.banka2.mobile.data.dto.loan

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * ME-11: BigDecimal migracija za Loan DTO familiju (spec C2 §255).
 * `interestRate` / `effectiveRate` ostaju Double (procenti, ne novac).
 */
class LoanDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun loan_parses_precise_amount_balance_installment_and_income() {
        val adapter = moshi.adapter(LoanDto::class.java)
        val json = """
            {
              "id": 1,
              "amount": 5000000.00,
              "balance": 3500000.55,
              "monthlyInstallment": 41666.75,
              "monthlyIncome": 120000.00,
              "interestRate": 4.5,
              "effectiveRate": 4.78,
              "currency": "RSD",
              "status": "APPROVED"
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        // Money fields su BigDecimal
        assertEquals(BigDecimal("5000000.00"), dto!!.amount)
        assertEquals(BigDecimal("3500000.55"), dto.balance)
        assertEquals(BigDecimal("41666.75"), dto.monthlyInstallment)
        assertEquals(BigDecimal("120000.00"), dto.monthlyIncome)
        // Interest rate ostaje Double
        assertEquals(4.5, dto.interestRate!!, 0.001)
        assertEquals(4.78, dto.effectiveRate!!, 0.001)
    }

    @Test
    fun loan_installment_amount_is_bigdecimal_with_precision() {
        val inst = LoanInstallmentDto(
            id = 1L,
            dueDate = "2026-06-15",
            amount = BigDecimal("41666.67")
        )
        assertEquals(BigDecimal("41666.67"), inst.amount)
    }

    @Test
    fun loan_application_request_carries_bigdecimal_amount_and_income() {
        val req = LoanApplicationDto(
            loanType = "CASH",
            amount = BigDecimal("1500000.00"),
            durationMonths = 36,
            purpose = "Renoviranje",
            monthlyIncome = BigDecimal("85000.50"),
            otpCode = "123456"
        )
        assertEquals(BigDecimal("1500000.00"), req.amount)
        assertEquals(BigDecimal("85000.50"), req.monthlyIncome)
    }

    @Test
    fun loan_application_response_amount_nullable() {
        val dto = LoanApplicationResponseDto(id = 1L)
        assertNull(dto.amount)
    }

    @Test
    fun loan_installment_default_zero_amount() {
        val inst = LoanInstallmentDto(id = 1L)
        assertEquals(BigDecimal.ZERO, inst.amount)
    }

    @Test
    fun bigdecimal_installment_arithmetic_is_precise() {
        // Spec: ako je glavnica 100.00 i 12 mesecnih rata pune kamate 5%,
        // svaka rata mora biti tacno proracunata bez Double round-off-a.
        val installments = listOf(
            LoanInstallmentDto(id = 1L, amount = BigDecimal("41666.67")),
            LoanInstallmentDto(id = 2L, amount = BigDecimal("41666.67")),
            LoanInstallmentDto(id = 3L, amount = BigDecimal("41666.66"))
        )
        val total = installments.fold(BigDecimal.ZERO) { acc, i -> acc + i.amount }
        assertEquals(BigDecimal("125000.00"), total)
    }
}
