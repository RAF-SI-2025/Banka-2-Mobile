package rs.raf.banka2.mobile.data.dto.fund

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * KONTRAKT (R1-224): BE `InvestmentFundDtos.InvestmentFundSummaryDto` salje
 * `fundValue` (NE `totalValue`); summary NE salje `managerId`/`profitPercent`/
 * `accountNumber`/`currency`. Ranije je Mobile citao `totalValue` → vrednost 0.
 * Vezujemo Kotlin `totalValue` na BE `fundValue` preko `@Json`.
 */
@JsonClass(generateAdapter = true)
data class FundSummaryDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val managerName: String? = null,
    val managerId: Long? = null,
    @param:Json(name = "fundValue") val totalValue: BigDecimal = BigDecimal.ZERO,
    val profit: BigDecimal = BigDecimal.ZERO,
    val profitPercent: Double? = null,
    val minimumContribution: BigDecimal = BigDecimal.ZERO,
    val currency: String? = "RSD",
    val accountNumber: String? = null,
    @param:Json(name = "inceptionDate") val createdAt: String? = null
)

/**
 * KONTRAKT (R1-224): BE `InvestmentFundDetailDto` salje `fundValue` (NE
 * `totalValue`), `liquidAmount` (NE `liquidFunds`), `managerEmployeeId` (NE
 * `managerId`), `accountNumber`. Ranije su value/likvidnost bili 0. Vezujemo
 * Kotlin imena na BE imena preko `@Json`.
 */
@JsonClass(generateAdapter = true)
data class FundDetailDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val managerName: String? = null,
    @param:Json(name = "managerEmployeeId") val managerId: Long? = null,
    @param:Json(name = "fundValue") val totalValue: BigDecimal = BigDecimal.ZERO,
    @param:Json(name = "liquidAmount") val liquidFunds: BigDecimal = BigDecimal.ZERO,
    val profit: BigDecimal = BigDecimal.ZERO,
    val profitPercent: Double? = null,
    val minimumContribution: BigDecimal = BigDecimal.ZERO,
    val currency: String? = "RSD",
    val accountNumber: String? = null,
    val holdings: List<FundHoldingDto> = emptyList(),
    @param:Json(name = "inceptionDate") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class FundHoldingDto(
    val listingId: Long? = null,
    val ticker: String? = null,
    val name: String? = null,
    val quantity: Int = 0,
    val currentPrice: BigDecimal? = null,
    val totalValue: BigDecimal? = null
)

@JsonClass(generateAdapter = true)
data class FundPerformancePointDto(
    val date: String,
    val value: BigDecimal
)

@JsonClass(generateAdapter = true)
data class FundTransactionDto(
    val id: Long,
    val clientName: String? = null,
    val amount: BigDecimal,
    val timestamp: String? = null,
    val inflow: Boolean = true,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class FundPositionDto(
    val id: Long,
    val fundId: Long,
    val fundName: String? = null,
    val totalInvested: BigDecimal = BigDecimal.ZERO,
    val currentValue: BigDecimal? = null,
    val percentOfFund: Double? = null,
    val profit: BigDecimal? = null,
    val profitPercent: Double? = null,
    val currency: String? = "RSD"
)

@JsonClass(generateAdapter = true)
data class CreateFundDto(
    val name: String,
    val description: String? = null,
    val minimumContribution: BigDecimal
)

/**
 * KONTRAKT (R1-223): BE `InvestFundDto` zahteva `@NotBlank currency` (da bi
 * mogao da konvertuje u RSD). Ranije Mobile nije slao currency → 400 na svaku
 * uplatu u fond. Default RSD (Mobile uplaca s RSD racuna).
 */
@JsonClass(generateAdapter = true)
data class FundInvestDto(
    val sourceAccountId: Long,
    val amount: BigDecimal,
    val currency: String = "RSD"
)

/**
 * KONTRAKT (R1 1054): BE `WithdrawFundDto` ima SAMO `destinationAccountId` + `amount`,
 * gde `amount == null` znaci "povuci celu poziciju" (spec linija 342). Mobile je
 * ranije slao i `withdrawAll: Boolean` koje BE NE cita (mrtvo polje na zici) — UI
 * checkbox "Povuci celu poziciju" se sada mapira na `amount = null` u repozitorijumu.
 */
@JsonClass(generateAdapter = true)
data class FundWithdrawDto(
    val destinationAccountId: Long,
    val amount: BigDecimal? = null
)

/** P1.2: telo zahteva za prebacivanje vlasnistva fonda na drugog menadzera. */
@JsonClass(generateAdapter = true)
data class ReassignFundManagerDto(
    val newManagerEmployeeId: Long
)
