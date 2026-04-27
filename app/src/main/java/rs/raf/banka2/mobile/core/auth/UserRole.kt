package rs.raf.banka2.mobile.core.auth

/**
 * Role koje vidi UI sloj. Backend salje ADMIN/EMPLOYEE/CLIENT u JWT-u, a permisije
 * (ADMIN/SUPERVISOR/AGENT) dolaze iz `/employees?email=...` poziva posle login-a.
 *
 * Hijerarhija po Celini 1 spec-u:
 *  - ADMIN moze sve i automatski je SUPERVISOR
 *  - SUPERVISOR moze Orderi/Aktuari/Porez/ProfitBanke + sve sto i AGENT
 *  - AGENT moze portal racuna/kartica/klijenata, NE moze OTC
 *  - CLIENT je obican klijent banke
 */
enum class UserRole {
    Admin, Supervisor, Agent, Client, Unknown;

    val isEmployee: Boolean get() = this == Admin || this == Supervisor || this == Agent
    val isAdmin: Boolean get() = this == Admin
    val isSupervisor: Boolean get() = this == Admin || this == Supervisor
    val isAgent: Boolean get() = this == Agent
    val isClient: Boolean get() = this == Client
    val canAccessOtc: Boolean get() = this == Admin || this == Supervisor || this == Client
}

/**
 * Permisije pristigle iz `GET /employees?email=...`. Ako api/zaposleni ima
 * `ADMIN` u listi → role je Admin; ako ima `SUPERVISOR` ali ne ADMIN → Supervisor;
 * inace Agent. Klijenti nemaju employees zapis — direktno mapiraju u Client.
 */
object RoleMapper {
    fun fromJwtRole(jwtRole: String?, permissions: Set<String>): UserRole {
        return when (jwtRole?.uppercase()) {
            "ADMIN", "EMPLOYEE" -> when {
                permissions.any { it.equals("ADMIN", ignoreCase = true) } -> UserRole.Admin
                permissions.any { it.equals("SUPERVISOR", ignoreCase = true) } -> UserRole.Supervisor
                else -> UserRole.Agent
            }
            "CLIENT" -> UserRole.Client
            else -> UserRole.Unknown
        }
    }
}
