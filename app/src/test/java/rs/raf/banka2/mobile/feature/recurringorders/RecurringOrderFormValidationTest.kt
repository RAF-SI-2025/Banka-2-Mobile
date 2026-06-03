package rs.raf.banka2.mobile.feature.recurringorders

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.data.dto.listing.ListingDto
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringCadence
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringDirection
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringMode
import java.math.BigDecimal

/**
 * Unit testovi za pure RecurringOrdersViewModel.validateForm() funkciju.
 */
class RecurringOrderFormValidationTest {

    private val listing = ListingDto(
        id = 1L, ticker = "AAPL", name = "Apple Inc.", listingType = "STOCK",
        currency = "USD", price = BigDecimal("180.0"),
    )

    @Test
    fun validate_emptyForm_returnsListingError() {
        val form = RecurringOrderForm()
        val result = RecurringOrdersViewModel.validateForm(form)
        assertNotNull(result)
        assertTrue(result!!.contains("hartiju"))
    }

    @Test
    fun validate_listingSelectedButNoValue_returnsValueError() {
        val form = RecurringOrderForm(selectedListing = listing, value = null)
        val result = RecurringOrdersViewModel.validateForm(form)
        assertNotNull(result)
        assertTrue(result!!.contains("Vrednost"))
    }

    @Test
    fun validate_negativeValue_returnsValueError() {
        val form = RecurringOrderForm(selectedListing = listing, value = BigDecimal("-1"))
        val result = RecurringOrdersViewModel.validateForm(form)
        assertNotNull(result)
    }

    @Test
    fun validate_zeroValue_returnsValueError() {
        val form = RecurringOrderForm(selectedListing = listing, value = BigDecimal.ZERO)
        val result = RecurringOrdersViewModel.validateForm(form)
        assertNotNull(result)
    }

    @Test
    fun validate_byQuantityFractionalValue_returnsError() {
        // 3.5 akcija nema smisla — BY_QUANTITY ocekuje ceo broj.
        val form = RecurringOrderForm(
            selectedListing = listing,
            mode = RecurringMode.BY_QUANTITY,
            value = BigDecimal("3.5"),
            accountId = 1L,
        )
        val result = RecurringOrdersViewModel.validateForm(form)
        assertNotNull(result)
        assertTrue(result!!.contains("ceo broj"))
    }

    @Test
    fun validate_byQuantityIntegerValue_returnsNull() {
        val form = RecurringOrderForm(
            selectedListing = listing,
            mode = RecurringMode.BY_QUANTITY,
            value = BigDecimal("3"),
            accountId = 1L,
        )
        assertNull(RecurringOrdersViewModel.validateForm(form))
    }

    @Test
    fun validate_byAmountFractionalValue_returnsNull() {
        // Po iznosu su decimale dozvoljene (npr. 100.50 USD).
        val form = RecurringOrderForm(
            selectedListing = listing,
            mode = RecurringMode.BY_AMOUNT,
            value = BigDecimal("100.50"),
            accountId = 1L,
        )
        assertNull(RecurringOrdersViewModel.validateForm(form))
    }

    @Test
    fun validate_missingAccountId_returnsAccountError() {
        val form = RecurringOrderForm(
            selectedListing = listing,
            value = BigDecimal("100"),
            accountId = null,
        )
        val result = RecurringOrdersViewModel.validateForm(form)
        assertNotNull(result)
        assertTrue(result!!.contains("racun"))
    }

    @Test
    fun validate_completeBuyByAmountForm_returnsNull() {
        val form = RecurringOrderForm(
            selectedListing = listing,
            direction = RecurringDirection.BUY,
            mode = RecurringMode.BY_AMOUNT,
            value = BigDecimal("100"),
            accountId = 5L,
            cadence = RecurringCadence.MONTHLY,
        )
        assertNull(RecurringOrdersViewModel.validateForm(form))
    }

    @Test
    fun validate_completeSellByQuantityForm_returnsNull() {
        val form = RecurringOrderForm(
            selectedListing = listing,
            direction = RecurringDirection.SELL,
            mode = RecurringMode.BY_QUANTITY,
            value = BigDecimal("5"),
            accountId = 5L,
            cadence = RecurringCadence.WEEKLY,
        )
        assertNull(RecurringOrdersViewModel.validateForm(form))
    }
}
