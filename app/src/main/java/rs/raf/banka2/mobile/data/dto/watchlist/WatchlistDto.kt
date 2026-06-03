package rs.raf.banka2.mobile.data.dto.watchlist

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * [FE2 Mobile port — Watchlist (multi-list manager)]
 *
 * Mapira na BE `WatchlistDto` (trading-service `rs.raf.trading.watchlist.dto`).
 * BE rute (paritet sa FE web):
 *   GET    /watchlists                    -> listMyWatchlists
 *   POST   /watchlists                    -> createWatchlist (CreateWatchlistDto)
 *   PATCH  /watchlists/{id}               -> renameWatchlist (CreateWatchlistDto)
 *   DELETE /watchlists/{id}
 *   GET    /watchlists/{id}/items?type=   -> listItems sa filter po tipu hartije
 *   POST   /watchlists/{id}/items?listingId={listingId}
 *   DELETE /watchlists/{id}/items/{listingId}   (po listingId, NE po itemId!)
 *
 * Spec: TODO_final C3 #8 + TODO_testovi.pdf Sc 35-39.
 */
@JsonClass(generateAdapter = true)
data class WatchlistDto(
    val id: Long,
    val ownerId: Long? = null,
    val ownerType: String? = null,
    val name: String,
    val itemCount: Int = 0,
    val createdAt: String? = null,
)

/**
 * Mapira na BE `WatchlistItemDto`. BE imenuje polja `ticker`, `listingName`,
 * `securityType`, `exchangeName`, `dailyChange`.
 *
 * R1 812: `dailyChange` je PROCENAT dnevne promene (BE
 * `WatchlistService` ga puni iz `listing.getChangePercent()`), NE apsolutna
 * promena cene — UI ga prikazuje sa `%` sufiksom direktno (bez dodatnog
 * preracuna iz cene).
 */
@JsonClass(generateAdapter = true)
data class WatchlistItemDto(
    val id: Long,
    val watchlistId: Long,
    val listingId: Long,
    val ticker: String,
    val listingName: String? = null,
    val securityType: String? = null,
    val exchangeName: String? = null,
    val currentPrice: BigDecimal? = null,
    val dailyChange: BigDecimal? = null,
    val volume: Long? = null,
    val addedAt: String? = null,
)

/** Payload za POST /watchlists i PATCH /watchlists/{id} (rename). */
@JsonClass(generateAdapter = true)
data class CreateWatchlistRequest(
    val name: String,
)

/**
 * Payload za POST /watchlists/{id}/items.
 *
 * KONTRAKT (R1-236): BE `addItem` cita `@RequestBody AddWatchlistItemRequest`
 * (`{"listingId":123}`), NE `@RequestParam`. Ranije je Mobile slao `?listingId=`
 * query → BE @NotNull listingId null → 400 na svako dodavanje u watchlistu.
 */
@JsonClass(generateAdapter = true)
data class AddWatchlistItemRequest(
    val listingId: Long,
)

/**
 * Filter po tipu hartije za UI chip. ALL ne salje query param BE-u
 * (BE `listItems(id, type=null)` vraca sve).
 */
enum class WatchlistFilterType(val apiValue: String?, val labelSr: String) {
    ALL(null, "Sve"),
    STOCK("STOCK", "Akcije"),
    FUTURES("FUTURES", "Fjucersi"),
    FOREX("FOREX", "Valute"),
    OPTION("OPTION", "Opcije");

    companion object {
        fun fromSecurityType(securityType: String?): WatchlistFilterType =
            when (securityType?.uppercase()) {
                "STOCK" -> STOCK
                "FUTURES" -> FUTURES
                "FOREX" -> FOREX
                "OPTION" -> OPTION
                else -> ALL
            }
    }
}
