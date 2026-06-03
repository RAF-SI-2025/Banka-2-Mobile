package rs.raf.banka2.mobile.data.dto.tax

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * P1-mobile-banking-1 (R1-176): KONTRAKT uskladjen sa BE `TaxRecordDto`
 * (`rs.raf.trading.tax.dto.TaxRecordDto`). BE salje `userName`/`totalProfit`/
 * `taxOwed`/`taxPaid` — stari Mobile DTO je citao `name`/`totalGain`/`taxAmount`/
 * `paidThisYear` → ceo tax portal je bio prazan. Zadrzavamo Kotlin imena (koja
 * TaxScreen vec cita) ali ih vezujemo za BE JSON imena preko [Json] aliasa.
 * BE NEMA `totalLoss`/`taxableIncome`/`email` → ta polja ostaju null (UI fallback).
 */
@JsonClass(generateAdapter = true)
data class TaxRecordDto(
    val userId: Long? = null,
    @param:Json(name = "userName") val name: String? = null,
    val email: String? = null,
    val userType: String? = null,
    @param:Json(name = "totalProfit") val totalGain: BigDecimal? = null,
    val totalLoss: BigDecimal? = null,
    val taxableIncome: BigDecimal? = null,
    @param:Json(name = "taxOwed") val taxAmount: BigDecimal? = null,
    @param:Json(name = "taxPaid") val paidThisYear: BigDecimal? = null,
    val currency: String? = null
)

/**
 * Per-listing breakdown poreza za korisnika (Spec Celina 3 §516-518).
 * UI prikazuje "AAPL: +$50 -> 7.50 RSD" za svaku hartiju koja je doprinela
 * profitu/gubitku.
 */
@JsonClass(generateAdapter = true)
data class TaxBreakdownItemDto(
    val listingId: Long? = null,
    val ticker: String? = null,
    val listingCurrency: String? = null,
    val profitNative: BigDecimal? = null,
    val profitRsd: BigDecimal? = null,
    val taxOwed: BigDecimal? = null
)
