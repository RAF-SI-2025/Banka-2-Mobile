package rs.raf.banka2.mobile.feature.margin

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.margin.MarginAccountDto
import rs.raf.banka2.mobile.data.dto.margin.MarginMessageDto
import rs.raf.banka2.mobile.data.repository.MarginRepository
import java.math.BigDecimal

/**
 * MarginViewModel — lista margin racuna (init refresh) + deposit/withdraw
 * koji na uspeh re-fetch-uju listu, a na gresku izlazu error u state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MarginViewModelTest {

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

    private fun acc(id: Long, loan: BigDecimal = BigDecimal.ZERO) =
        MarginAccountDto(id = id, initialMargin = BigDecimal("1000.0"), maintenanceMargin = BigDecimal("500.0"), loanValue = loan)

    @Test
    fun init_loadsAccounts() = runTest(dispatcher) {
        coEvery { repository.myAccounts() } returns ApiResult.Success(listOf(acc(1), acc(2)))

        val vm = MarginViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(2, state.accounts.size)
        assertEquals(null, state.error)
    }

    @Test
    fun init_failure_exposesError() = runTest(dispatcher) {
        coEvery { repository.myAccounts() } returns
            ApiResult.Failure(ApiError(httpCode = 500, message = "Server down", kind = ApiError.Kind.Server))

        val vm = MarginViewModel(repository)
        advanceUntilIdle()

        assertEquals("Server down", vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun deposit_success_refreshesAccounts() = runTest(dispatcher) {
        coEvery { repository.myAccounts() } returns ApiResult.Success(listOf(acc(1, loan = BigDecimal.ZERO)))
        val vm = MarginViewModel(repository)
        advanceUntilIdle()

        // deposit vraca {message} (MarginMessageDto), posle re-fetch vraca azurirani loanValue
        coEvery { repository.deposit(1L, BigDecimal("5000.0")) } returns ApiResult.Success(MarginMessageDto("Deposit successful"))
        coEvery { repository.myAccounts() } returns ApiResult.Success(listOf(acc(1, loan = BigDecimal("5000.0"))))

        vm.deposit(1L, BigDecimal("5000.0"))
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deposit(1L, BigDecimal("5000.0")) }
        assertEquals(BigDecimal("5000.0"), vm.state.value.accounts[0].loanValue)
    }

    @Test
    fun deposit_failure_setsError_withoutRefresh() = runTest(dispatcher) {
        coEvery { repository.myAccounts() } returns ApiResult.Success(listOf(acc(1)))
        val vm = MarginViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.deposit(1L, BigDecimal("1.0")) } returns
            ApiResult.Failure(ApiError(httpCode = 400, message = "Iznos mora biti pozitivan", kind = ApiError.Kind.Validation))

        vm.deposit(1L, BigDecimal("1.0"))
        advanceUntilIdle()

        assertEquals("Iznos mora biti pozitivan", vm.state.value.error)
        // myAccounts pozvan samo iz init-a (deposit fail ne refresh-uje)
        coVerify(exactly = 1) { repository.myAccounts() }
    }

    @Test
    fun withdraw_failure_setsError() = runTest(dispatcher) {
        coEvery { repository.myAccounts() } returns ApiResult.Success(listOf(acc(1)))
        val vm = MarginViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.withdraw(1L, BigDecimal("99999.0")) } returns
            ApiResult.Failure(ApiError(httpCode = 409, message = "Nedovoljno sredstava", kind = ApiError.Kind.Conflict))

        vm.withdraw(1L, BigDecimal("99999.0"))
        advanceUntilIdle()

        assertEquals("Nedovoljno sredstava", vm.state.value.error)
    }
}
