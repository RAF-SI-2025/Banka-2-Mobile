package rs.raf.banka2.mobile.data.dto.notification

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * TODO_final C2 #4 — In-app notifikacije (Mobile portovan iz FE-a).
 *
 * Polje `type` se na BE-u modelira kao enum + opciono `data` JSON za
 * referenced entity. Na Mobile-u mapiramo BE enum imena direktno u
 * [NotificationType] string konstante i radimo deep-link preko
 * `relatedEntityType`/`relatedEntityId`.
 *
 * KONTRAKT (P0-M1 N1): BE `NotificationDto` (banka-core
 * `rs.raf.banka2_bek.notification.dto.NotificationDto`) salje telo poruke kao
 * `body` (NE `message`) i referencu kao `referenceType`/`referenceId`
 * (NE `relatedEntityType`/`relatedEntityId`). Stara Moshi mapa je bacala na
 * SVAKI red (non-null `message` koje BE nikad ne salje) → cela lista je UVEK
 * pucala. Zadrzavamo Kotlin imena polja koja ostatak koda cita
 * (`message`/`relatedEntity*`) ali ih vezujemo za BE JSON imena preko
 * [Json] aliasa; `message` je nullable da nedostajuci/null `body` ne pukne.
 *
 * Reference: `Banka-2-Frontend/src/types/notification.ts`,
 * `banka2_bek/.../notification/dto/NotificationDto.java`.
 */
@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: Long,
    /** Vrednost iz [NotificationType.ALL] — fallback je [NotificationType.GENERAL]. */
    val type: String,
    val title: String,
    /** BE polje `body`. Nullable jer BE moze poslati null telo. */
    @param:Json(name = "body") val message: String? = null,
    val read: Boolean,
    /** ISO 8601 timestamp. */
    val createdAt: String,
    /** BE `referenceType`: PAYMENT / ORDER / OTC_OFFER / OTC_CONTRACT / FUND / LOAN / CARD / ACCOUNT */
    @param:Json(name = "referenceType") val relatedEntityType: String? = null,
    /** BE `referenceId`. */
    @param:Json(name = "referenceId") val relatedEntityId: Long? = null
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
    // P1-mobile-banking-1 (R1-151 Mobile deo): vrednosti uskladjene 1:1 sa STVARNIM
    // BE enum-om (`banka2_bek` + `trading-service` `NotificationType.java`) — paritet sa
    // FE web `notification.ts`. Stari spisak je imao izmisljena imena (`PAYMENT_RECEIVED`/
    // `ORDER_FILLED`/`OTC_OFFER_*`/`FUND_*`/`MARGIN_ACCOUNT_BLOCKED`/`GENERIC`...) kojih
    // nema u BE enum-u → labela/ikona se nikad nije razresavala. Fallback je `GENERAL`.
    const val PAYMENT = "PAYMENT"
    const val TRANSFER = "TRANSFER"
    const val LIMIT_CHANGE = "LIMIT_CHANGE"

    const val ORDER_PENDING = "ORDER_PENDING"
    const val ORDER_APPROVED = "ORDER_APPROVED"
    const val ORDER_DECLINED = "ORDER_DECLINED"
    const val ORDER_EXECUTED = "ORDER_EXECUTED"
    const val ORDER_PARTIAL_FILL = "ORDER_PARTIAL_FILL"
    const val ORDER_CANCELLED = "ORDER_CANCELLED"

    const val OTC_COUNTER_OFFER = "OTC_COUNTER_OFFER"
    const val OTC_ACCEPTED = "OTC_ACCEPTED"
    const val OTC_DECLINED = "OTC_DECLINED"
    const val OTC_CONTRACT_EXPIRING = "OTC_CONTRACT_EXPIRING"

    const val LOAN_CREATED = "LOAN_CREATED"
    const val LOAN_APPROVED = "LOAN_APPROVED"
    const val LOAN_REJECTED = "LOAN_REJECTED"

    const val CARD_BLOCKED = "CARD_BLOCKED"
    const val CARD_UNBLOCKED = "CARD_UNBLOCKED"

    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"

    const val PRICE_ALERT_TRIGGERED = "PRICE_ALERT_TRIGGERED"
    const val RECURRING_ORDER_SKIPPED = "RECURRING_ORDER_SKIPPED"

    /** BE fallback tip — koristi se i kao default za nepoznate vrednosti. */
    const val GENERAL = "GENERAL"

    /** Sve poznate vrednosti — koristi se u testovima i UI helper-ima. */
    val ALL: Set<String> = setOf(
        PAYMENT, TRANSFER, LIMIT_CHANGE,
        ORDER_PENDING, ORDER_APPROVED, ORDER_DECLINED, ORDER_EXECUTED,
        ORDER_PARTIAL_FILL, ORDER_CANCELLED,
        OTC_COUNTER_OFFER, OTC_ACCEPTED, OTC_DECLINED, OTC_CONTRACT_EXPIRING,
        LOAN_CREATED, LOAN_APPROVED, LOAN_REJECTED,
        CARD_BLOCKED, CARD_UNBLOCKED,
        ACCOUNT_LOCKED,
        PRICE_ALERT_TRIGGERED, RECURRING_ORDER_SKIPPED,
        GENERAL
    )

    fun normalize(raw: String?): String =
        if (raw != null && raw in ALL) raw else GENERAL

    /** Mapa stringa → srpska labela. Nepoznata vrednost → "Obavestenje". */
    val LABEL_SR: Map<String, String> = mapOf(
        PAYMENT to "Placanje",
        TRANSFER to "Prenos sredstava",
        LIMIT_CHANGE to "Promena limita",
        ORDER_PENDING to "Order na cekanju",
        ORDER_APPROVED to "Order odobren",
        ORDER_DECLINED to "Order odbijen",
        ORDER_EXECUTED to "Order izvrsen",
        ORDER_PARTIAL_FILL to "Order delimicno izvrsen",
        ORDER_CANCELLED to "Order otkazan",
        OTC_COUNTER_OFFER to "OTC kontraponuda",
        OTC_ACCEPTED to "Prihvacena OTC ponuda",
        OTC_DECLINED to "Odbijena OTC ponuda",
        OTC_CONTRACT_EXPIRING to "OTC ugovor uskoro istice",
        LOAN_CREATED to "Zahtev za kredit kreiran",
        LOAN_APPROVED to "Kredit odobren",
        LOAN_REJECTED to "Kredit odbijen",
        CARD_BLOCKED to "Kartica blokirana",
        CARD_UNBLOCKED to "Kartica odblokirana",
        ACCOUNT_LOCKED to "Nalog zakljucan",
        PRICE_ALERT_TRIGGERED to "Cenovni alarm aktiviran",
        RECURRING_ORDER_SKIPPED to "Trajni nalog preskocen",
        GENERAL to "Obavestenje"
    )

    fun labelOf(type: String?): String = LABEL_SR[normalize(type)] ?: "Obavestenje"
}

/**
 * Filter mode za listu notifikacija. Mapira se na BE query param `onlyUnread`
 * (Boolean) u `NotificationRepository.list`: [ALL] -> null (sve), [UNREAD] -> true.
 * R1 693/694: stari `queryValue` ("all"/"unread") string je bio mrtav — nijedan
 * caller ga nije citao, a komentar je pogresno tvrdio da BE prima `filter=...` param.
 */
enum class NotificationFilter {
    ALL,
    UNREAD
}
