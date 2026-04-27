package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.OptionApi
import rs.raf.banka2.mobile.data.dto.option.OptionChainDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OptionRepository @Inject constructor(
    private val api: OptionApi
) {
    suspend fun chainFor(stockListingId: Long): ApiResult<List<OptionChainDto>> =
        safeApiCall { api.getOptionChain(stockListingId) }

    suspend fun exercise(optionId: Long): ApiResult<Unit> =
        safeApiCall { api.exerciseOption(optionId) }.map { }
}
