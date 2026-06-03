package rs.raf.banka2.mobile.feature.exchanges

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.ExchangeManagementRepository

/**
 * ExchangesViewModel — R1 752 role-gate za test-mode toggle.
 * Switch (canManageTestMode) sme samo admin/supervisor; ostali ga ne vide.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExchangesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ExchangeManagementRepository>()
    private val sessionManager = mockk<SessionManager>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.list() } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun session(role: UserRole): MutableStateFlow<SessionState> =
        MutableStateFlow(
            SessionState.LoggedIn(
                UserProfile(id = 1L, email = "u@b.rs", firstName = "U", lastName = "B", role = role, permissions = emptySet())
            )
        )

    private fun makeVm(role: UserRole): ExchangesViewModel {
        every { sessionManager.state } returns session(role)
        return ExchangesViewModel(repository, sessionManager)
    }

    @Test
    fun supervisor_canManageTestMode_true() = runTest(dispatcher) {
        val vm = makeVm(UserRole.Supervisor)
        advanceUntilIdle()
        assertTrue(vm.state.value.canManageTestMode)
    }

    @Test
    fun admin_canManageTestMode_true() = runTest(dispatcher) {
        val vm = makeVm(UserRole.Admin)
        advanceUntilIdle()
        assertTrue(vm.state.value.canManageTestMode)
    }

    @Test
    fun agent_canManageTestMode_false() = runTest(dispatcher) {
        val vm = makeVm(UserRole.Agent)
        advanceUntilIdle()
        assertFalse(vm.state.value.canManageTestMode)
    }

    @Test
    fun client_canManageTestMode_false() = runTest(dispatcher) {
        val vm = makeVm(UserRole.Client)
        advanceUntilIdle()
        assertFalse(vm.state.value.canManageTestMode)
    }
}
