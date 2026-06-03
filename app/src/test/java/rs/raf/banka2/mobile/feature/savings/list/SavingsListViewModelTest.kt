package rs.raf.banka2.mobile.feature.savings.list

import io.mockk.coEvery
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
import rs.raf.banka2.mobile.data.dto.savings.SavingsDepositDto
import rs.raf.banka2.mobile.data.repository.SavingsRepository
import java.math.BigDecimal

/**
 * TEST-mobile-banking-vm-1 (R4-1364-savings): karakterizacija [SavingsListViewModel]
 * agregata + FX-mix probe.
 *
 * BUG-FOUND (R4-1364-savings, display/money): `totalInterestPaid` je sabirao
 * `totalInterestPaid` PREKO SVIH valuta (EUR+RSD+...) bez konverzije, ALI
 * `SavingsListScreen.kt:141` prikazuje taj zbir sa hard-kodiranim labelom "RSD".
 * EUR/USD kamata se sabirala u "RSD" ukupno → pogresan prikaz (mesanje valuta).
 * `totalPrincipalRsd` je vec pravilno filtrirao samo RSD; ovaj test pinuje da i
 * `totalInterestPaid` broji SAMO RSD depozite (isti, RSD-labelirani prikaz).
 * Fix je minimalan (display-only, bez kretanja novca) → primenjen TDD.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavingsListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<SavingsRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun deposit(
        id: Long,
        currency: String,
        principal: String,
        interest: String,
        status: String = "ACTIVE"
    ) = SavingsDepositDto(
        id = id,
        clientId = 1L,
        clientName = "Pera",
        linkedAccountId = 10L,
        linkedAccountNumber = "222-$currency",
        principalAmount = BigDecimal(principal),
        currencyCode = currency,
        termMonths = 12,
        annualInterestRate = BigDecimal("0.05"),
        startDate = "2026-01-01",
        maturityDate = "2027-01-01",
        nextInterestPaymentDate = "2026-02-01",
        totalInterestPaid = BigDecimal(interest),
        autoRenew = false,
        status = status,
        createdAt = null,
        updatedAt = null
    )

    @Test
    fun refresh_failure_setsError() = runTest(dispatcher) {
        coEvery { repository.listMy() } returns ApiResult.Failure(
            ApiError(httpCode = 500, message = "Greska na serveru.", kind = ApiError.Kind.Server)
        )
        val vm = SavingsListViewModel(repository)
        advanceUntilIdle()

        assertFalse(vm.state.value.loading)
        assertEquals("Greska na serveru.", vm.state.value.error)
    }

    @Test
    fun totalPrincipalRsd_sumsOnlyRsdDeposits() = runTest(dispatcher) {
        coEvery { repository.listMy() } returns ApiResult.Success(
            listOf(
                deposit(1, "RSD", principal = "100000", interest = "1000"),
                deposit(2, "RSD", principal = "50000", interest = "500"),
                deposit(3, "EUR", principal = "1000", interest = "20")
            )
        )
        val vm = SavingsListViewModel(repository)
        advanceUntilIdle()

        // 100000 + 50000 (EUR 1000 NE ulazi u RSD total)
        assertEquals(BigDecimal("150000"), vm.state.value.totalPrincipalRsd)
    }

    @Test
    fun totalInterestPaid_onlyRsd_doesNotMixForeignCurrencyIntoRsdLabel() = runTest(dispatcher) {
        // R4-1364-savings [BUG-FOUND→fixed]: ekran labelira total kao "RSD".
        // EUR kamata (20) NE sme da se sabere u RSD ukupno. Ocekivano: 1000 + 500 = 1500
        // (NE 1520, sto bi bio mix EUR+RSD pod RSD labelom).
        coEvery { repository.listMy() } returns ApiResult.Success(
            listOf(
                deposit(1, "RSD", principal = "100000", interest = "1000"),
                deposit(2, "RSD", principal = "50000", interest = "500"),
                deposit(3, "EUR", principal = "1000", interest = "20")
            )
        )
        val vm = SavingsListViewModel(repository)
        advanceUntilIdle()

        assertEquals(BigDecimal("1500"), vm.state.value.totalInterestPaid)
    }

    @Test
    fun activeCount_countsOnlyActiveStatus() = runTest(dispatcher) {
        coEvery { repository.listMy() } returns ApiResult.Success(
            listOf(
                deposit(1, "RSD", "100000", "1000", status = "ACTIVE"),
                deposit(2, "RSD", "50000", "500", status = "MATURED"),
                deposit(3, "RSD", "20000", "200", status = "ACTIVE")
            )
        )
        val vm = SavingsListViewModel(repository)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.activeCount)
    }

    @Test
    fun emptyList_aggregatesAreZero() = runTest(dispatcher) {
        coEvery { repository.listMy() } returns ApiResult.Success(emptyList())
        val vm = SavingsListViewModel(repository)
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(BigDecimal.ZERO, s.totalPrincipalRsd)
        assertEquals(BigDecimal.ZERO, s.totalInterestPaid)
        assertEquals(0, s.activeCount)
    }
}
