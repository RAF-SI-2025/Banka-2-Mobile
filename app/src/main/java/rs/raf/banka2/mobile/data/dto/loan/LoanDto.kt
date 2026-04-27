package rs.raf.banka2.mobile.data.dto.loan

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoanDto(
    val id: Long,
    val clientEmail: String? = null,
    val accountNumber: String? = null,
    val loanType: String? = null,
    val rateType: String? = null,            // FIXED / VARIABLE
    val amount: Double = 0.0,
    val currency: String? = null,
    val balance: Double? = null,
    val interestRate: Double? = null,        // nominalna
    val effectiveRate: Double? = null,       // efektivna (za varijabilnu R+M)
    val durationMonths: Int? = null,
    val monthlyInstallment: Double? = null,
    val nextInstallmentDate: String? = null,
    val maturityDate: String? = null,        // datum dospeca celog kredita
    val purpose: String? = null,
    val employmentStatus: String? = null,
    val employmentMonths: Int? = null,
    val employer: String? = null,
    val monthlyIncome: Double? = null,
    val phone: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class LoanInstallmentDto(
    val id: Long,
    val dueDate: String? = null,
    val amount: Double = 0.0,
    val status: String? = null,
    val paidDate: String? = null
)

@JsonClass(generateAdapter = true)
data class LoanApplicationDto(
    val loanType: String,
    val amount: Double,
    val durationMonths: Int,
    val purpose: String,
    val accountId: Long? = null,
    val accountNumber: String? = null,
    val currency: String? = null,
    val monthlyIncome: Double? = null,
    val employer: String? = null
)

@JsonClass(generateAdapter = true)
data class LoanApplicationResponseDto(
    val id: Long,
    val clientEmail: String? = null,
    val amount: Double? = null,
    val durationMonths: Int? = null,
    val interestRate: Double? = null,
    val purpose: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)
