package rs.raf.banka2.mobile.data.dto.recurringorder

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * [FE3 Mobile port — DCA / RecurringOrder (trajni nalozi)]
 *
 * Mapira BE `RecurringOrderDto` (trading-service `rs.raf.trading.recurringorder.dto`).
 *
 * BE rute:
 *   POST   /recurring-orders            (CreateRecurringOrderDto, 201 Created)
 *   GET    /recurring-orders            -> listMy
 *   GET    /recurring-orders/{id}       -> getById
 *   PATCH  /recurring-orders/{id}/pause -> ACTIVE -> PAUSED
 *   PATCH  /recurring-orders/{id}/resume -> PAUSED -> ACTIVE
 *   DELETE /recurring-orders/{id}       -> 204 No Content
 *
 * Polja:
 *   - direction: "BUY" ili "SELL" (BE pattern regex)
 *   - mode:      "BYAMOUNT" (po novcanom iznosu) | "BYQUANTITY" (po broju akcija)
 *   - cadence:   "DAILY" | "WEEKLY" | "MONTHLY"
 *   - value:     ako mode=BYAMOUNT iznos u valuti racuna; ako BYQUANTITY broj akcija
 *   - active:    true = ACTIVE (sledeci run zakazan), false = PAUSED
 *   - nextRun:   ISO LocalDateTime (sledeci put kad ce scheduler okidnuti)
 *
 * Spec: TODO_final C3 #10 + TODO_testovi.pdf Sc 47-53.
 */
@JsonClass(generateAdapter = true)
data class RecurringOrderDto(
    val id: Long,
    val ownerId: Long? = null,
    val ownerType: String? = null,
    val listingId: Long,
    val listingTicker: String? = null,
    val direction: String,   // "BUY" | "SELL"
    val mode: String,        // "BYAMOUNT" | "BYQUANTITY"
    val value: BigDecimal,
    val accountId: Long,
    val cadence: String,     // "DAILY" | "WEEKLY" | "MONTHLY"
    val nextRun: String? = null,
    val active: Boolean,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * Payload za POST /recurring-orders. `firstRun` opciono (BE racuna iz `cadence`
 * ako se ne posalje).
 */
@JsonClass(generateAdapter = true)
data class CreateRecurringOrderRequest(
    val listingId: Long,
    val direction: String,
    val mode: String,
    val value: BigDecimal,
    val accountId: Long,
    val cadence: String,
    val firstRun: String? = null,
)

enum class RecurringCadence(val apiValue: String, val labelSr: String) {
    DAILY("DAILY", "Dnevno"),
    WEEKLY("WEEKLY", "Sedmicno"),
    MONTHLY("MONTHLY", "Mesecno"),
}

enum class RecurringMode(val apiValue: String, val labelSr: String) {
    BYAMOUNT("BYAMOUNT", "Po iznosu"),
    BYQUANTITY("BYQUANTITY", "Po kolicini"),
}

enum class RecurringDirection(val apiValue: String, val labelSr: String) {
    BUY("BUY", "Kupovina"),
    SELL("SELL", "Prodaja"),
}

object RecurringOrderLabels {
    /** UI tab filter za RecurringOrdersScreen. */
    enum class FilterTab(val labelSr: String) {
        ACTIVE("Aktivni"),
        PAUSED("Pauzirani"),
        ALL("Svi"),
    }

    fun cadenceLabel(api: String): String = when (api.uppercase()) {
        "DAILY" -> "Dnevno"
        "WEEKLY" -> "Sedmicno"
        "MONTHLY" -> "Mesecno"
        else -> api
    }

    fun modeLabel(api: String): String = when (api.uppercase()) {
        "BYAMOUNT" -> "Po iznosu"
        "BYQUANTITY" -> "Po kolicini"
        else -> api
    }

    fun directionLabel(api: String): String = when (api.uppercase()) {
        "BUY" -> "Kupovina"
        "SELL" -> "Prodaja"
        else -> api
    }
}
