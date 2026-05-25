package rs.raf.banka2.mobile.data.dto.audit

import com.squareup.moshi.JsonClass

/**
 * Audit log zapis sa svih BE polja (B7 / spec Celina 3 §69).
 * Vraca `GET /audit-logs` paginirano sa Spring `Page` omotacem.
 *
 * `oldValue`/`newValue` mogu biti free-form JSON stringovi (BE moze cuvati
 * razne vrednosti — primer: stara/nova limita, lista permisija, razlog).
 */
@JsonClass(generateAdapter = true)
data class AuditLogDto(
    val id: Long,
    val actionType: String, // LIMIT_CHANGED / USED_LIMIT_RESET / ORDER_APPROVED / ORDER_DECLINED / PERMISSIONS_CHANGED / TAX_RUN_TRIGGERED
    val actorId: Long? = null,
    val actorType: String? = null,
    val actorEmail: String? = null,
    val actorName: String? = null,
    val targetType: String? = null,
    val targetId: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val metadata: String? = null,
    val createdAt: String
)

/**
 * Lista podrzanih audit action tipova — koristi se za render filter dropdown-a.
 */
object AuditActionTypes {
    const val LIMIT_CHANGED = "LIMIT_CHANGED"
    const val USED_LIMIT_RESET = "USED_LIMIT_RESET"
    const val ORDER_APPROVED = "ORDER_APPROVED"
    const val ORDER_DECLINED = "ORDER_DECLINED"
    const val PERMISSIONS_CHANGED = "PERMISSIONS_CHANGED"
    const val TAX_RUN_TRIGGERED = "TAX_RUN_TRIGGERED"

    val ALL: List<String> = listOf(
        LIMIT_CHANGED, USED_LIMIT_RESET, ORDER_APPROVED, ORDER_DECLINED,
        PERMISSIONS_CHANGED, TAX_RUN_TRIGGERED
    )

    /** Srpske labele za UI prikaz. */
    val LABEL_SR: Map<String, String> = mapOf(
        LIMIT_CHANGED to "Promena limita",
        USED_LIMIT_RESET to "Reset iskoriscenog limita",
        ORDER_APPROVED to "Order odobren",
        ORDER_DECLINED to "Order odbijen",
        PERMISSIONS_CHANGED to "Izmena permisija",
        TAX_RUN_TRIGGERED to "Pokrenut poreski obracun"
    )

    fun label(actionType: String?): String = actionType?.let { LABEL_SR[it] ?: it } ?: "—"
}
