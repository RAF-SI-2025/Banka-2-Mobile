package rs.raf.banka2.mobile.feature.pricealerts

import org.junit.Assert.assertEquals
import org.junit.Test
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertDto
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertLabels
import java.math.BigDecimal

/**
 * Testovi za PriceAlertsState filter logiku (Aktivni/Istorija/Sve).
 */
class PriceAlertsStateFilterTest {

    private fun alert(id: Long, active: Boolean) = PriceAlertDto(
        id = id, listingId = id, listingTicker = "TICK$id",
        condition = "ABOVE", threshold = BigDecimal("100"), active = active,
    )

    private val mixed = listOf(
        alert(1, active = true),
        alert(2, active = true),
        alert(3, active = false),
        alert(4, active = true),
        alert(5, active = false),
    )

    @Test
    fun filterActive_returnsOnlyActive() {
        val state = PriceAlertsState(alerts = mixed, filter = PriceAlertLabels.FilterTab.ACTIVE)
        val filtered = state.filteredAlerts
        assertEquals(3, filtered.size)
        assertEquals(listOf(1L, 2L, 4L), filtered.map { it.id })
    }

    @Test
    fun filterHistory_returnsOnlyInactive() {
        val state = PriceAlertsState(alerts = mixed, filter = PriceAlertLabels.FilterTab.HISTORY)
        val filtered = state.filteredAlerts
        assertEquals(2, filtered.size)
        assertEquals(listOf(3L, 5L), filtered.map { it.id })
    }

    @Test
    fun filterAll_returnsEverything() {
        val state = PriceAlertsState(alerts = mixed, filter = PriceAlertLabels.FilterTab.ALL)
        assertEquals(5, state.filteredAlerts.size)
    }

    @Test
    fun activeCount_correct() {
        val state = PriceAlertsState(alerts = mixed)
        assertEquals(3, state.activeCount)
    }

    @Test
    fun historyCount_correct() {
        val state = PriceAlertsState(alerts = mixed)
        assertEquals(2, state.historyCount)
    }

    @Test
    fun totalCount_correct() {
        val state = PriceAlertsState(alerts = mixed)
        assertEquals(5, state.totalCount)
    }

    @Test
    fun emptyAlerts_allCountsZero() {
        val state = PriceAlertsState(alerts = emptyList())
        assertEquals(0, state.activeCount)
        assertEquals(0, state.historyCount)
        assertEquals(0, state.totalCount)
        assertEquals(0, state.filteredAlerts.size)
    }
}
