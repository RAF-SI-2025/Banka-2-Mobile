package rs.raf.banka2.mobile.feature.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.data.dto.audit.AuditActionTypes

/**
 * B7: lista action tipova mora 1:1 matchuje BE `AuditActionType` enum.
 * Label-i moraju imati srpski prevod za svaki tip.
 */
class AuditActionTypesTest {

    @Test
    fun allTypesHaveSerbianLabels() {
        AuditActionTypes.ALL.forEach { actionType ->
            val label = AuditActionTypes.LABEL_SR[actionType]
            assertTrue("Missing SR label for $actionType", !label.isNullOrBlank())
        }
    }

    @Test
    fun listSize_matchesBackendUnion() {
        // R1-594: UNIJA AuditActionType enum-a OBA servisa (banka-core + trading) = 30.
        // Paritet sa FE `types/audit.ts` (AUDIT_ACTION_TYPES).
        assertEquals(30, AuditActionTypes.ALL.size)
    }

    @Test
    fun coversBankCoreAndTradingTypes() {
        // Spot-check da nove kategorije (krediti/placanja/kartice/fondovi) postoje.
        listOf(
            "LOAN_APPROVED", "PAYMENT_CREATED", "CARD_BLOCKED",
            "SAVINGS_OPENED", "FUND_CREATED", "USED_LIMIT_RESET_ALL"
        ).forEach { type ->
            assertTrue("Missing type $type", AuditActionTypes.ALL.contains(type))
        }
    }

    @Test
    fun label_returnsSerbianStringForKnownTypes() {
        assertEquals("Order odobren", AuditActionTypes.label("ORDER_APPROVED"))
        assertEquals("Order odbijen", AuditActionTypes.label("ORDER_DECLINED"))
        assertEquals("Promena limita", AuditActionTypes.label("LIMIT_CHANGED"))
        assertEquals("Izmena permisija", AuditActionTypes.label("PERMISSIONS_CHANGED"))
    }

    @Test
    fun label_returnsRawStringForUnknownTypes() {
        // Forward-compat: ako BE doda novi tip pre nego sto Mobile bude azuriran,
        // UI prikazuje BE konstanu (npr. "FUTURE_ACTION") umesto rusenja.
        assertEquals("FUTURE_ACTION", AuditActionTypes.label("FUTURE_ACTION"))
    }

    @Test
    fun label_returnsEmDashForNull() {
        assertEquals("—", AuditActionTypes.label(null))
    }
}
