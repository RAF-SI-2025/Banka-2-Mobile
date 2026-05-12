package rs.raf.banka2.mobile.feature.savings

import java.math.BigDecimal
import java.math.RoundingMode

object SavingsMath {
    private val MONTHLY_DIVISOR = BigDecimal("1200")
    private val PENALTY_RATE = BigDecimal("0.01")

    fun monthlyInterest(principal: BigDecimal, annualRate: BigDecimal): BigDecimal =
        principal.multiply(annualRate).divide(MONTHLY_DIVISOR, 4, RoundingMode.HALF_UP)

    fun totalInterestOverTerm(principal: BigDecimal, annualRate: BigDecimal, termMonths: Int): BigDecimal =
        monthlyInterest(principal, annualRate).multiply(BigDecimal.valueOf(termMonths.toLong()))

    fun earlyWithdrawalPenalty(principal: BigDecimal): BigDecimal =
        principal.multiply(PENALTY_RATE).setScale(4, RoundingMode.HALF_UP)
}
