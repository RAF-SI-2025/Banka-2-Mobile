package rs.raf.banka2.mobile.data.dto.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TODO_final C2 #4 — NotificationType mapa + normalizacija test.
 */
class NotificationTypeTest {

    @Test
    fun normalize_knownValue_passesThrough() {
        assertEquals(NotificationType.PAYMENT_RECEIVED, NotificationType.normalize("PAYMENT_RECEIVED"))
        assertEquals(NotificationType.ORDER_EXECUTED, NotificationType.normalize("ORDER_EXECUTED"))
    }

    @Test
    fun normalize_unknownValue_returnsGeneric() {
        assertEquals(NotificationType.GENERIC, NotificationType.normalize("SOMETHING_NEW"))
    }

    @Test
    fun normalize_null_returnsGeneric() {
        assertEquals(NotificationType.GENERIC, NotificationType.normalize(null))
    }

    @Test
    fun labelOf_knownType_returnsSerbian() {
        assertEquals("Primljeno placanje", NotificationType.labelOf("PAYMENT_RECEIVED"))
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
        // Svaka const vrednost u NotificationType (sem UNKNOWN sentinel-a) mora
        // imati labelu u LABEL_SR.
        val sentinels = setOf(NotificationType.UNKNOWN)
        for (type in NotificationType.ALL - sentinels) {
            assertTrue(
                "Tip $type bez SR labele",
                NotificationType.LABEL_SR.containsKey(type)
            )
        }
    }

    // ─── DeepLink resolver ────────────────────────────────────────────────

    @Test
    fun deepLink_paymentPendingApproval_resolvesToQuickApprove() {
        val n = NotificationDto(
            id = 99L,
            type = "PAYMENT_PENDING_APPROVAL",
            title = "Placanje ceka odobrenje",
            message = "100 USD ka 222...",
            read = false,
            createdAt = "2026-05-26T10:00:00Z",
            relatedEntityType = "PAYMENT",
            relatedEntityId = 555L,
        )
        val result = rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        assertTrue(
            "Expected QuickApprovePayment, got=$result",
            result is rs.raf.banka2.mobile.feature.notifications.NotificationTarget.QuickApprovePayment
        )
        result as rs.raf.banka2.mobile.feature.notifications.NotificationTarget.QuickApprovePayment
        assertEquals(555L, result.paymentId)
    }

    @Test
    fun deepLink_paymentEntity_resolvesToPayments() {
        val n = sampleNotification(type = "PAYMENT_RECEIVED", entity = "PAYMENT", id = 7L)
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
        val n = sampleNotification(type = "OTC_OFFER_RECEIVED", entity = "OTC_OFFER", id = 3L)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.Otc,
            rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        )
    }

    @Test
    fun deepLink_fundEntityWithId_resolvesToFundDetail() {
        val n = sampleNotification(type = "FUND_INTEREST_PAID", entity = "FUND", id = 42L)
        val result = rs.raf.banka2.mobile.feature.notifications.NotificationDeepLink.resolve(n)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.Fund(42L),
            result
        )
    }

    @Test
    fun deepLink_fundEntityNullId_resolvesToFundsList() {
        val n = sampleNotification(type = "FUND_INTEREST_PAID", entity = "FUND", id = null)
        assertEquals(
            rs.raf.banka2.mobile.feature.notifications.NotificationTarget.Funds,
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
