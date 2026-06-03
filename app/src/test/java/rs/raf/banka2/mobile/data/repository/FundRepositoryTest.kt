package rs.raf.banka2.mobile.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.api.FundApi
import rs.raf.banka2.mobile.data.dto.fund.FundTransactionDto
import rs.raf.banka2.mobile.data.dto.fund.FundWithdrawDto
import java.math.BigDecimal

/**
 * Karakterizacioni testovi za FundRepository.withdraw (R1 1054).
 *
 * BE `WithdrawFundDto` ima samo `destinationAccountId` + `amount`, gde
 * `amount == null` znaci "povuci celu poziciju". Mobile vise ne salje mrtvo
 * `withdrawAll` polje — checkbox se mapira na `amount = null`.
 */
class FundRepositoryTest {

    private val api = mockk<FundApi>()
    private val repo = FundRepository(api)

    private val okTx = FundTransactionDto(id = 1L, amount = BigDecimal("100"))

    @Test
    fun withdraw_withdrawAllTrue_sendsNullAmount() = runTest {
        val bodySlot = slot<FundWithdrawDto>()
        coEvery { api.withdraw(7L, capture(bodySlot)) } returns Response.success(okTx)

        val result = repo.withdraw(
            fundId = 7L,
            destinationAccountId = 99L,
            amount = BigDecimal("250"), // ignorise se kad je withdrawAll=true
            withdrawAll = true
        )

        assertTrue(result is ApiResult.Success)
        // BE "povuci sve" = amount == null (NE 250, NE withdrawAll polje)
        assertNull(bodySlot.captured.amount)
        assertEquals(99L, bodySlot.captured.destinationAccountId)
        coVerify(exactly = 1) { api.withdraw(7L, any()) }
    }

    @Test
    fun withdraw_withdrawAllFalse_sendsParsedAmount() = runTest {
        val bodySlot = slot<FundWithdrawDto>()
        coEvery { api.withdraw(7L, capture(bodySlot)) } returns Response.success(okTx)

        val result = repo.withdraw(
            fundId = 7L,
            destinationAccountId = 99L,
            amount = BigDecimal("250"),
            withdrawAll = false
        )

        assertTrue(result is ApiResult.Success)
        assertEquals(BigDecimal("250"), bodySlot.captured.amount)
        assertEquals(99L, bodySlot.captured.destinationAccountId)
    }
}
