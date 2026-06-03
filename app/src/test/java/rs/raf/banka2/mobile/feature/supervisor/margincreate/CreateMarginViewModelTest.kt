package rs.raf.banka2.mobile.feature.supervisor.margincreate

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.margin.MarginAccountDto
import rs.raf.banka2.mobile.data.repository.MarginRepository
import java.math.BigDecimal

/**
 * CreateMarginViewModel — validacija forme za otvaranje margin racuna +
 * happy/error putanje repo poziva. VM nema I/O u init-u pa je determinizam pun.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateMarginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<MarginRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vmWithValidForm(): CreateMarginViewModel {
        val vm = CreateMarginViewModel(repository)
        vm.setAccountId("17")              // R1-202: bazni RSD racun (BE @NotNull)
        vm.setInitialMargin("100000")
        vm.setMaintenanceMargin("50000")
        vm.setBankParticipation("0,5")     // sr-RS zarez za decimale
        vm.setUserId("7")
        return vm
    }

    @Test
    fun submit_missingAccountId_setsError_andDoesNotCallRepo() = runTest(dispatcher) {
        val vm = CreateMarginViewModel(repository)
        vm.setInitialMargin("100000")
        vm.setMaintenanceMargin("50000")
        vm.setBankParticipation("0,5")
        vm.setUserId("7")

        vm.submit()
        advanceUntilIdle()

        assertEquals("ID baznog (RSD) racuna je obavezan.", vm.state.value.error)
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun submit_missingInitialMargin_setsError_andDoesNotCallRepo() = runTest(dispatcher) {
        val vm = CreateMarginViewModel(repository)
        vm.setAccountId("17")
        vm.setMaintenanceMargin("50000")
        vm.setBankParticipation("0,5")
        vm.setUserId("7")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Initial margin je obavezan i pozitivan.", vm.state.value.error)
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun submit_participationOutOfRange_setsError() = runTest(dispatcher) {
        val vm = CreateMarginViewModel(repository)
        vm.setAccountId("17")
        vm.setInitialMargin("100000")
        vm.setMaintenanceMargin("50000")
        vm.setBankParticipation("2")       // > 1
        vm.setUserId("7")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Bank participation mora biti u [0..1] (0.5 = 50%).", vm.state.value.error)
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun submit_bothUserAndCompany_setsError() = runTest(dispatcher) {
        val vm = vmWithValidForm()
        vm.setCompanyId("9")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Unesi samo userId ILI companyId, ne oba.", vm.state.value.error)
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun submit_validForm_callsRepoWithParsedValues_andEmitsCreated() = runTest(dispatcher) {
        coEvery { repository.create(17L, BigDecimal("100000"), BigDecimal("50000"), BigDecimal("0.5"), 7L, null) } returns
            ApiResult.Success(MarginAccountDto(id = 55L))

        val vm = vmWithValidForm()

        val collected = mutableListOf<CreateMarginEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.submit()
        advanceUntilIdle()
        job.cancel()

        coVerify(exactly = 1) { repository.create(17L, BigDecimal("100000"), BigDecimal("50000"), BigDecimal("0.5"), 7L, null) }
        assertFalse(vm.state.value.submitting)
        assertEquals(null, vm.state.value.error)
        val created = collected.filterIsInstance<CreateMarginEvent.Created>().firstOrNull()
        assertTrue("expected Created event, got=$collected", created != null)
        assertEquals(55L, created!!.accountId)
    }

    @Test
    fun submit_repoFailure_setsErrorMessage_andStopsSubmitting() = runTest(dispatcher) {
        coEvery { repository.create(any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Failure(ApiError(httpCode = 409, message = "Klijent vec ima margin racun", kind = ApiError.Kind.Conflict))

        val vm = vmWithValidForm()
        vm.submit()
        advanceUntilIdle()

        assertEquals("Klijent vec ima margin racun", vm.state.value.error)
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun setUserId_keepsDigitsOnly() = runTest(dispatcher) {
        val vm = CreateMarginViewModel(repository)
        vm.setUserId("7a8b")
        assertEquals("78", vm.state.value.userId)
    }
}
