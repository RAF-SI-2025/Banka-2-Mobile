package rs.raf.banka2.mobile.feature.funds.create

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rs.raf.banka2.mobile.core.network.ApiError
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.fund.FundDetailDto
import rs.raf.banka2.mobile.data.repository.FundRepository
import java.math.BigDecimal

/**
 * TEST-mobile-trading-vm-1 (OT-1255 sample): karakterizacioni baseline za
 * [CreateFundViewModel] (nepokriven ekran). Pinuje klijent-side validaciju:
 *  - prazan naziv → error, BEZ network poziva
 *  - min ulog null/<=0 → error, BEZ network poziva
 *  - validan unos → repository.create sa trimovanim imenom + parsiranim min, emit Created
 *  - decimal-comma min ("1.000,50") parsira se ispravno (sr-RS), ne gubi novac
 *  - BE neuspeh → error
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateFundViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<FundRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = CreateFundViewModel(repository)

    @Test
    fun submit_blankName_setsError_noNetwork() = runTest(dispatcher) {
        val vm = vm()
        vm.setMinContribution("1000")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Naziv fonda je obavezan.", vm.state.value.error)
        coVerify(exactly = 0) { repository.create(any(), any(), any()) }
    }

    @Test
    fun submit_zeroMinContribution_setsError_noNetwork() = runTest(dispatcher) {
        val vm = vm()
        vm.setName("Tech Fund")
        vm.setMinContribution("0")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Min ulog mora biti veci od 0.", vm.state.value.error)
        coVerify(exactly = 0) { repository.create(any(), any(), any()) }
    }

    @Test
    fun submit_invalidMinContribution_setsError_noNetwork() = runTest(dispatcher) {
        val vm = vm()
        vm.setName("Tech Fund")
        vm.setMinContribution("abc")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Min ulog mora biti veci od 0.", vm.state.value.error)
        coVerify(exactly = 0) { repository.create(any(), any(), any()) }
    }

    @Test
    fun submit_valid_callsCreate_withTrimmedName_andParsedMin_emitsCreated() = runTest(dispatcher) {
        val nameSlot = slot<String>()
        val minSlot = slot<BigDecimal>()
        coEvery { repository.create(capture(nameSlot), any(), capture(minSlot)) } returns
            ApiResult.Success(FundDetailDto(id = 88L, name = "Tech Fund"))

        val vm = vm()
        vm.setName("  Tech Fund  ")
        vm.setDescription("desc")
        vm.setMinContribution("5000")

        val collected = mutableListOf<CreateFundEvent>()
        val job = CoroutineScope(dispatcher).launch { vm.events.collect { collected.add(it) } }

        vm.submit()
        advanceUntilIdle()
        job.cancel()

        assertEquals("Tech Fund", nameSlot.captured)             // trimovano
        assertEquals(BigDecimal("5000"), minSlot.captured)
        assertTrue(collected.any { it is CreateFundEvent.Created && it.fundId == 88L })
        assertNull(vm.state.value.error)
    }

    @Test
    fun submit_decimalCommaMin_parsedCorrectly() = runTest(dispatcher) {
        // sr-RS unos "1.000,50" → 1000.50 (grouping tacke + decimalni zarez), ne 1.0.
        val minSlot = slot<BigDecimal>()
        coEvery { repository.create(any(), any(), capture(minSlot)) } returns
            ApiResult.Success(FundDetailDto(id = 1L, name = "F"))

        val vm = vm()
        vm.setName("F")
        vm.setMinContribution("1.000,50")

        vm.submit()
        advanceUntilIdle()

        assertEquals(0, BigDecimal("1000.50").compareTo(minSlot.captured))
    }

    @Test
    fun submit_backendFailure_setsError() = runTest(dispatcher) {
        coEvery { repository.create(any(), any(), any()) } returns ApiResult.Failure(
            ApiError(httpCode = 403, message = "Samo supervizor moze kreirati fond.", kind = ApiError.Kind.Forbidden)
        )
        val vm = vm()
        vm.setName("F")
        vm.setMinContribution("1000")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Samo supervizor moze kreirati fond.", vm.state.value.error)
    }
}
