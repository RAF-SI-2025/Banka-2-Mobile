package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.LoanApi
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationDto
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationResponseDto
import rs.raf.banka2.mobile.data.dto.loan.LoanDto
import rs.raf.banka2.mobile.data.dto.loan.LoanInstallmentDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val api: LoanApi
) {
    suspend fun apply(application: LoanApplicationDto): ApiResult<LoanApplicationResponseDto> =
        safeApiCall { api.submitApplication(application) }

    suspend fun myLoans(): ApiResult<List<LoanDto>> =
        safeApiCall { api.getMyLoans() }.map { it.content }

    suspend fun loanDetails(id: Long): ApiResult<LoanDto> = safeApiCall { api.getLoanById(id) }

    suspend fun installments(id: Long): ApiResult<List<LoanInstallmentDto>> =
        safeApiCall { api.getInstallments(id) }

    suspend fun earlyRepay(id: Long): ApiResult<LoanDto> = safeApiCall { api.earlyRepay(id) }

    suspend fun myApplications(): ApiResult<List<LoanApplicationResponseDto>> =
        safeApiCall { api.getMyApplications() }

    suspend fun listAllRequests(status: String? = null): ApiResult<List<LoanApplicationResponseDto>> =
        safeApiCall { api.listAllRequests(status = status) }.map { it.content }

    suspend fun approveRequest(id: Long): ApiResult<LoanDto> =
        safeApiCall { api.approveLoanRequest(id) }

    suspend fun rejectRequest(id: Long): ApiResult<LoanApplicationResponseDto> =
        safeApiCall { api.rejectLoanRequest(id) }

    suspend fun listAllLoans(
        loanType: String? = null,
        status: String? = null,
        accountNumber: String? = null
    ): ApiResult<List<LoanDto>> =
        safeApiCall { api.listAllLoans(loanType, status, accountNumber) }.map { it.content }
}
