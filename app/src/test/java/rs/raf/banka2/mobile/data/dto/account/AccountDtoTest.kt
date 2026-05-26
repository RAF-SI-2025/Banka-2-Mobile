package rs.raf.banka2.mobile.data.dto.account

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.core.network.BigDecimalAdapter
import java.math.BigDecimal

/**
 * ME-11: BigDecimal migracija za AccountDto familiju. Verifikuje da:
 *  1. Moshi nativno parsuje BigDecimal iz JSON number-a sa preciznoscu (no
 *     Double round-off — npr 12345678901234.56 ostaje tacan).
 *  2. `effectiveReserved` helper koristi `reservedAmount` polje ako postoji,
 *     a fallback na (balance - availableBalance) ako nije postavljeno.
 *  3. Aritmetika na BigDecimal-u je precizna (no 0.1 + 0.2 = 0.30000000000000004 bug).
 */
class AccountDtoTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(AccountDto::class.java)

    @Test
    fun parses_precise_balance_from_json_without_double_roundoff() {
        // BigDecimal mora ocuvati 14 cifara — Double bi izgubio precision na poslednjim.
        val json = """
            {
              "id": 1,
              "accountNumber": "265000000000000001",
              "currency": "RSD",
              "balance": 12345678901234.56,
              "availableBalance": 12000000000000.00,
              "reservedAmount": 345678901234.56
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        // Verifikujemo preciznost (Double bi pao na 12345678901234.559 ili slicno).
        assertEquals(BigDecimal("12345678901234.56"), dto!!.balance)
        assertEquals(BigDecimal("12000000000000.00"), dto.availableBalance)
        assertEquals(BigDecimal("345678901234.56"), dto.reservedAmount)
    }

    @Test
    fun effectiveReserved_prefers_reservedAmount_field() {
        val dto = AccountDto(
            id = 1L,
            accountNumber = "ACC",
            balance = BigDecimal("1000.00"),
            availableBalance = BigDecimal("800.00"),
            reservedAmount = BigDecimal("150.00") // ne 200 (1000-800), nego eksplicitno 150
        )
        // BE explicit field beats derived
        assertEquals(BigDecimal("150.00"), dto.effectiveReserved)
    }

    @Test
    fun effectiveReserved_fallback_to_balance_minus_available() {
        val dto = AccountDto(
            id = 1L,
            accountNumber = "ACC",
            balance = BigDecimal("1000.00"),
            availableBalance = BigDecimal("800.00")
        )
        assertEquals(BigDecimal("200.00"), dto.effectiveReserved)
    }

    @Test
    fun bigDecimal_arithmetic_avoids_double_roundoff_bug() {
        // Klasican Double bug: 0.1 + 0.2 = 0.30000000000000004
        // Sa BigDecimal: 0.1 + 0.2 = 0.3 (egzaktno)
        val a = AccountDto(
            id = 1L, accountNumber = "A",
            balance = BigDecimal("0.10"),
            availableBalance = BigDecimal.ZERO
        )
        val b = AccountDto(
            id = 2L, accountNumber = "B",
            balance = BigDecimal("0.20"),
            availableBalance = BigDecimal.ZERO
        )
        assertEquals(BigDecimal("0.30"), a.balance + b.balance)
    }

    @Test
    fun limits_dto_holds_precise_bigdecimal_values() {
        val dto = AccountLimitsUpdateDto(
            dailyLimit = BigDecimal("50000.55"),
            monthlyLimit = BigDecimal("1500000.99"),
            otpCode = "123456"
        )
        assertEquals(BigDecimal("50000.55"), dto.dailyLimit)
        assertEquals(BigDecimal("1500000.99"), dto.monthlyLimit)
    }

    @Test
    fun create_account_dto_default_initial_deposit_is_zero() {
        val dto = CreateAccountDto(
            accountType = "CHECKING",
            currency = "RSD",
            ownerEmail = "x@y.rs"
        )
        assertEquals(BigDecimal.ZERO, dto.initialDeposit)
    }

    @Test
    fun account_request_dto_preserves_decimal_precision() {
        val dto = AccountRequestDto(
            accountType = "CHECKING",
            currency = "EUR",
            initialDeposit = BigDecimal("99.99")
        )
        assertEquals(BigDecimal("99.99"), dto.initialDeposit)
    }

    @Test
    fun account_request_response_initial_deposit_nullable() {
        val dto = AccountRequestResponseDto(id = 1L)
        assertNull(dto.initialDeposit)
    }

    @Test
    fun zero_balance_isBusiness_false_for_personal() {
        val dto = AccountDto(
            id = 1L, accountNumber = "A",
            balance = BigDecimal.ZERO,
            availableBalance = BigDecimal.ZERO,
            accountType = "CHECKING"
        )
        assertTrue(!dto.isBusiness)
    }
}
