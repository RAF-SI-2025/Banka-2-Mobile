package rs.raf.banka2.mobile.data.dto.card

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * ME-03 tests: kartice imaju cardCategory (DEBIT/CREDIT/INTERNET_PREPAID) + per-category fields.
 * Verifikuje da `isPrepaid` / `isCredit` helper getters tacno rade case-insensitive.
 */
class CardDtoTest {

    @Test
    fun debit_isPrepaid_returnsFalse() {
        val card = CardDto(id = 1L, cardCategory = "DEBIT")
        assertFalse(card.isPrepaid)
        assertFalse(card.isCredit)
    }

    @Test
    fun credit_isCredit_returnsTrue() {
        val card = CardDto(id = 1L, cardCategory = "CREDIT", creditLimit = BigDecimal("100000"))
        assertTrue(card.isCredit)
        assertFalse(card.isPrepaid)
    }

    @Test
    fun internetPrepaid_isPrepaid_returnsTrue() {
        val card = CardDto(id = 1L, cardCategory = "INTERNET_PREPAID", prepaidBalance = BigDecimal("500"))
        assertTrue(card.isPrepaid)
        assertFalse(card.isCredit)
    }

    @Test
    fun nullCardCategory_treatedAsNonPrepaid() {
        val card = CardDto(id = 1L, cardCategory = null)
        assertFalse(card.isPrepaid)
        assertFalse(card.isCredit)
    }

    @Test
    fun categoryCaseInsensitive() {
        val lower = CardDto(id = 1L, cardCategory = "internet_prepaid")
        assertTrue(lower.isPrepaid)
        val mixed = CardDto(id = 2L, cardCategory = "Credit")
        assertTrue(mixed.isCredit)
    }

    @Test
    fun cardTopUpRequest_storesFields() {
        val req = CardTopUpRequest(sourceAccountId = 5L, amount = BigDecimal("250.50"))
        assertEquals(5L, req.sourceAccountId)
        assertEquals(BigDecimal("250.50"), req.amount)
    }

    @Test
    fun cardWithdrawRequest_storesFields() {
        val req = CardWithdrawRequest(targetAccountId = 7L, amount = BigDecimal("100.00"))
        assertEquals(7L, req.targetAccountId)
        assertEquals(BigDecimal("100.00"), req.amount)
    }

    @Test
    fun cardRequestCreateDto_defaultsToDebit() {
        val req = CardRequestCreateDto(
            accountId = 1L,
            cardLimit = BigDecimal("100000"),
            cardType = "VISA"
        )
        assertEquals("DEBIT", req.cardCategory)
        assertEquals(null, req.creditLimit)
    }

    @Test
    fun cardRequestCreateDto_creditCarriesCreditLimit() {
        val req = CardRequestCreateDto(
            accountId = 1L,
            cardLimit = BigDecimal("100000"),
            cardType = "MASTERCARD",
            cardCategory = "CREDIT",
            creditLimit = BigDecimal("500000")
        )
        assertEquals("CREDIT", req.cardCategory)
        assertEquals(BigDecimal("500000"), req.creditLimit)
    }
}
