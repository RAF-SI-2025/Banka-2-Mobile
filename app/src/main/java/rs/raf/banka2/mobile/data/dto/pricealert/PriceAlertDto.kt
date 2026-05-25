package rs.raf.banka2.mobile.data.dto.pricealert

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * [FE2 Mobile port — Price Alert (cenovni alarmi)]
 *
 * Mapira BE `PriceAlertDto` (trading-service `rs.raf.trading.pricealert.dto`).
 *
 * BE rute:
 *   POST   /price-alerts                       (CreatePriceAlertDto, 201 Created)
 *   GET    /price-alerts/my?active=true|false  (opcioni filter)
 *   DELETE /price-alerts/{id}                  (204 No Content)
 *
 * `condition` je STRING enum sa BE-a ("ABOVE" | "BELOW"). Moshi default mapiranje
 * radi 1:1 jer BE serializuje enum kao string.
 *
 * Spec: TODO_final C3 #6 + TODO_testovi.pdf Sc 26-29.
 */
@JsonClass(generateAdapter = true)
data class PriceAlertDto(
    val id: Long,
    val ownerId: Long? = null,
    val ownerType: String? = null,
    val listingId: Long,
    val listingTicker: String,
    val listingType: String? = null,
    val condition: String, // "ABOVE" | "BELOW"
    val threshold: BigDecimal,
    val active: Boolean,
    val createdAt: String? = null,
    val triggeredAt: String? = null,
)

/**
 * Payload za POST /price-alerts. BE validacije:
 *  - listingId NotNull
 *  - condition NotNull (enum ABOVE/BELOW)
 *  - threshold NotNull i > 0
 */
@JsonClass(generateAdapter = true)
data class CreatePriceAlertRequest(
    val listingId: Long,
    val condition: String, // "ABOVE" | "BELOW"
    val threshold: BigDecimal,
)

enum class PriceAlertCondition(val apiValue: String, val labelSr: String) {
    ABOVE("ABOVE", "Iznad praga"),
    BELOW("BELOW", "Ispod praga"),
}

object PriceAlertLabels {
    /** Klijent-orijentisan filter dropdown na PriceAlertsScreen. */
    enum class FilterTab(val labelSr: String) {
        ACTIVE("Aktivni"),
        HISTORY("Istorija"),
        ALL("Sve"),
    }

    fun conditionLabel(condition: String): String = when (condition.uppercase()) {
        "ABOVE" -> "Iznad praga"
        "BELOW" -> "Ispod praga"
        else -> condition
    }
}
