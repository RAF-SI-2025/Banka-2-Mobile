package rs.raf.banka2.mobile.feature.payments.history

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
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.repository.PaymentRepository

/**
 * R1-585: PaymentHistory je hardkodirao page=0/limit=50 BEZ ikakvog filtera —
 * korisnik nije mogao da suzi listu po racunu/statusu. VM sada prosledjuje
 * aktivne filtere BE-u (repo vec podrzava accountNumber/status).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<PaymentRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery {
            repository.getMyPayments(any(), any(), any(), any())
        } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_noFilters_passesNull() = runTest(dispatcher) {
        PaymentHistoryViewModel(repository)
        advanceUntilIdle()

        coVerify {
            repository.getMyPayments(page = 0, limit = 50, accountNumber = null, status = null)
        }
    }

    @Test
    fun setAccountFilter_passedToRepository() = runTest(dispatcher) {
        val vm = PaymentHistoryViewModel(repository)
        advanceUntilIdle()

        vm.setAccountFilter("222000123")
        advanceUntilIdle()

        coVerify {
            repository.getMyPayments(page = 0, limit = 50, accountNumber = "222000123", status = null)
        }
    }

    @Test
    fun setStatusFilter_passedToRepository() = runTest(dispatcher) {
        val vm = PaymentHistoryViewModel(repository)
        advanceUntilIdle()

        vm.setStatusFilter("COMPLETED")
        advanceUntilIdle()

        coVerify {
            repository.getMyPayments(page = 0, limit = 50, accountNumber = null, status = "COMPLETED")
        }
    }

    @Test
    fun blankFilter_normalizedToNull() = runTest(dispatcher) {
        val vm = PaymentHistoryViewModel(repository)
        advanceUntilIdle()

        vm.setAccountFilter("   ")
        advanceUntilIdle()

        coVerify {
            repository.getMyPayments(page = 0, limit = 50, accountNumber = null, status = null)
        }
    }
}
