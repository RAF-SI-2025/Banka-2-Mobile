package rs.raf.banka2.mobile.feature.transfers.history

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.transfer.TransferResponseDto
import rs.raf.banka2.mobile.data.repository.TransferRepository
import java.math.BigDecimal

/**
 * TEST-mobile-banking-vm-1 (R4-1364-histories): 0-test karakterizacioni baseline za
 * [TransferHistoryViewModel] (PaymentHistory je dobio filter-testove u P2, ali
 * TransferHistory nije imao nijedan). Pinuje init load happy/failure i da VM cuva
 * BE-aliasirana polja (fromAccount/toAccount/convertedAmount preko @Json mape).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransferHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<TransferRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsTransfers_happyPath() = runTest(dispatcher) {
        coEvery { repository.listMyTransfers() } returns ApiResult.Success(
            listOf(
                TransferResponseDto(
                    id = 1L,
                    fromAccount = "222-RSD",
                    toAccount = "265-EUR",
                    amount = BigDecimal("10000"),
                    currency = "RSD",
                    convertedAmount = BigDecimal("85.20"),
                    rate = BigDecimal("117.37"),
                    fee = BigDecimal("100"),
                    status = "COMPLETED"
                )
            )
        )
        val vm = TransferHistoryViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertNull(state.error)
        assertEquals(1, state.transfers.size)
        val t = state.transfers[0]
        assertEquals("222-RSD", t.fromAccount)
        assertEquals("265-EUR", t.toAccount)
        assertEquals(BigDecimal("85.20"), t.convertedAmount)
        assertEquals("COMPLETED", t.status)
    }

    @Test
    fun init_callsListMyTransfersWithoutAccountFilter() = runTest(dispatcher) {
        coEvery { repository.listMyTransfers() } returns ApiResult.Success(emptyList())
        val vm = TransferHistoryViewModel(repository)
        advanceUntilIdle()

        // VM ne prosledjuje accountNumber filter (null default)
        coVerify(exactly = 1) { repository.listMyTransfers(null) }
        assertTrue(vm.state.value.transfers.isEmpty())
    }

    @Test
    fun refresh_failure_setsErrorAndClearsLoading() = runTest(dispatcher) {
        coEvery { repository.listMyTransfers() } returns ApiResult.Failure(
            ApiError(httpCode = 500, message = "Greska na serveru.", kind = ApiError.Kind.Server)
        )
        val vm = TransferHistoryViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals("Greska na serveru.", state.error)
        assertTrue(state.transfers.isEmpty())
    }
}
