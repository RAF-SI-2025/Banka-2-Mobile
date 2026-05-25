package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.CardApi
import rs.raf.banka2.mobile.data.dto.card.CardDto
import rs.raf.banka2.mobile.data.dto.card.CardLimitUpdateDto
import rs.raf.banka2.mobile.data.dto.card.CardRequestCreateDto
import rs.raf.banka2.mobile.data.dto.card.CardRequestRejectDto
import rs.raf.banka2.mobile.data.dto.card.CardRequestResponseDto
import rs.raf.banka2.mobile.data.dto.card.CardTopUpRequest
import rs.raf.banka2.mobile.data.dto.card.CardWithdrawRequest
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val api: CardApi
) {
    suspend fun myCards(): ApiResult<List<CardDto>> = safeApiCall { api.getMyCards() }

    suspend fun cardsForAccount(accountId: Long): ApiResult<List<CardDto>> =
        safeApiCall { api.getCardsForAccount(accountId) }

    suspend fun block(id: Long): ApiResult<CardDto> = safeApiCall { api.blockCard(id) }

    suspend fun unblock(id: Long): ApiResult<CardDto> = safeApiCall { api.unblockCard(id) }

    suspend fun deactivate(id: Long): ApiResult<CardDto> = safeApiCall { api.deactivateCard(id) }

    /** ME-11: limit kao BigDecimal. */
    suspend fun updateLimit(id: Long, limit: BigDecimal): ApiResult<CardDto> =
        safeApiCall { api.updateLimit(id, CardLimitUpdateDto(limit)) }

    /**
     * ME-03: top-up INTERNET_PREPAID kartice.
     */
    suspend fun topUp(
        cardId: Long,
        sourceAccountId: Long,
        amount: BigDecimal
    ): ApiResult<CardDto> =
        safeApiCall { api.topUpCard(cardId, CardTopUpRequest(sourceAccountId, amount)) }

    /**
     * ME-03: povlacenje sa INTERNET_PREPAID kartice na Account.
     */
    suspend fun withdrawFromCard(
        cardId: Long,
        targetAccountId: Long,
        amount: BigDecimal
    ): ApiResult<CardDto> =
        safeApiCall { api.withdrawFromCard(cardId, CardWithdrawRequest(targetAccountId, amount)) }

    /**
     * ME-03: submit prosiren sa cardCategory i creditLimit (opciono za CREDIT karticu).
     */
    suspend fun submitRequest(
        accountId: Long,
        limit: BigDecimal,
        cardType: String,
        cardCategory: String = "DEBIT",
        creditLimit: BigDecimal? = null
    ): ApiResult<CardRequestResponseDto> =
        safeApiCall {
            api.submitCardRequest(
                CardRequestCreateDto(
                    accountId = accountId,
                    cardLimit = limit,
                    cardType = cardType,
                    cardCategory = cardCategory,
                    creditLimit = creditLimit
                )
            )
        }

    suspend fun confirmRequest(id: Long, otpCode: String): ApiResult<CardRequestResponseDto> =
        safeApiCall {
            api.confirmCardRequest(id, rs.raf.banka2.mobile.data.dto.payment.OtpVerifyRequest(otpCode))
        }

    suspend fun myRequests(): ApiResult<List<CardRequestResponseDto>> =
        safeApiCall { api.getMyCardRequests() }.map { it.content }

    suspend fun listAllRequests(status: String? = null): ApiResult<List<CardRequestResponseDto>> =
        safeApiCall { api.listAllCardRequests(status = status) }.map { it.content }

    suspend fun approveRequest(id: Long): ApiResult<CardRequestResponseDto> =
        safeApiCall { api.approveCardRequest(id) }

    suspend fun rejectRequest(id: Long, reason: String): ApiResult<CardRequestResponseDto> =
        safeApiCall { api.rejectCardRequest(id, CardRequestRejectDto(reason)) }
}
