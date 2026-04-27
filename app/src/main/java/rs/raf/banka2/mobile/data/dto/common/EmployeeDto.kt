package rs.raf.banka2.mobile.data.dto.common

import com.squareup.moshi.JsonClass


/**
 * Pageable wrapper koji backend vraca za paginirane endpoint-e (Spring Page).
 * Generička klasa namerno NEMA `@JsonClass(generateAdapter = true)` jer
 * Moshi KSP codegen ne podrzava `T` parametar — odgovor parsira reflection
 * adapter (`KotlinJsonAdapterFactory`) registrovan u [NetworkModule].
 */
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0L,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
    val empty: Boolean = true
)

@JsonClass(generateAdapter = true)
data class EmployeeDto(
    val id: Long,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val position: String? = null,
    val department: String? = null,
    val active: Boolean? = null,
    val permissions: List<String>? = null,
    val isAgent: Boolean? = null,
    val isSupervisor: Boolean? = null,
    val isAdmin: Boolean? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateEmployeeRequestDto(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val position: String? = null,
    val department: String? = null,
    val isAgent: Boolean = false,
    val isSupervisor: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UpdateEmployeeRequestDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val position: String? = null,
    val department: String? = null,
    val active: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class UpdateEmployeePermissionsDto(
    val isAgent: Boolean,
    val isSupervisor: Boolean,
    val isAdmin: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class CreateClientRequestDto(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String? = null,
    val address: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateClientRequestDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null
)

@JsonClass(generateAdapter = true)
data class ClientDto(
    val id: Long,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val status: String? = null
)
