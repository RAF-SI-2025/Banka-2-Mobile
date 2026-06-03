package rs.raf.banka2.mobile.feature.accounts

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import java.math.BigDecimal

/**
 * TEST-mobile-banking-vm-1 (R4-1364-accounts): 0-test karakterizacioni baseline za
 * [AccountsListViewModel]. Pinuje init load happy/failure i da VM state cuva
 * AccountDto polja (accountSubtype/reservedAmount/companyName/effectiveReserved)
 * koja ekran prikazuje. Moshi field-parsing je pokriven u AccountDtoTest; ovde je
 * VM-state karakterizacija.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountsListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AccountRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsAccounts_happyPath() = runTest(dispatcher) {
        coEvery { repository.getMyAccounts() } returns ApiResult.Success(
            listOf(
                AccountDto(
                    id = 1L,
                    accountNumber = "222-RSD",
                    currency = "RSD",
                    balance = BigDecimal("1000"),
                    availableBalance = BigDecimal("800"),
                    reservedAmount = BigDecimal("200"),
                    accountSubtype = "TEKUCI",
                    status = "ACTIVE"
                )
            )
        )
        val vm = AccountsListViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertNull(state.error)
        assertEquals(1, state.accounts.size)
        val acc = state.accounts[0]
        assertEquals("TEKUCI", acc.accountSubtype)
        // effectiveReserved preferira reservedAmount polje (200), ne balance-available (1000-800).
        assertEquals(BigDecimal("200"), acc.effectiveReserved)
    }

    @Test
    fun effectiveReserved_fallsBackToBalanceMinusAvailable_whenFieldMissing() = runTest(dispatcher) {
        // reservedAmount=null (legacy zapis) → fallback (balance - availableBalance).
        coEvery { repository.getMyAccounts() } returns ApiResult.Success(
            listOf(
                AccountDto(
                    id = 2L,
                    accountNumber = "222-EUR",
                    currency = "EUR",
                    balance = BigDecimal("500"),
                    availableBalance = BigDecimal("450"),
                    reservedAmount = null
                )
            )
        )
        val vm = AccountsListViewModel(repository)
        advanceUntilIdle()

        assertEquals(BigDecimal("50"), vm.state.value.accounts[0].effectiveReserved)
    }

    @Test
    fun businessAccount_flaggedViaCompanyName() = runTest(dispatcher) {
        coEvery { repository.getMyAccounts() } returns ApiResult.Success(
            listOf(
                AccountDto(
                    id = 3L,
                    accountNumber = "222-BIZ",
                    currency = "RSD",
                    companyName = "Pera DOO"
                )
            )
        )
        val vm = AccountsListViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.state.value.accounts[0].isBusiness)
    }

    @Test
    fun refresh_failure_setsErrorAndClearsLoading() = runTest(dispatcher) {
        coEvery { repository.getMyAccounts() } returns ApiResult.Failure(
            ApiError(httpCode = 401, message = "Niste prijavljeni.", kind = ApiError.Kind.Unauthorized)
        )
        val vm = AccountsListViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals("Niste prijavljeni.", state.error)
        assertTrue(state.accounts.isEmpty())
    }
}
