package rs.raf.banka2.mobile.data.dto.notification

import com.squareup.moshi.JsonClass

/**
 * TODO_final C2 #4 — In-app notifikacije (Mobile portovan iz FE-a).
 *
 * Polje `type` se na BE-u modelira kao enum + opciono `data` JSON za
 * referenced entity. Na Mobile-u mapiramo BE enum imena direktno u
 * [NotificationType] string konstante i radimo deep-link preko
 * `relatedEntityType`/`relatedEntityId`.
 *
 * Reference: `Banka-2-Frontend/src/types/notification.ts`.
 */
@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: Long,
    /** Vrednost iz [NotificationType.ALL] — fallback je [NotificationType.GENERIC]. */
    val type: String,
    val title: String,
    val message: String,
    val read: Boolean,
    /** ISO 8601 timestamp. */
    val createdAt: String,
    /** PAYMENT / ORDER / OTC_OFFER / OTC_CONTRACT / FUND / LOAN / CARD / ACCOUNT */
    val relatedEntityType: String? = null,
    val relatedEntityId: Long? = null
)

@JsonClass(generateAdapter = true)
data class NotificationPageDto(
    val content: List<NotificationDto> = emptyList(),
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 20
)

@JsonClass(generateAdapter = true)
data class UnreadCountDto(
    val count: Int = 0
)

/**
 * Mobile-strana enum-style const string-ovi koji odgovaraju BE
 * `NotificationType` enum vrednostima. Cuvamo kao string-ove (a ne
 * Kotlin enum) zato sto Moshi ne sme bacati na nepoznatu vrednost
 * iz BE-a (forward compat).
 */
object NotificationType {
    const val PAYMENT_RECEIVED = "PAYMENT_RECEIVED"
    const val PAYMENT_SENT = "PAYMENT_SENT"
    const val PAYMENT_PENDING_APPROVAL = "PAYMENT_PENDING_APPROVAL"

    const val ORDER_PENDING = "ORDER_PENDING"
    const val ORDER_APPROVED = "ORDER_APPROVED"
    const val ORDER_DECLINED = "ORDER_DECLINED"
    const val ORDER_EXECUTED = "ORDER_EXECUTED"
    const val ORDER_PARTIAL_FILL = "ORDER_PARTIAL_FILL"
    const val ORDER_CANCELLED = "ORDER_CANCELLED"
    const val ORDER_FILLED = "ORDER_FILLED"

    const val OTC_OFFER_RECEIVED = "OTC_OFFER_RECEIVED"
    const val OTC_COUNTER_OFFER = "OTC_COUNTER_OFFER"
    const val OTC_ACCEPTED = "OTC_ACCEPTED"
    const val OTC_DECLINED = "OTC_DECLINED"
    const val OTC_OFFER_ACCEPTED = "OTC_OFFER_ACCEPTED"
    const val OTC_OFFER_DECLINED = "OTC_OFFER_DECLINED"
    const val OTC_CONTRACT_EXPIRING = "OTC_CONTRACT_EXPIRING"
    const val OTC_CONTRACT_EXERCISED = "OTC_CONTRACT_EXERCISED"
    const val OTC_CONTRACT_EXPIRED = "OTC_CONTRACT_EXPIRED"

    const val FUND_INTEREST_PAID = "FUND_INTEREST_PAID"
    const val FUND_DEPOSIT_MATURED = "FUND_DEPOSIT_MATURED"

    const val LOAN_APPROVED = "LOAN_APPROVED"
    const val LOAN_DECLINED = "LOAN_DECLINED"
    const val LOAN_PAYMENT_DUE = "LOAN_PAYMENT_DUE"

    const val CARD_BLOCKED = "CARD_BLOCKED"
    const val CARD_UNBLOCKED = "CARD_UNBLOCKED"

    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    const val MARGIN_ACCOUNT_BLOCKED = "MARGIN_ACCOUNT_BLOCKED"

    const val PRICE_ALERT_TRIGGERED = "PRICE_ALERT_TRIGGERED"
    const val RECURRING_ORDER_SKIPPED = "RECURRING_ORDER_SKIPPED"

    const val GENERAL = "GENERAL"
    const val GENERIC = "GENERIC"
    const val TRANSFER = "TRANSFER"

    /** Default fallback za nepoznate BE vrednosti. */
    const val UNKNOWN = "UNKNOWN"

    /** Sve poznate vrednosti — koristi se u testovima i UI helper-ima. */
    val ALL: Set<String> = setOf(
        PAYMENT_RECEIVED, PAYMENT_SENT, PAYMENT_PENDING_APPROVAL,
        ORDER_PENDING, ORDER_APPROVED, ORDER_DECLINED, ORDER_EXECUTED,
        ORDER_PARTIAL_FILL, ORDER_CANCELLED, ORDER_FILLED,
        OTC_OFFER_RECEIVED, OTC_COUNTER_OFFER, OTC_ACCEPTED, OTC_DECLINED,
        OTC_OFFER_ACCEPTED, OTC_OFFER_DECLINED,
        OTC_CONTRACT_EXPIRING, OTC_CONTRACT_EXERCISED, OTC_CONTRACT_EXPIRED,
        FUND_INTEREST_PAID, FUND_DEPOSIT_MATURED,
        LOAN_APPROVED, LOAN_DECLINED, LOAN_PAYMENT_DUE,
        CARD_BLOCKED, CARD_UNBLOCKED,
        ACCOUNT_LOCKED, MARGIN_ACCOUNT_BLOCKED,
        PRICE_ALERT_TRIGGERED, RECURRING_ORDER_SKIPPED,
        GENERAL, GENERIC, TRANSFER
    )

    fun normalize(raw: String?): String =
        if (raw != null && raw in ALL) raw else GENERIC

    /** Mapa stringa → srpska labela. Nepoznata vrednost → "Obavestenje". */
    val LABEL_SR: Map<String, String> = mapOf(
        PAYMENT_RECEIVED to "Primljeno placanje",
        PAYMENT_SENT to "Poslato placanje",
        PAYMENT_PENDING_APPROVAL to "Placanje ceka odobrenje",
        TRANSFER to "Transfer",
        ORDER_PENDING to "Order kreiran",
        ORDER_APPROVED to "Order odobren",
        ORDER_DECLINED to "Order odbijen",
        ORDER_EXECUTED to "Order izvrsen",
        ORDER_PARTIAL_FILL to "Order delimicno izvrsen",
        ORDER_CANCELLED to "Order otkazan",
        ORDER_FILLED to "Order izvrsen",
        OTC_OFFER_RECEIVED to "Primljena OTC ponuda",
        OTC_COUNTER_OFFER to "Kontra-ponuda OTC",
        OTC_ACCEPTED to "Prihvacena OTC ponuda",
        OTC_DECLINED to "Odbijena OTC ponuda",
        OTC_OFFER_ACCEPTED to "Prihvacena OTC ponuda",
        OTC_OFFER_DECLINED to "Odbijena OTC ponuda",
        OTC_CONTRACT_EXPIRING to "OTC ugovor istice",
        OTC_CONTRACT_EXERCISED to "OTC ugovor iskoriscen",
        OTC_CONTRACT_EXPIRED to "OTC ugovor istekao",
        FUND_INTEREST_PAID to "Isplacena kamata fonda",
        FUND_DEPOSIT_MATURED to "Dospelo fond ulaganje",
        LOAN_APPROVED to "Kredit odobren",
        LOAN_DECLINED to "Kredit odbijen",
        LOAN_PAYMENT_DUE to "Dospela rata kredita",
        CARD_BLOCKED to "Kartica blokirana",
        CARD_UNBLOCKED to "Kartica odblokirana",
        ACCOUNT_LOCKED to "Nalog zakljucan",
        MARGIN_ACCOUNT_BLOCKED to "Margin racun blokiran",
        PRICE_ALERT_TRIGGERED to "Cenovni alarm aktiviran",
        RECURRING_ORDER_SKIPPED to "Trajni nalog preskocen",
        GENERAL to "Obavestenje",
        GENERIC to "Obavestenje"
    )

    fun labelOf(type: String?): String = LABEL_SR[normalize(type)] ?: "Obavestenje"
}

/**
 * Filter mode za listu notifikacija. Vrednost se salje BE-u kao
 * query param `filter` — backend prepoznaje "all" i "unread".
 */
enum class NotificationFilter(val queryValue: String) {
    ALL("all"),
    UNREAD("unread")
}
