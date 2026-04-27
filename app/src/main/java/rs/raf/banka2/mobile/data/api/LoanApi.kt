package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.common.PageResponse
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationDto
import rs.raf.banka2.mobile.data.dto.loan.LoanApplicationResponseDto
import rs.raf.banka2.mobile.data.dto.loan.LoanDto
import rs.raf.banka2.mobile.data.dto.loan.LoanInstallmentDto

interface LoanApi {

    @POST("loans")
    suspend fun submitApplication(@Body body: LoanApplicationDto): Response<LoanApplicationResponseDto>

    @GET("loans/my")
    suspend fun getMyLoans(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageResponse<LoanDto>>

    @GET("loans/{id}")
    suspend fun getLoanById(@Path("id") id: Long): Response<LoanDto>

    @GET("loans/{id}/installments")
    suspend fun getInstallments(@Path("id") id: Long): Response<List<LoanInstallmentDto>>

    @POST("loans/{id}/early-repayment")
    suspend fun earlyRepay(@Path("id") id: Long): Response<LoanDto>

    @GET("loans/requests/my")
    suspend fun getMyApplications(): Response<List<LoanApplicationResponseDto>>

    // ─── Employee/Admin operacije ─────────────────────────────────
    @GET("loans/requests")
    suspend fun listAllRequests(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageResponse<LoanApplicationResponseDto>>

    @retrofit2.http.PATCH("loans/requests/{id}/approve")
    suspend fun approveLoanRequest(@Path("id") id: Long): Response<LoanDto>

    @retrofit2.http.PATCH("loans/requests/{id}/reject")
    suspend fun rejectLoanRequest(@Path("id") id: Long): Response<LoanApplicationResponseDto>

    @GET("loans")
    suspend fun listAllLoans(
        @Query("loanType") loanType: String? = null,
        @Query("status") status: String? = null,
        @Query("accountNumber") accountNumber: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageResponse<LoanDto>>
}
