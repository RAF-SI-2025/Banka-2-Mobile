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

/**
 * ME-12 fix: BE EmployeeResponseDto izlaze `phone` (ne `phoneNumber`) i `permissions: Set<String>`.
 * `isAgent`/`isSupervisor`/`isAdmin` su derived flagovi iz `permissions` koje cuvamo radi
 * lakseg pristupa iz UI sloja (mapiraju se posle parsiranja, vidi `derivePermissionFlags`).
 */
@JsonClass(generateAdapter = true)
data class EmployeeDto(
    val id: Long,
    val email: String,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    // BE polje se zove `phone` — Moshi `@Json(name = ...)` da prima oba imena
    // bez breaking change-a na postojece consumere koji citaju `phoneNumber`.
    @param:com.squareup.moshi.Json(name = "phone") val phoneNumber: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val position: String? = null,
    val department: String? = null,
    val active: Boolean? = null,
    val permissions: List<String>? = null,
    val isAgent: Boolean? = null,
    val isSupervisor: Boolean? = null,
    val isAdmin: Boolean? = null,
    val createdAt: String? = null
) {
    /**
     * Helper: BE ne mora da posalje flagove, samo `permissions: List<String>`.
     * Ovi su computed iz liste, ali ako BE eksplicitno postavi `isAgent`, taj
     * override pobedjuje (legacy compat sa starim mock-ovima).
     */
    val derivedIsAgent: Boolean get() = isAgent ?: permissions.orEmpty().any { it.equals("AGENT", true) }
    val derivedIsSupervisor: Boolean get() = isSupervisor ?: permissions.orEmpty().any { it.equals("SUPERVISOR", true) }
    val derivedIsAdmin: Boolean get() = isAdmin ?: permissions.orEmpty().any { it.equals("ADMIN", true) }
}

/**
 * ME-12 fix: paritet sa `CreateEmployeeRequestDto.java`:
 *  - `phone` umesto `phoneNumber`
 *  - `username`, `dateOfBirth`, `gender` obavezna polja (BE @NotBlank)
 *  - `permissions: List<String>` umesto isAgent/isSupervisor boolean flagova
 *  - Datum kao ISO `YYYY-MM-DD` string (Moshi nema LocalDate adapter; BE
 *    Jackson default-uje `LocalDate.parse(String)`).
 */
@JsonClass(generateAdapter = true)
data class CreateEmployeeRequestDto(
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String, // YYYY-MM-DD
    val gender: String,
    val phone: String,
    val address: String,
    val position: String,
    val department: String,
    val active: Boolean? = true,
    val permissions: List<String> = emptyList()
)

/**
 * ME-13 fix: BE `UpdateEmployeeRequestDto.java` sad prima `permissions: Set<String>`
 * pa nema potrebe za odvojenim PATCH /permissions endpoint-om. Sve ide kroz
 * PUT /employees/{id} sa svim poljima ukljucujuci permissions i active u jednom
 * pozivu (ME-10 atomic update).
 */
@JsonClass(generateAdapter = true)
data class UpdateEmployeeRequestDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val position: String? = null,
    val department: String? = null,
    val active: Boolean? = null,
    val permissions: List<String>? = null
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

/**
 * ME-04 fix: dodato `canTradeStocks` polje (T4A-017 / spec Celina 4 §137-141).
 * BE polje moze biti null za legacy seed; default true u UserProfile.canTradeStocks.
 */
@JsonClass(generateAdapter = true)
data class ClientDto(
    val id: Long,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val status: String? = null,
    val active: Boolean? = null,
    val canTradeStocks: Boolean? = null
)
