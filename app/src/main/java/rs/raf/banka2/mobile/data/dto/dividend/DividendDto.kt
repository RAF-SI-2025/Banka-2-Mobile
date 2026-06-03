package rs.raf.banka2.mobile.data.dto.dividend

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Jedna isplacena dividenda za poziciju u portfoliju — paritet sa FE
 * `DividendPayoutDto` (B9 spec, `GET /dividends/my` i
 * `GET /dividends/by-position/{portfolioId}`).
 *
 * EMPLOYEE ima `taxExempt=true` (aktuar drzi akcije u ime banke — bez 15% poreza).
 */
@JsonClass(generateAdapter = true)
data class DividendPayoutDto(
    val id: Long,
    val ownerId: Long? = null,
    val ownerType: String? = null, // CLIENT / EMPLOYEE
    val stockListingId: Long? = null,
    val stockTicker: String? = null,
    val quantity: Int? = null,
    /** Cena akcije na dan obracuna. */
    val priceOnDate: BigDecimal? = null,
    /** Kvartalni prinos (godisnji dividendYield / 4). */
    val dividendYieldRate: Double? = null,
    /** Bruto iznos pre poreza. */
    val grossAmount: BigDecimal = BigDecimal.ZERO,
    /** Iznos poreza po odbitku (0 za EMPLOYEE — vidi taxExempt). */
    val tax: BigDecimal = BigDecimal.ZERO,
    /** Neto iznos knjizen na racun. */
    val netAmount: BigDecimal = BigDecimal.ZERO,
    val creditedAccountId: Long? = null,
    val currencyCode: String? = null,
    /** ISO datum isplate (YYYY-MM-DD). */
    val paymentDate: String? = null,
    /** true za EMPLOYEE (aktuar drzi akcije u ime banke — bez 15% poreza). */
    val taxExempt: Boolean = false,
    /** ISO datetime kreiranja zapisa. */
    val createdAt: String? = null
)
