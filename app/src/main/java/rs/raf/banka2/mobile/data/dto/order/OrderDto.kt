package rs.raf.banka2.mobile.data.dto.order

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * KONTRAKT (R1-162): BE `CreateOrderDto` (trading-service) cita `limitValue`/
 * `stopValue` za LIMIT/STOP cene i `fundId` za fund-trade — NE `limitPrice`/
 * `stopPrice`/`onBehalfOfFundId`. Ranije je Mobile slao pogresna imena pa je
 * BE potpuno ignorisao cenu (LIMIT/STOP order bez cene = 400/odbijen). Cuvamo
 * Kotlin imena koja VM puni, ali ih vezujemo na BE wire-imena preko `@Json`.
 */
@JsonClass(generateAdapter = true)
data class CreateOrderDto(
    val listingId: Long,
    val orderType: String,            // MARKET / LIMIT / STOP / STOP_LIMIT
    val direction: String,            // BUY / SELL
    val quantity: Int,
    @param:Json(name = "limitValue") val limitPrice: BigDecimal? = null,
    @param:Json(name = "stopValue") val stopPrice: BigDecimal? = null,
    val allOrNone: Boolean = false,
    val margin: Boolean = false,
    val accountId: Long? = null,
    @param:Json(name = "fundId") val onBehalfOfFundId: Long? = null,
    val otpCode: String
)

/**
 * KONTRAKT (R6-1987): BE `OrderDto` salje `limitValue`/`stopValue` (NE
 * `limitPrice`/`stopPrice`), `pricePerUnit`/`approximatePrice` za fill cenu,
 * `fundId` (NE `onBehalfOfFundId`), `lastModification` (NE `updatedAt`) i NE
 * salje `filledQuantity`/`averageExecutionPrice`/`totalValue`/`fee`/
 * `accountCurrency`/`approvedBy`/`onBehalfOfFundName`. Vezujemo Kotlin imena na
 * BE wire-imena preko `@Json` da LIMIT/STOP/fund/fill polja ne budu uvek null.
 */
@JsonClass(generateAdapter = true)
data class OrderDto(
    val id: Long,
    val listingId: Long? = null,
    val listingTicker: String? = null,
    val listingName: String? = null,
    val listingType: String? = null,
    val orderType: String,
    val direction: String,
    val quantity: Int,
    val remainingPortions: Int? = null,
    @param:Json(name = "limitValue") val limitPrice: BigDecimal? = null,
    @param:Json(name = "stopValue") val stopPrice: BigDecimal? = null,
    /** BE fill/izvrsna cena po jedinici. */
    @param:Json(name = "pricePerUnit") val pricePerUnit: BigDecimal? = null,
    /** BE rezervisana/approx cena (Market/Stop). */
    @param:Json(name = "approximatePrice") val approximatePrice: BigDecimal? = null,
    val fxCommission: BigDecimal? = null,
    val exchangeRate: BigDecimal? = null,
    val status: String,               // PENDING / APPROVED / DONE / DECLINED
    val allOrNone: Boolean? = null,
    val margin: Boolean? = null,
    val afterHours: Boolean? = null,
    val approvedBy: String? = null,
    @param:Json(name = "fundId") val onBehalfOfFundId: Long? = null,
    val createdAt: String? = null,
    @param:Json(name = "lastModification") val updatedAt: String? = null,
    // BE OrderDto ih NE salje (uvek null) — zadrzani da UI kod koji ih cita ne
    // pukne kompilaciju; prikaz fill/vrednosti se oslanja na pricePerUnit/quantity.
    val filledQuantity: Int? = null,
    val totalValue: BigDecimal? = null,
    val fee: BigDecimal? = null,
    val onBehalfOfFundName: String? = null
)
