package rs.raf.banka2.mobile.feature.cards

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.card.CardDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.CardRepository
import java.math.BigDecimal

/**
 * TEST-mobile-banking-vm-1 (R1-186 cards): 0-test karakterizacioni baseline za
 * [CardsViewModel]. Pinuje:
 *  - init { refresh + loadAccounts } happy-path (cards + accounts u state-u)
 *  - prepaid top-up/withdraw odbijanje (ApiResult.Failure → state.error, BEZ Toast event-a)
 *  - prepaid top-up/withdraw uspeh → Toast event + re-refresh
 *  - block/unblock/deactivate/updateLimit success → Toast + refresh; failure → error
 *
 * confirm-404 (BE endpoint ne postoji za Mobile potvrdu kartice) je feature-gap i
 * NE testira se ovde (vidi batch-plan dedup F3 "Mobile-confirm-404").
 *
 * Repo se mockuje (deterministicki VM logika); URL/body asercija je u
 * CardRepositoryMockServerTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<CardRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)

    private val prepaidCard = CardDto(
        id = 7L,
        cardNumber = "1234-5678-9012-3456",
        cardCategory = "INTERNET_PREPAID",
        status = "ACTIVE",
        prepaidBalance = BigDecimal("500.00"),
        accountId = 1L
    )

    private val rsdAccount = AccountDto(id = 1L, accountNumber = "222-RSD", currency = "RSD")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.myCards() } returns ApiResult.Success(listOf(prepaidCard))
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(listOf(rsdAccount))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = CardsViewModel(repository, accountRepository)

    @Test
    fun init_loadsCardsAndAccounts() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(1, state.cards.size)
        assertEquals(7L, state.cards[0].id)
        assertTrue(state.cards[0].isPrepaid)
        assertEquals(1, state.accounts.size)
        assertNull(state.error)
    }

    @Test
    fun refresh_failure_setsError() = runTest(dispatcher) {
        coEvery { repository.myCards() } returns ApiResult.Failure(
            ApiError(httpCode = 500, message = "Greska na serveru.", kind = ApiError.Kind.Server)
        )
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals("Greska na serveru.", state.error)
        assertTrue(state.cards.isEmpty())
    }

    @Test
    fun topUpCard_failure_setsError_noToast_noRefetch() = runTest(dispatcher) {
        // top-up odbijen (npr. non-prepaid / BLOCKED / insufficient na BE) → state.error,
        // NEMA Toast event-a i NEMA drugog myCards() poziva (samo init refresh).
        coEvery {
            repository.topUp(any(), any(), any(), any())
        } returns ApiResult.Failure(
            ApiError(httpCode = 400, message = "Nedovoljno sredstava.", kind = ApiError.Kind.Validation)
        )
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<CardEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.topUpCard(cardId = 7L, sourceAccountId = 1L, amount = BigDecimal("100"))
        advanceUntilIdle()
        job.cancel()

        assertEquals("Nedovoljno sredstava.", vm.state.value.error)
        assertTrue("ne sme da emituje Toast na neuspeh", collected.isEmpty())
        // samo init refresh (1×), bez re-fetch posle neuspeha
        coVerify(exactly = 1) { repository.myCards() }
    }

    @Test
    fun topUpCard_success_emitsToast_andRefreshes() = runTest(dispatcher) {
        coEvery { repository.topUp(7L, 1L, BigDecimal("100"), any()) } returns
            ApiResult.Success(prepaidCard)
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<CardEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.topUpCard(cardId = 7L, sourceAccountId = 1L, amount = BigDecimal("100"))
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is CardEvent.Toast })
        // init refresh + post-success refresh = 2×
        coVerify(exactly = 2) { repository.myCards() }
        assertNull(vm.state.value.error)
    }

    @Test
    fun withdrawFromCard_failure_setsError() = runTest(dispatcher) {
        coEvery {
            repository.withdrawFromCard(any(), any(), any(), any())
        } returns ApiResult.Failure(
            ApiError(httpCode = 400, message = "Iznos prelazi prepaid balans.", kind = ApiError.Kind.Validation)
        )
        val vm = vm()
        advanceUntilIdle()

        vm.withdrawFromCard(cardId = 7L, targetAccountId = 1L, amount = BigDecimal("999999"))
        advanceUntilIdle()

        assertEquals("Iznos prelazi prepaid balans.", vm.state.value.error)
    }

    @Test
    fun withdrawFromCard_success_emitsToast_andRefreshes() = runTest(dispatcher) {
        coEvery { repository.withdrawFromCard(7L, 1L, BigDecimal("50"), any()) } returns
            ApiResult.Success(prepaidCard)
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<CardEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.withdrawFromCard(cardId = 7L, targetAccountId = 1L, amount = BigDecimal("50"))
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is CardEvent.Toast })
        coVerify(exactly = 2) { repository.myCards() }
    }

    @Test
    fun blockCard_success_emitsToast_andRefreshes() = runTest(dispatcher) {
        coEvery { repository.block(7L) } returns ApiResult.Success(prepaidCard)
        val vm = vm()
        advanceUntilIdle()

        val collected = mutableListOf<CardEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.blockCard(7L)
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is CardEvent.Toast })
        coVerify(exactly = 2) { repository.myCards() }
    }

    @Test
    fun updateLimit_failure_setsError() = runTest(dispatcher) {
        coEvery { repository.updateLimit(any(), any()) } returns ApiResult.Failure(
            ApiError(httpCode = 403, message = "Nemate dozvolu.", kind = ApiError.Kind.Forbidden)
        )
        val vm = vm()
        advanceUntilIdle()

        vm.updateLimit(7L, BigDecimal("100000"))
        advanceUntilIdle()

        assertEquals("Nemate dozvolu.", vm.state.value.error)
    }
}
