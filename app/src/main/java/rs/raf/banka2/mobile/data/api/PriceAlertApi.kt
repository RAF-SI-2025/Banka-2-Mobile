package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.raf.banka2.mobile.data.dto.pricealert.CreatePriceAlertRequest
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertDto

/**
 * [FE2 Mobile port] Retrofit interfejs za Price Alert endpoint-e.
 *
 * BE: trading-service `PriceAlertController` izlaze rute pod `/price-alerts`.
 * Sve rute zahtevaju JWT.
 */
interface PriceAlertApi {

    @POST("price-alerts")
    suspend fun create(@Body body: CreatePriceAlertRequest): Response<PriceAlertDto>

    /**
     * `active` opcioni filter:
     *   - true  -> samo aktivni alarmi
     *   - false -> samo okidnuti (istorija)
     *   - null  -> sve
     */
    @GET("price-alerts/my")
    suspend fun listMy(@Query("active") active: Boolean? = null): Response<List<PriceAlertDto>>

    @DELETE("price-alerts/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}
