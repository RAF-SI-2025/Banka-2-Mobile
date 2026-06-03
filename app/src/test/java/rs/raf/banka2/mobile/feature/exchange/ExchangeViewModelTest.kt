package rs.raf.banka2.mobile.feature.exchange

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.exchange.CalculateExchangeResponseDto
import rs.raf.banka2.mobile.data.repository.ExchangeRepository
import java.math.BigDecimal

/**
 * R1-582: `recalc()` je za svaki keystroke pravio NOV coroutine bez otkazivanja
 * prethodnog → out-of-order odgovor moze da pregazi najnoviji kurs. Fix drzi
 * `recalcJob` i otkazuje ga pre novog poziva (latest-wins).
 * R1-369: ista valuta (from==to) ne ide na BE — prikazuje se 1:1.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ExchangeRepository>(relaxed = true)

    private fun resp(amount: String, rate: String) = CalculateExchangeResponseDto(
        fromCurrency = "RSD",
        toCurrency = "EUR",
        amount = BigDecimal("100"),
        convertedAmount = BigDecimal(amount),
        rate = BigDecimal(rate)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.rates() } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = ExchangeViewModel(repository)

    @Test
    fun sameCurrency_doesNotCallRepository() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.setTo("RSD") // from default RSD → to RSD (ista valuta)
        vm.setAmount("250")
        advanceUntilIdle()

        // R1-369: nikad ne zovemo calculate za istu valutu.
        coVerify(exactly = 0) { repository.calculate(any(), any(), any()) }
        assertNull(vm.state.value.calculation)
    }

    @Test
    fun rapidKeystrokes_latestWins() = runTest(dispatcher) {
        // Prvi poziv "visi" (deferred), drugi se vraca odmah. Ako se prvi NE otkaze,
        // njegov (zastareli) rezultat bi mogao da pregazi drugi.
        val firstGate = CompletableDeferred<ApiResult<CalculateExchangeResponseDto>>()
        coEvery { repository.calculate(100.0, "RSD", "EUR") } coAnswers { firstGate.await() }
        coEvery { repository.calculate(200.0, "RSD", "EUR") } returns ApiResult.Success(resp("1.70", "0.0085"))

        val vm = vm()
        advanceUntilIdle()

        vm.setAmount("100") // izdaje prvi (visi) job
        vm.setAmount("200") // otkazuje prvi, izdaje drugi
        advanceUntilIdle()

        // Drugi (najnoviji) rezultat je primenjen.
        assertEquals(BigDecimal("1.70"), vm.state.value.calculation?.convertedAmount)

        // Sada "kasni" odgovor prvog poziva — posto je job otkazan, ne sme da pregazi.
        firstGate.complete(ApiResult.Success(resp("0.85", "0.0085")))
        advanceUntilIdle()
        assertEquals(BigDecimal("1.70"), vm.state.value.calculation?.convertedAmount)
    }
}
