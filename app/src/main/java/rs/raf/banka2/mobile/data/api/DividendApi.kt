package rs.raf.banka2.mobile.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import rs.raf.banka2.mobile.data.dto.dividend.DividendPayoutDto

/**
 * Dividend API — Spec C3 §11 / B9 backend zadatak.
 *
 *  - `getMy()` vraca sve dividende ulogovanog korisnika sortirane DESC paymentDate.
 *  - `getByPosition(portfolioId)` vraca istoriju za konkretnu poziciju (jedan STOCK red u portfoliu).
 */
interface DividendApi {
    @GET("dividends/my")
    suspend fun getMy(): Response<List<DividendPayoutDto>>

    @GET("dividends/by-position/{portfolioId}")
    suspend fun getByPosition(@Path("portfolioId") portfolioId: Long): Response<List<DividendPayoutDto>>
}
