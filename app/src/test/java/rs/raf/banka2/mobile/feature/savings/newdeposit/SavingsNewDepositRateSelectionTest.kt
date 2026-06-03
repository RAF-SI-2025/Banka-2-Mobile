package rs.raf.banka2.mobile.feature.savings.newdeposit

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
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.savings.SavingsRateDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.SavingsRepository
import java.math.BigDecimal

/**
 * TEST-mobile-banking-vm-1 (R4-1364-savingsrate): per-currency / per-term selekcija
 * kamatne stope u [SavingsNewDepositViewModel].
 *
 * Pinuje:
 *  - loadRates filtrira na active=true (neaktivne stope se odbacuju)
 *  - `annualRate` derived getter bira stopu po izabranom termMonths
 *  - setSource(currency) ponovno ucitava stope za valutu izabranog racuna
 *    (getRates pozvan sa tom valutom) — per-currency selekcija
 *  - minAmount prati valutu izvornog racuna
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavingsNewDepositRateSelectionTest {

    private val dispatcher = StandardTestDispatcher()
    private val savingsRepository = mockk<SavingsRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>()

    private val rsdAccount = AccountDto(id = 1L, accountNumber = "222-RSD", currency = "RSD")
    private val eurAccount = AccountDto(id = 2L, accountNumber = "222-EUR", currency = "EUR")

    private fun rate(currency: String, term: Int, annual: String, active: Boolean = true) =
        SavingsRateDto(
            id = (currency.hashCode() + term).toLong(),
            currencyCode = currency,
            termMonths = term,
            annualRate = BigDecimal(annual),
            active = active,
            effectiveFrom = "2026-01-01"
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(
            listOf(rsdAccount, eurAccount)
        )
        // default: RSD stope (3 i 12 meseci) + jedna NEAKTIVNA
        coEvery { savingsRepository.getRates(any()) } returns ApiResult.Success(
            listOf(
                rate("RSD", term = 3, annual = "0.04"),
                rate("RSD", term = 12, annual = "0.06"),
                rate("RSD", term = 24, annual = "0.99", active = false)
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = SavingsNewDepositViewModel(savingsRepository, accountRepository)

    @Test
    fun loadRates_keepsOnlyActiveRates() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val terms = vm.state.value.rates.map { it.termMonths }.toSet()
        // 3 i 12 ostaju; 24 (neaktivna) se odbacuje
        assertEquals(setOf(3, 12), terms)
    }

    @Test
    fun annualRate_selectedByTermMonths() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.setTerm(12)
        assertEquals(BigDecimal("0.06"), vm.state.value.annualRate)

        vm.setTerm(3)
        assertEquals(BigDecimal("0.04"), vm.state.value.annualRate)
    }

    @Test
    fun annualRate_zeroWhenNoMatchingTerm() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.setTerm(36) // nema 36-mesecnu stopu
        assertEquals(BigDecimal.ZERO, vm.state.value.annualRate)
    }

    @Test
    fun setSource_reloadsRatesForSelectedAccountCurrency() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.setSource(2L) // EUR racun
        advanceUntilIdle()

        // getRates mora biti pozvan sa "EUR" za izabrani racun (per-currency selekcija)
        coVerify { savingsRepository.getRates("EUR") }
        assertEquals(2L, vm.state.value.sourceAccountId)
        assertEquals("EUR", vm.state.value.currencyCode)
    }

    @Test
    fun minAmount_followsSourceCurrency() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.setSource(2L) // EUR → min 100
        advanceUntilIdle()
        assertEquals(BigDecimal("100"), vm.state.value.minAmount)

        vm.setSource(1L) // RSD → min 10000
        advanceUntilIdle()
        assertEquals(BigDecimal("10000"), vm.state.value.minAmount)
    }
}
