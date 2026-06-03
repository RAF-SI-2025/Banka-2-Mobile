package rs.raf.banka2.mobile.data.dto.audit

import com.squareup.moshi.JsonClass

/**
 * Audit log zapis sa svih BE polja (B7 / spec Celina 3 §69).
 * Vraca `GET /audit` paginirano sa Spring `Page` omotacem (vidi `AuditApi`).
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
 * Lista podrzanih audit action tipova — koristi se za render filter chip-ova.
 *
 * R1-594: ranije je lista imala samo 6 tipova, pa je dropdown nudio mali podskup
 * stvarnih BE akcija. Sada je UNIJA `AuditActionType` enum-a OBA servisa
 * (banka-core + trading-service) = 30 tipova, identicno FE `types/audit.ts`.
 * Gateway rutira `/audit` ka pravom servisu po kontekstu, ali filter nudi sve.
 * `ALL` je IZVEDEN iz `LABEL_SR` redosleda → nikad van sinhrona sa labelama.
 */
object AuditActionTypes {
    // Zajednicki (aktuari / orderi / tax / permisije)
    const val LIMIT_CHANGED = "LIMIT_CHANGED"
    const val USED_LIMIT_RESET = "USED_LIMIT_RESET"
    const val ORDER_APPROVED = "ORDER_APPROVED"
    const val ORDER_DECLINED = "ORDER_DECLINED"
    const val PERMISSIONS_CHANGED = "PERMISSIONS_CHANGED"
    const val TAX_RUN_TRIGGERED = "TAX_RUN_TRIGGERED"

    /** Srpske labele za UI prikaz (kljucevi = svi BE tipovi). */
    val LABEL_SR: Map<String, String> = linkedMapOf(
        LIMIT_CHANGED to "Promena limita",
        USED_LIMIT_RESET to "Reset iskoriscenog limita",
        ORDER_APPROVED to "Order odobren",
        ORDER_DECLINED to "Order odbijen",
        PERMISSIONS_CHANGED to "Izmena permisija",
        TAX_RUN_TRIGGERED to "Pokrenut poreski obracun",
        // banka-core — krediti
        "LOAN_APPROVED" to "Kredit odobren",
        "LOAN_REJECTED" to "Kredit odbijen",
        "LOAN_EARLY_REPAYMENT" to "Prevremena otplata kredita",
        "LOAN_INSTALLMENT_PAID" to "Rata kredita naplacena",
        "LOAN_INSTALLMENT_FAILED" to "Naplata rate neuspela",
        // banka-core — placanja
        "PAYMENT_CREATED" to "Placanje kreirano",
        "PAYMENT_ABORTED" to "Placanje prekinuto",
        "PAYMENT_QUICK_APPROVED" to "Placanje brzo odobreno",
        // banka-core — transferi
        "TRANSFER_INTERNAL" to "Interni transfer",
        "TRANSFER_FX" to "Devizni transfer",
        // banka-core — stednja
        "SAVINGS_OPENED" to "Orocena stednja otvorena",
        "SAVINGS_WITHDRAWN_EARLY" to "Prevremeno razorocenje",
        "SAVINGS_AUTO_RENEWED" to "Automatska obnova stednje",
        // banka-core — kartice
        "CARD_BLOCKED" to "Kartica blokirana",
        "CARD_UNBLOCKED" to "Kartica odblokirana",
        "CARD_LIMIT_CHANGED" to "Promena limita kartice",
        "CARD_DEACTIVATED" to "Kartica deaktivirana",
        // banka-core — racuni
        "ACCOUNT_STATUS_CHANGED" to "Promena statusa racuna",
        "ACCOUNT_LIMITS_CHANGED" to "Promena limita racuna",
        // banka-core — zaposleni
        "EMPLOYEE_DEACTIVATED" to "Zaposleni deaktiviran",
        // trading — fondovi
        "FUND_CREATED" to "Fond kreiran",
        "FUND_INVEST" to "Uplata u fond",
        "FUND_WITHDRAW" to "Povlacenje iz fonda",
        // trading — bulk cron reset aktuarskih limita
        "USED_LIMIT_RESET_ALL" to "Reset svih limita (cron)"
    )

    /** Izvedeno iz labela tako da nikad ne ostane van sinhrona sa unijom tipova. */
    val ALL: List<String> = LABEL_SR.keys.toList()

    fun label(actionType: String?): String = actionType?.let { LABEL_SR[it] ?: it } ?: "—"
}
