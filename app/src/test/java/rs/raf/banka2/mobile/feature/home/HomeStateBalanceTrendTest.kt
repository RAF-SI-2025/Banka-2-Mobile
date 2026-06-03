package rs.raf.banka2.mobile.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.payment.PaymentListItemDto
import java.math.BigDecimal

/**
 * R2-1492 [money]: `balanceTrend` krece od `totalRsdBalance` (samo RSD racuni),
 * pa sme da sabira/oduzima SAMO RSD placanja. Ranije se `p.amount` racunao bez
 * obzira na valutu → FX placanje (npr 1000 EUR) bi pomerilo RSD trend za 1000
 * kao da je 1000 RSD i besmisleno iskrivilo grafik.
 */
class HomeStateBalanceTrendTest {

    private fun rsdAccount(balance: String) =
        AccountDto(id = 1L, accountNumber = "222-RSD", currency = "RSD", balance = BigDecimal(balance))

    private fun payment(amount: String, currency: String, direction: String) =
        PaymentListItemDto(id = 1L, amount = BigDecimal(amount), currency = currency, direction = direction)

    @Test
    fun balanceTrend_excludesFxPayments() {
        val state = HomeState(
            accounts = listOf(rsdAccount("10000")),
            recentPayments = listOf(
                payment("500", "RSD", "OUTGOING"),
                payment("1000", "EUR", "OUTGOING"), // FX — NE sme da utice na RSD trend
                payment("200", "RSD", "INCOMING")
            )
        )

        // Bez FX-a, trend ima 2 tacke (samo RSD placanja) i poslednja je tekuci RSD saldo.
        val trend = state.balanceTrend
        assertEquals(2, trend.size)
        assertEquals(BigDecimal("10000"), trend.last())
        // EUR placanje od 1000 nigde ne pomera saldo za 1000.
        assertTrue(trend.none { it == BigDecimal("9000") || it == BigDecimal("11000") })
    }

    @Test
    fun balanceTrend_treatsNullCurrencyAsRsd() {
        val state = HomeState(
            accounts = listOf(rsdAccount("5000")),
            recentPayments = listOf(payment("300", "RSD", "OUTGOING").copy(currency = null))
        )
        assertEquals(1, state.balanceTrend.size)
    }

    @Test
    fun balanceTrend_onlyFxPayments_returnsEmpty() {
        val state = HomeState(
            accounts = listOf(rsdAccount("5000")),
            recentPayments = listOf(payment("1000", "EUR", "OUTGOING"))
        )
        assertTrue(state.balanceTrend.isEmpty())
    }
}
