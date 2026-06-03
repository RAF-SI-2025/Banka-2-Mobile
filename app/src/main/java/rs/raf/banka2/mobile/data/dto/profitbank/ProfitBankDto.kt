package rs.raf.banka2.mobile.data.dto.profitbank

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * KONTRAKT (R1-230): BE `ProfitBankDtos.ActuaryProfitDto` salje
 * `employeeId/name/position/totalProfitRsd/ordersDone` — NE
 * `firstName/lastName/email/realizedProfitRsd`. Ranije je Mobile citao
 * nepostojeca polja → profit svakog aktuara 0 RSD, ime prazno. Vezujemo
 * Kotlin imena (koja ProfitBankScreen cita) na BE wire-imena preko `@Json`.
 */
@JsonClass(generateAdapter = true)
data class ActuaryProfitDto(
    val employeeId: Long,
    @param:Json(name = "name") val name: String? = null,
    val position: String? = null,
    @param:Json(name = "totalProfitRsd") val realizedProfitRsd: BigDecimal = BigDecimal.ZERO,
    val ordersDone: Int? = null
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "—"
}

/**
 * KONTRAKT (R1-231): BE `ProfitBankDtos.BankFundPositionDto` salje
 * `fundId/fundName/managerName/percentShare/rsdValue/profitRsd` — NE
 * `sharePercent/shareAmountRsd/accountNumber`. Ranije su bankine pozicije
 * bile prazne/0. Vezujemo Kotlin imena na BE imena preko `@Json`.
 */
@JsonClass(generateAdapter = true)
data class BankFundPositionDto(
    val fundId: Long,
    val fundName: String? = null,
    val managerName: String? = null,
    @param:Json(name = "percentShare") val sharePercent: Double? = null,
    @param:Json(name = "rsdValue") val shareAmountRsd: BigDecimal? = null,
    val profitRsd: BigDecimal? = null
)
