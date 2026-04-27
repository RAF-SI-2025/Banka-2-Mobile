package rs.raf.banka2.mobile.data.repository

import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.network.map
import rs.raf.banka2.mobile.core.network.safeApiCall
import rs.raf.banka2.mobile.data.api.TaxApi
import rs.raf.banka2.mobile.data.dto.tax.TaxRecordDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaxRepository @Inject constructor(
    private val api: TaxApi
) {
    suspend fun listAll(userType: String? = null, name: String? = null): ApiResult<List<TaxRecordDto>> =
        safeApiCall { api.listAll(userType, name) }

    suspend fun myRecord(): ApiResult<TaxRecordDto> = safeApiCall { api.myRecord() }

    suspend fun calculate(): ApiResult<Unit> = safeApiCall { api.calculate() }.map { }
}
