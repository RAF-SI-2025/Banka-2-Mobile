package rs.raf.banka2.mobile.data.dto.fundstatistics

import com.squareup.moshi.JsonClass

/**
 * Statisticke metrike performansi jednog fonda — paritet sa FE
 * `FundStatisticsDto` (B12 spec, `GET /funds/{id}/statistics`).
 *
 * Metrike su null kad fond ima manje od 30 dnevnih snimaka — BE prag
 * MIN_SNAPSHOTS_REQUIRED = 30. UI fallback "Nema dovoljno istorije".
 */
@JsonClass(generateAdapter = true)
data class FundStatisticsDto(
    val fundId: Long,
    val fundName: String? = null,
    /** Ukupan broj dnevnih snimaka koji se koriste za obracun. */
    val snapshotCount: Int = 0,
    /** Anualizovani prinos u % (godisnji). null ako nema dovoljno snimaka. */
    val annualizedReturnPercent: Double? = null,
    /** Standardna devijacija mesecnih prinosa u %. */
    val volatilityPercent: Double? = null,
    /** Maksimalni pad od vrha do dna u % (negativna vrednost ili 0). */
    val maxDrawdownPercent: Double? = null,
    /** Sharpe-like racio: annualizedReturn / volatility. */
    val rewardToVariabilityRatio: Double? = null,
    /** true ako je snapshotCount >= BE prag. */
    val sufficientHistory: Boolean = false
)
