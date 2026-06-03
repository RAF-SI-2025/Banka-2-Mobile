package rs.raf.banka2.mobile.feature.otc.contracts

import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.storage.OtcStateStore
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.OtcRepository
import java.math.BigDecimal

/**
 * P1-mobile-trading-1:
 *  - R1-271: COMPENSATING saga status NIJE terminal — polling mora nastaviti.
 *  - R1-213: myRole se derivira na klijentu iz buyerId/sellerId (BE ga ne salje).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OtcOffersAndContractsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<OtcRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val store = mockk<OtcStateStore>(relaxed = true)
    private val sessionManager = mockk<SessionManager>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { store.lastEntrance(any(), any()) } returns 0L
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loggedIn(userId: Long) = SessionState.LoggedIn(
        UserProfile(
            id = userId, email = "u@b.rs", firstName = "U", lastName = "B",
            role = UserRole.Client, permissions = emptySet(), canTradeStocks = true
        )
    )

    private fun makeVm(userId: Long): OtcOffersAndContractsViewModel {
        every { sessionManager.state } returns MutableStateFlow(loggedIn(userId))
        // init() poziva refresh() (default tab Offers) — vrati praznu listu.
        coEvery { repository.listOffers(any()) } returns ApiResult.Success(emptyList())
        coEvery { repository.listContracts(any()) } returns ApiResult.Success(emptyList())
        return OtcOffersAndContractsViewModel(repository, accountRepository, store, sessionManager)
    }

    private fun account(id: Long, currency: String) = AccountDto(
        id = id, accountNumber = "ACC$id", currency = currency,
        balance = BigDecimal.TEN, availableBalance = BigDecimal.TEN, status = "ACTIVE"
    )

    @Test
    fun contracts_deriveMyRoleFromBuyerId() = runTest(dispatcher) {
        val vm = makeVm(userId = 7L)
        advanceUntilIdle()
        coEvery { repository.listContracts(false) } returns ApiResult.Success(
            listOf(
                OtcContractDto(
                    id = 1, listingId = 5, quantity = 10, strikePrice = 100.0,
                    premium = 5.0, status = "ACTIVE", buyerId = 7L, sellerId = 42L
                )
            )
        )
        vm.setTab(OtcTab.ContractsDomestic)
        advanceUntilIdle()

        assertEquals("BUYER", vm.state.value.contracts.first().myRole)
    }

    @Test
    fun contracts_inter_doesNotDeriveRole() = runTest(dispatcher) {
        val vm = makeVm(userId = 7L)
        advanceUntilIdle()
        coEvery { repository.listContracts(true) } returns ApiResult.Success(
            listOf(
                OtcContractDto(
                    id = 1, listingId = 5, quantity = 10, strikePrice = 100.0,
                    premium = 5.0, status = "ACTIVE", foreign = true
                )
            )
        )
        vm.setTab(OtcTab.ContractsForeign)
        advanceUntilIdle()

        assertNull(vm.state.value.contracts.first().myRole)
    }

    @Test
    fun applySagaStatus_compensating_isNotTerminal_keepsPolling() = runTest(dispatcher) {
        val vm = makeVm(userId = 7L)
        advanceUntilIdle()

        // startExercise -> exerciseIntra vraca COMPENSATING (non-terminal) sa sagaId;
        // pollIntraSaga nastavlja da pita dok ne stigne terminal.
        coEvery { repository.exerciseIntra(any(), any()) } returns ApiResult.Success(
            rs.raf.banka2.mobile.data.dto.otc.OtcExerciseResultDto(
                sagaId = "s1", sagaStatus = "COMPENSATING", currentStep = 3, id = 1L, status = "ACTIVE"
            )
        )
        // prvi poll: jos COMPENSATING; drugi: COMPENSATED (terminal -> ABORTED)
        coEvery { repository.sagaStatusIntra("s1") } returnsMany listOf(
            ApiResult.Success(rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto("s1", "COMPENSATING", 3, emptyList())),
            ApiResult.Success(rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto("s1", "COMPENSATED", 3, emptyList()))
        )

        val contract = OtcContractDto(
            id = 1, listingId = 5, quantity = 10, strikePrice = 100.0,
            premium = 5.0, status = "ACTIVE", buyerId = 7L, sellerId = 42L
        )
        vm.startExercise(contract, buyerAccountId = 9L)
        advanceUntilIdle()

        // Krajnji ishod posle COMPENSATED je ABORTED — ali kljucno je da je polling
        // nastavio kroz COMPENSATING (ne stao prerano). Posle terminala faza = ABORTED.
        assertEquals("ABORTED", vm.state.value.exerciseInProgress?.phase)
    }

    // ─── R1-593: exercise/accept moraju poslati realni buyerAccountId, ne null ───

    @Test
    fun exercise_sendsSelectedAccountId_notNull() = runTest(dispatcher) {
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(
            listOf(account(11L, "EUR"), account(22L, "RSD"))
        )
        val vm = makeVm(userId = 7L)
        advanceUntilIdle()

        coEvery { repository.exerciseIntra(any(), any()) } returns ApiResult.Success(
            rs.raf.banka2.mobile.data.dto.otc.OtcExerciseResultDto(
                sagaId = null, sagaStatus = "COMPLETED", currentStep = 5, id = 1L, status = "EXERCISED"
            )
        )

        val contract = OtcContractDto(
            id = 1, listingId = 5, quantity = 10, strikePrice = 100.0,
            premium = 5.0, status = "ACTIVE", buyerId = 7L, sellerId = 42L
        )
        // Screen prosledjuje state.selectedAccountId (RSD default = 22L).
        vm.startExercise(contract, vm.state.value.selectedAccountId)
        advanceUntilIdle()

        // KLJUCNO: BE dobija 22L (RSD default), NE null (R1-593).
        coVerify { repository.exerciseIntra(contract, 22L) }
        assertEquals(22L, vm.state.value.selectedAccountId)
    }

    @Test
    fun accept_usesSelectedAccountId_whenNotProvided() = runTest(dispatcher) {
        coEvery { accountRepository.getMyAccounts() } returns ApiResult.Success(
            listOf(account(33L, "RSD"))
        )
        val vm = makeVm(userId = 7L)
        advanceUntilIdle()

        val offer = OtcOfferDto(
            id = 5, listingId = 5, quantity = 10, pricePerStock = 100.0,
            premium = 5.0, status = "ACTIVE", buyerId = 7L, sellerId = 42L, myTurn = true
        )
        coEvery { repository.accept(any(), any(), any()) } returns ApiResult.Success(offer)

        vm.acceptOffer(offer)
        advanceUntilIdle()

        coVerify { repository.accept(false, offer, 33L) }
    }

    // ─── R1-479: abandon ugovora ───

    @Test
    fun abandonContract_callsRepository_andRefreshes() = runTest(dispatcher) {
        val vm = makeVm(userId = 7L)
        advanceUntilIdle()

        val contract = OtcContractDto(
            id = 9, listingId = 5, quantity = 10, strikePrice = 100.0,
            premium = 5.0, status = "ACTIVE", buyerId = 7L, sellerId = 42L
        )
        coEvery { repository.abandonContract(contract) } returns ApiResult.Success(
            contract.copy(status = "ABANDONED")
        )

        vm.abandonContract(contract)
        advanceUntilIdle()

        coVerify { repository.abandonContract(contract) }
    }
}
