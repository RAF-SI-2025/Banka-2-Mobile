package rs.raf.banka2.mobile.data.dto.loan

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * ME-11: novcana polja prebacena sa Double na BigDecimal (spec C2 §255).
 * Polje `interestRate` / `effectiveRate` ostaju Double — to su rate koeficijenti
 * (procenat) koji ne predstavljaju iznos novca pa preciznost nije kritican zahtev.
 */
@JsonClass(generateAdapter = true)
data class LoanDto(
    val id: Long,
    val clientEmail: String? = null,
    val accountNumber: String? = null,
    val loanType: String? = null,
    val rateType: String? = null,            // FIXED / VARIABLE
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String? = null,
    val balance: BigDecimal? = null,
    val interestRate: Double? = null,        // nominalna (procenat — Double OK)
    val effectiveRate: Double? = null,       // efektivna (za varijabilnu R+M)
    val durationMonths: Int? = null,
    val monthlyInstallment: BigDecimal? = null,
    val nextInstallmentDate: String? = null,
    val maturityDate: String? = null,        // datum dospeca celog kredita
    val purpose: String? = null,
    val employmentStatus: String? = null,
    val employmentMonths: Int? = null,
    val employer: String? = null,
    val monthlyIncome: BigDecimal? = null,
    val phone: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class LoanInstallmentDto(
    val id: Long,
    val dueDate: String? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
    val status: String? = null,
    val paidDate: String? = null
)

/**
 * ME-09 fix: dodato `otpCode` polje — paritet sa PaymentRequest/TransferRequest/SavingsRequest.
 * BE BE-PAY-06 fix u istom pravcu (apply + early-repayment moraju OTP-gated). Default null
 * za backwards-compat sa starim BE-om koji jos ne zahteva OTP.
 *
 * ME-11: amount + monthlyIncome BigDecimal (spec C2 §255).
 *
 * P1-mobile-banking-1 (R1-131): KONTRAKT uskladjen sa BE `LoanRequestDto`
 * (`rs.raf.banka2_bek.loan.dto.LoanRequestDto`). BE zahteva (`@NotNull`)
 * `interestType` i `repaymentPeriod` (+`@NotBlank` `accountNumber`/`currency`).
 * Stari Mobile DTO je slao `durationMonths`/`purpose`/`employer` i NIJE slao
 * `interestType`/`repaymentPeriod` → svaki apply je padao na 400. Polja su sada
 * preimenovana 1:1 na BE wire-imena (`@Json`) i dodata su obavezna polja.
 */
@JsonClass(generateAdapter = true)
data class LoanApplicationDto(
    val loanType: String,
    /** FIXED / VARIABLE — BE `interestType` @NotNull. */
    val interestType: String,
    @param:Json(name = "amount") val amount: BigDecimal,
    @param:Json(name = "currency") val currency: String,
    /** BE `repaymentPeriod` @NotNull @Positive (broj meseci). */
    val repaymentPeriod: Int,
    @param:Json(name = "accountNumber") val accountNumber: String,
    val loanPurpose: String? = null,
    val phoneNumber: String? = null,
    val employmentStatus: String? = null,
    val monthlyIncome: BigDecimal? = null,
    val permanentEmployment: Boolean? = null,
    val employmentPeriod: Int? = null,
    val otpCode: String? = null
)

@JsonClass(generateAdapter = true)
data class LoanApplicationResponseDto(
    val id: Long,
    val clientEmail: String? = null,
    val amount: BigDecimal? = null,
    val durationMonths: Int? = null,
    val interestRate: Double? = null,
    val purpose: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)
