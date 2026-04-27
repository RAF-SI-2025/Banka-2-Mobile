package rs.raf.banka2.mobile.data.dto.actuary

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActuaryDto(
    val employeeId: Long,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val name: String? = null,
    val position: String? = null,
    val dailyLimit: Double? = null,
    val usedLimit: Double? = null,
    val needApproval: Boolean? = null
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(firstName, lastName).joinToString(" ").takeIf { it.isNotBlank() }
            ?: email.orEmpty()
}

@JsonClass(generateAdapter = true)
data class UpdateActuaryLimitDto(
    val dailyLimit: Double,
    val needApproval: Boolean
)
