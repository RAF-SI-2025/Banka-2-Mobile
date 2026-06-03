package rs.raf.banka2.mobile.feature.savings.newdeposit

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
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.SavingsRepository
import java.math.BigDecimal

/**
 * P1-mobile-banking-1 (R7-2027): `setPrincipalText` je koristio
 * `String.toBigDecimalOrNull()` koji NE prepoznaje srpski zarez ("250.000,00")
 * → glavnica je tiho padala na ZERO i depozit bi isao sa nula glavnicom / pao
 * na min-iznos validaciji. Fix koristi MoneyFormatter.parseBigDecimal (sr-RS).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavingsNewDepositViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val savingsRepository = mockk<SavingsRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(
            listOf(AccountDto(id = 1L, accountNumber = "222-RSD", currency = "RSD"))
        )
        coEvery { savingsRepository.getRates(any()) } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = SavingsNewDepositViewModel(savingsRepository, accountRepository)

    @Test
    fun setPrincipalText_serbianComma_parsedAsDecimal() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.setPrincipalText("250.000,50")  // sr-RS: tacka=hiljade, zarez=decimala
        assertEquals(BigDecimal("250000.50"), vm.state.value.principalAmount)
    }

    @Test
    fun setPrincipalText_plainNumber_parsed() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.setPrincipalText("100000")
        assertEquals(BigDecimal("100000"), vm.state.value.principalAmount)
    }

    @Test
    fun setPrincipalText_blank_fallsBackToZero() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.setPrincipalText("abc")
        assertEquals(BigDecimal.ZERO, vm.state.value.principalAmount)
    }

    @Test
    fun setPrincipalText_serbianComma_notZeroNotInflated() = runTest(dispatcher) {
        // R7-2028 [money]: "1.234,56" mora postati 1234.56 — NE 0 (izgubljena
        // glavnica) i NE 123456 (1000×/100× zbog slepog strip-ovanja tacaka).
        val vm = vm()
        advanceUntilIdle()

        vm.setPrincipalText("1.234,56")
        val parsed = vm.state.value.principalAmount
        assertEquals(BigDecimal("1234.56"), parsed)
    }

    @Test
    fun setPrincipalText_enDotDecimal_notInflated() = runTest(dispatcher) {
        // R7-2033 [money]: tastatura na sr-RS Decimal moze proizvesti "5000.00";
        // tacka sa 2 cifre = decimala, NE grouping → ostaje 5000.00 (ne 500000).
        val vm = vm()
        advanceUntilIdle()

        vm.setPrincipalText("5000.00")
        assertEquals(BigDecimal("5000.00"), vm.state.value.principalAmount)
    }
}
