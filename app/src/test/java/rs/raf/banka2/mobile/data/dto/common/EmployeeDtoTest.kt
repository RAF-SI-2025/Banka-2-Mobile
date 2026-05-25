package rs.raf.banka2.mobile.data.dto.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ME-12 tests: EmployeeDto derived flagovi (isAgent/isSupervisor/isAdmin) podrzavaju
 * dva BE formata:
 *   1) Legacy: BE eksplicitno postavi `isAdmin: true` / `isSupervisor: false` flagove.
 *   2) Novi: BE salje samo `permissions: ["ADMIN", "SUPERVISOR", ...]`.
 *
 * Derived getter handluje oba — flag override-uje permission list ako je setovan.
 */
class EmployeeDtoTest {

    @Test
    fun derivedIsAdmin_fromPermissionsList() {
        val emp = EmployeeDto(
            id = 1L,
            email = "admin@b.rs",
            permissions = listOf("ADMIN", "SUPERVISOR")
        )
        assertTrue(emp.derivedIsAdmin)
        assertTrue(emp.derivedIsSupervisor)
        assertFalse(emp.derivedIsAgent)
    }

    @Test
    fun derivedIsSupervisor_fromPermissionsList() {
        val emp = EmployeeDto(
            id = 1L,
            email = "sup@b.rs",
            permissions = listOf("SUPERVISOR")
        )
        assertFalse(emp.derivedIsAdmin)
        assertTrue(emp.derivedIsSupervisor)
        assertFalse(emp.derivedIsAgent)
    }

    @Test
    fun derivedIsAgent_fromPermissionsList() {
        val emp = EmployeeDto(
            id = 1L,
            email = "agent@b.rs",
            permissions = listOf("AGENT")
        )
        assertFalse(emp.derivedIsAdmin)
        assertFalse(emp.derivedIsSupervisor)
        assertTrue(emp.derivedIsAgent)
    }

    @Test
    fun explicitFlag_overridesPermissionList() {
        // Legacy BE moze setovati flag a NE poslati permissions list-u.
        val emp = EmployeeDto(
            id = 1L,
            email = "admin@b.rs",
            isAdmin = true,
            permissions = null
        )
        assertTrue(emp.derivedIsAdmin)
        // isSupervisor je null, isAdmin true — derivedIsSupervisor proverava permissions
        // ako flag fali; permissions je null → vraca false.
        assertFalse(emp.derivedIsSupervisor)
    }

    @Test
    fun caseInsensitivePermissionMatch() {
        val emp = EmployeeDto(
            id = 1L,
            email = "test@b.rs",
            permissions = listOf("admin")  // lowercase
        )
        assertTrue(emp.derivedIsAdmin)
    }

    @Test
    fun emptyPermissions_allFalse() {
        val emp = EmployeeDto(id = 1L, email = "user@b.rs", permissions = emptyList())
        assertFalse(emp.derivedIsAdmin)
        assertFalse(emp.derivedIsSupervisor)
        assertFalse(emp.derivedIsAgent)
    }

    @Test
    fun nullPermissions_handledSafely() {
        val emp = EmployeeDto(id = 1L, email = "user@b.rs", permissions = null)
        assertFalse(emp.derivedIsAdmin)
        assertFalse(emp.derivedIsSupervisor)
        assertFalse(emp.derivedIsAgent)
    }
}
