package rs.raf.banka2.mobile.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import rs.raf.banka2.mobile.core.auth.JwtDecoder
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.storage.AuthStore
import rs.raf.banka2.mobile.data.api.AuthApi
import rs.raf.banka2.mobile.data.api.ClientApi
import rs.raf.banka2.mobile.data.api.EmployeeApi
import rs.raf.banka2.mobile.data.dto.auth.LoginResponse
import rs.raf.banka2.mobile.data.dto.common.ClientDto
import rs.raf.banka2.mobile.data.dto.common.EmployeeDto
import rs.raf.banka2.mobile.data.dto.common.PageResponse

/**
 * R4-1364-authrepo: AuthRepository login profile-resolve grane.
 *
 *  - CLIENT login: ide kroz `/clients/me` (P1-fe-mobile-authz-1) — NE `/clients?email=`.
 *    Uspeh → pravi id + canTradeStocks iz BE. Failure → FAIL-CLOSED (canTradeStocks=false).
 *  - EMPLOYEE login: `/employees?email=` transient pad (5xx/network) NE rusi login —
 *    R1-583 degradiran profil bez permisija (JWT je autoritativan za rolu).
 *
 * `JwtDecoder` koristi `android.util.Base64` koji baca "Stub!" u plain JVM unit
 * testu (suite nema Robolectric) — zato `mockkObject(JwtDecoder)` stub-uje decode().
 * Time se test fokusira na AuthRepository profil-resolve logiku (ne na JWT parsiranje
 * koje je odvojeno pokriveno tamo gde Base64 nije u igri).
 */
class AuthRepositoryTest {

    private val authApi = mockk<AuthApi>()
    private val employeeApi = mockk<EmployeeApi>()
    private val clientApi = mockk<ClientApi>()
    private val authStore = mockk<AuthStore>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)

    private val repo = AuthRepository(authApi, employeeApi, clientApi, authStore, sessionManager)

    private val accessToken = "header.payload.sig"

    @Before
    fun setUp() {
        mockkObject(JwtDecoder)
    }

    @After
    fun tearDown() {
        unmockkObject(JwtDecoder)
    }

    private fun stubDecode(role: String, email: String) {
        every { JwtDecoder.decode(any()) } returns
            JwtDecoder.Payload(sub = email, role = role, active = true, exp = 9_999_999_999L)
    }

    private fun loginResponse() = Response.success(
        LoginResponse(accessToken = accessToken, refreshToken = "refresh")
    )

    // ── CLIENT login → /clients/me (NE /clients?email=) ──────────────────────

    @Test
    fun clientLogin_usesClientsMe_resolvesIdAndCanTradeStocks() = runTest {
        stubDecode("CLIENT", "stefan@gmail.com")
        coEvery { authApi.login(any()) } returns loginResponse()
        coEvery { clientApi.me() } returns Response.success(
            ClientDto(id = 77L, email = "stefan@gmail.com", firstName = "Stefan", lastName = "J", canTradeStocks = true)
        )

        val result = repo.login("stefan@gmail.com", "pass")

        assertTrue(result is ApiResult.Success)
        val profile = (result as ApiResult.Success).data
        assertEquals(UserRole.Client, profile.role)
        assertEquals(77L, profile.id)
        assertTrue(profile.canTradeStocks)
        // KLJUCNO: CLIENT ne sme da zove employees ni `/clients?email=` (403) — samo `/me`.
        coVerify(exactly = 1) { clientApi.me() }
        coVerify(exactly = 0) { employeeApi.searchByEmail(any(), any(), any()) }
        coVerify(exactly = 0) { clientApi.list(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun clientLogin_meReturnsCanTradeFalse_propagatesFalse() = runTest {
        stubDecode("CLIENT", "lazar@yahoo.com")
        coEvery { authApi.login(any()) } returns loginResponse()
        coEvery { clientApi.me() } returns Response.success(
            ClientDto(id = 88L, email = "lazar@yahoo.com", canTradeStocks = false)
        )

        val profile = (repo.login("lazar@yahoo.com", "pass") as ApiResult.Success).data
        assertEquals(88L, profile.id)
        assertFalse("BE canTradeStocks=false mora da se postuje", profile.canTradeStocks)
    }

    @Test
    fun clientLogin_meReturnsNullCanTrade_defaultsTrue_legacyCompat() = runTest {
        stubDecode("CLIENT", "ana@hotmail.com")
        coEvery { authApi.login(any()) } returns loginResponse()
        coEvery { clientApi.me() } returns Response.success(
            ClientDto(id = 99L, email = "ana@hotmail.com", canTradeStocks = null)
        )

        val profile = (repo.login("ana@hotmail.com", "pass") as ApiResult.Success).data
        // legacy seed: null → default true (CLAUDE.md backwards-compat).
        assertTrue(profile.canTradeStocks)
    }

    @Test
    fun clientLogin_meFails403_failClosed_canTradeStocksFalse() = runTest {
        stubDecode("CLIENT", "stefan@gmail.com")
        coEvery { authApi.login(any()) } returns loginResponse()
        coEvery { clientApi.me() } returns
            Response.error(403, "Forbidden".toResponseBody("text/plain".toMediaTypeOrNull()))

        val result = repo.login("stefan@gmail.com", "pass")
        // login NE pada (JWT je vec izdat), ali FAIL-CLOSED: bez trade pristupa.
        assertTrue(result is ApiResult.Success)
        val profile = (result as ApiResult.Success).data
        assertEquals(UserRole.Client, profile.role)
        assertEquals(0L, profile.id)
        assertFalse("self-lookup fail → NE dodeljuj trade pristup", profile.canTradeStocks)
    }

    // ── EMPLOYEE login → /employees?email= transient pad NE rusi login ───────

    @Test
    fun employeeLogin_employeesTransientFailure_degradedProfile_loginStillSucceeds() = runTest {
        stubDecode("EMPLOYEE", "nikola.milenkovic@banka.rs")
        coEvery { authApi.login(any()) } returns loginResponse()
        // /employees privremeno pada (5xx) — R1-583: NE rusi login.
        coEvery { employeeApi.searchByEmail(any(), any(), any()) } returns
            Response.error(503, "Service Unavailable".toResponseBody("text/plain".toMediaTypeOrNull()))

        val result = repo.login("nikola.milenkovic@banka.rs", "pass")

        assertTrue("transient /employees pad NE sme da srusi login", result is ApiResult.Success)
        val profile = (result as ApiResult.Success).data
        // degradiran profil: nema permisija (RoleMapper EMPLOYEE bez perms → Agent),
        // ali sesija je uspostavljena (JWT autoritativan za pristup).
        assertTrue(profile.permissions.isEmpty())
        assertEquals(UserRole.Agent, profile.role)
        // zaposleni uvek moze da trguje (default true u ResolvedProfile()).
        assertTrue(profile.canTradeStocks)
        // sesija je zaista upisana u SessionManager.
        val stateSlot = slot<SessionState>()
        coVerify { sessionManager.update(capture(stateSlot)) }
        assertTrue(stateSlot.captured is SessionState.LoggedIn)
    }

    @Test
    fun employeeLogin_admin_resolvesPermissionsAndAdminRole() = runTest {
        stubDecode("ADMIN", "marko.petrovic@banka.rs")
        coEvery { authApi.login(any()) } returns loginResponse()
        coEvery { employeeApi.searchByEmail(any(), any(), any()) } returns Response.success(
            PageResponse(
                content = listOf(
                    EmployeeDto(
                        id = 3L,
                        email = "marko.petrovic@banka.rs",
                        firstName = "Marko",
                        lastName = "Petrovic",
                        permissions = listOf("ADMIN")
                    )
                )
            )
        )

        val profile = (repo.login("marko.petrovic@banka.rs", "pass") as ApiResult.Success).data
        assertEquals(UserRole.Admin, profile.role)
        assertEquals(3L, profile.id)
        assertTrue(profile.permissions.contains("ADMIN"))
        // CLIENT putanja se NE poziva za employee rolu.
        coVerify(exactly = 0) { clientApi.me() }
    }

    @Test
    fun login_authApiFails_propagatesFailure_noProfileResolve() = runTest {
        // login sam pada (401) — repo vraca Failure i NE pokusava profile resolve.
        coEvery { authApi.login(any()) } returns
            Response.error(401, "Bad credentials".toResponseBody("text/plain".toMediaTypeOrNull()))

        val result = repo.login("x@y.rs", "wrong")

        assertTrue(result is ApiResult.Failure)
        coVerify(exactly = 0) { clientApi.me() }
        coVerify(exactly = 0) { employeeApi.searchByEmail(any(), any(), any()) }
        coVerify(exactly = 0) { sessionManager.update(any<SessionState.LoggedIn>()) }
    }
}
