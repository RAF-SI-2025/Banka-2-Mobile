package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import rs.raf.banka2.mobile.data.dto.recurringorder.CreateRecurringOrderRequest
import rs.raf.banka2.mobile.data.dto.recurringorder.RecurringOrderDto

/**
 * [FE3 Mobile port] Retrofit interfejs za RecurringOrder (DCA) endpoint-e.
 *
 * BE: trading-service `RecurringOrderController` izlaze rute pod `/recurring-orders`.
 * BE `listMy` ne podrzava active query filter — filtriramo FE-strana po `active`.
 */
interface RecurringOrderApi {

    @POST("recurring-orders")
    suspend fun create(@Body body: CreateRecurringOrderRequest): Response<RecurringOrderDto>

    @GET("recurring-orders")
    suspend fun listMy(): Response<List<RecurringOrderDto>>

    @PATCH("recurring-orders/{id}/pause")
    suspend fun pause(@Path("id") id: Long): Response<RecurringOrderDto>

    @PATCH("recurring-orders/{id}/resume")
    suspend fun resume(@Path("id") id: Long): Response<RecurringOrderDto>

    @DELETE("recurring-orders/{id}")
    suspend fun cancel(@Path("id") id: Long): Response<Unit>
}
