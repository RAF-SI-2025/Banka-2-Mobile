package rs.raf.banka2.mobile.data.dto.actuary

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * P1-mobile-banking-1 (R1-185): KONTRAKT uskladjen sa BE `ActuaryInfoDto`
 * (`rs.raf.trading.actuary.dto.ActuaryInfoDto`). BE salje `employeeName`/
 * `employeeEmail`/`employeePosition` — stari Mobile DTO je citao `firstName`/
 * `lastName`/`email`/`position` → ekran Aktuari je bio prazan (samo limit brojevi).
 * Zadrzavamo Kotlin imena (koja ActuariesScreen vec cita: `displayName`/`email`)
 * ali ih vezujemo za BE JSON imena preko [Json] aliasa.
 */
@JsonClass(generateAdapter = true)
data class ActuaryDto(
    val employeeId: Long,
    @param:Json(name = "employeeEmail") val email: String? = null,
    @param:Json(name = "employeeName") val name: String? = null,
    @param:Json(name = "employeePosition") val position: String? = null,
    val dailyLimit: BigDecimal? = null,
    val usedLimit: BigDecimal? = null,
    val needApproval: Boolean? = null
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: email.orEmpty()
}

@JsonClass(generateAdapter = true)
data class UpdateActuaryLimitDto(
    val dailyLimit: BigDecimal,
    val needApproval: Boolean
)
