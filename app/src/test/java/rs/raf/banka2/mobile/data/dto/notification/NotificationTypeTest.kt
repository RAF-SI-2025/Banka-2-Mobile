package rs.raf.banka2.mobile.data.dto.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TODO_final C2 #4 — NotificationType mapa + normalizacija test.
 *
 * P1-mobile-banking-1 (R1-151): vrednosti uskladjene 1:1 sa BE NotificationType enum-om
 * (`PAYMENT`/`ORDER_*`/`OTC_*`/`LOAN_*`/`CARD_*`/... — NE izmisljena imena). Fallback je
 * `GENERAL` (BE ime), ne vise `GENERIC`. Quick Approve deep-link special-case je uklonjen
 * (BE nema `PAYMENT_PENDING_APPROVAL` tip).
 */
class NotificationTypeTest {

    @Test
    fun normalize_knownValue_passesThrough() {
        assertEquals(NotificationType.PAYMENT, NotificationType.normalize("PAYMENT"))
        assertEquals(NotificationType.ORDER_EXECUTED, NotificationType.normalize("ORDER_EXECUTED"))
    }

    @Test
    fun normalize_unknownValue_returnsGeneral() {
        assertEquals(NotificationType.GENERAL, NotificationType.normalize("SOMETHING_NEW"))
        // Izmisljena imena koja su nekad bila u Mobile spisku, a NEMA ih u BE enum-u.
        assertEquals(NotificationType.GENERAL, NotificationType.normalize("PAYMENT_RECEIVED"))
        assertEquals(NotificationType.GENERAL, NotificationType.normalize("ORDER_FILLED"))
    }

    @Test
    fun normalize_null_returnsGeneral() {
        assertEquals(NotificationType.GENERAL, NotificationType.normalize(null))
    }

    @Test
    fun labelOf_knownType_returnsSerbian() {
        assertEquals("Placanje", NotificationType.labelOf("PAYMENT"))
        assertEquals("Order izvrsen", NotificationType.labelOf("ORDER_EXECUTED"))
        assertEquals("Cenovni alarm aktiviran", NotificationType.labelOf("PRICE_ALERT_TRIGGERED"))
    }

    @Test
    fun labelOf_unknownType_returnsFallback() {
        assertEquals("Obavestenje", NotificationType.labelOf("UNKNOWN_BACKEND_VALUE"))
    }

    @Test
    fun labelOf_null_returnsFallback() {
        assertEquals("Obavestenje", NotificationType.labelOf(null))
    }

    @Test
    fun allLabelsCovered() {
        // Svaka const vrednost u NotificationType.ALL mora imati labelu u LABEL_SR.
        for (type in NotificationType.ALL) {
            assertTrue(
                "Tip $type bez SR labele",
                NotificationType.LABEL_SR.containsKey(type)
            )
        }
    }

    // ─── DeepLink resolver ────────────────────────────────────────────────

    @Test
    fun deepLink_paymentEntity_resolvesToPayments() {
        val n = sampleNotification(type = "PAYMENT", entity = "PAYMENT", id = 7L)
        val result = rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.Payments,
            result
        )
    }

    @Test
    fun deepLink_orderEntity_resolvesToOrders() {
        val n = sampleNotification(type = "ORDER_EXECUTED", entity = "ORDER", id = 12L)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.Orders,
            rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        )
    }

    @Test
    fun deepLink_otcOffer_resolvesToOtc() {
        val n = sampleNotification(type = "OTC_COUNTER_OFFER", entity = "OTC_OFFER", id = 3L)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.Otc,
            rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        )
    }

    @Test
    fun deepLink_fundEntityWithId_resolvesToFundDetail() {
        val n = sampleNotification(type = "GENERAL", entity = "FUND", id = 42L)
        val result = rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.Fund(42L),
            result
        )
    }

    @Test
    fun deepLink_fundEntityNullId_resolvesToFundsList() {
        val n = sampleNotification(type = "GENERAL", entity = "FUND", id = null)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.Funds,
            rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        )
    }

    @Test
    fun deepLink_recurringOrderEntity_resolvesToRecurringOrders() {
        // R1 689: BE emituje referenceType=RECURRING_ORDER (RecurringOrderScheduler).
        val n = sampleNotification(type = "RECURRING_ORDER_SKIPPED", entity = "RECURRING_ORDER", id = 9L)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.RecurringOrders,
            rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        )
    }

    @Test
    fun deepLink_priceAlertEntity_resolvesToPriceAlerts() {
        // R1 689: BE emituje referenceType=PRICE_ALERT (PriceAlertService).
        val n = sampleNotification(type = "PRICE_ALERT_TRIGGERED", entity = "PRICE_ALERT", id = 5L)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.PriceAlerts,
            rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        )
    }

    @Test
    fun deepLink_loanRequestEntity_resolvesToLoans() {
        val n = sampleNotification(type = "LOAN_CREATED", entity = "LOAN_REQUEST", id = 2L)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.Loans,
            rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        )
    }

    @Test
    fun deepLink_unknownEntity_returnsNull() {
        val n = sampleNotification(type = "GENERAL", entity = "RANDOM", id = 1L)
        val result = rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        assertEquals(null, result)
    }

    @Test
    fun deepLink_nullEntity_returnsNull() {
        val n = sampleNotification(type = "GENERAL", entity = null, id = null)
        val result = rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        assertEquals(null, result)
    }

    private fun sampleNotification(type: String, entity: String?, id: Long?): NotificationDto =
        NotificationDto(
            id = 1L,
            type = type,
            title = "T",
            message = "M",
            read = false,
            createdAt = "2026-05-26T10:00:00Z",
            relatedEntityType = entity,
            relatedEntityId = id,
        )
}
