package rs.raf.banka2.mobile.core.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import rs.raf.banka2.mobile.BuildConfig
import rs.raf.banka2.mobile.core.network.AuthInterceptor
import rs.raf.banka2.mobile.core.network.TokenAuthenticator
import rs.raf.banka2.mobile.data.api.AccountApi
import rs.raf.banka2.mobile.data.api.ActuaryApi
import rs.raf.banka2.mobile.data.api.AuthApi
import rs.raf.banka2.mobile.data.api.CardApi
import rs.raf.banka2.mobile.data.api.ClientApi
import rs.raf.banka2.mobile.data.api.EmployeeAdminApi
import rs.raf.banka2.mobile.data.api.EmployeeApi
import rs.raf.banka2.mobile.data.api.ExchangeApi
import rs.raf.banka2.mobile.data.api.ExchangeManagementApi
import rs.raf.banka2.mobile.data.api.FundApi
import rs.raf.banka2.mobile.data.api.InterbankApi
import rs.raf.banka2.mobile.data.api.ListingApi
import rs.raf.banka2.mobile.data.api.LoanApi
import rs.raf.banka2.mobile.data.api.MarginApi
import rs.raf.banka2.mobile.data.api.OptionApi
import rs.raf.banka2.mobile.data.api.OrderApi
import rs.raf.banka2.mobile.data.api.OtcApi
import rs.raf.banka2.mobile.data.api.PaymentApi
import rs.raf.banka2.mobile.data.api.PortfolioApi
import rs.raf.banka2.mobile.data.api.ProfitBankApi
import rs.raf.banka2.mobile.data.api.RecipientApi
import rs.raf.banka2.mobile.data.api.TaxApi
import rs.raf.banka2.mobile.data.api.TransferApi
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.ENABLE_HTTP_LOGGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    /**
     * Klijent koji koristi `/auth/refresh`. Mora da nema [AuthInterceptor]
     * ni [TokenAuthenticator] da ne bismo upali u rekurziju.
     */
    @Provides
    @Singleton
    @Named(REFRESH_CLIENT)
    fun provideRefreshClient(
        logging: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        logging: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideEmployeeApi(retrofit: Retrofit): EmployeeApi = retrofit.create(EmployeeApi::class.java)

    @Provides
    @Singleton
    fun provideAccountApi(retrofit: Retrofit): AccountApi = retrofit.create(AccountApi::class.java)

    @Provides
    @Singleton
    fun providePaymentApi(retrofit: Retrofit): PaymentApi = retrofit.create(PaymentApi::class.java)

    @Provides
    @Singleton
    fun provideRecipientApi(retrofit: Retrofit): RecipientApi = retrofit.create(RecipientApi::class.java)

    @Provides
    @Singleton
    fun provideTransferApi(retrofit: Retrofit): TransferApi = retrofit.create(TransferApi::class.java)

    @Provides
    @Singleton
    fun provideExchangeApi(retrofit: Retrofit): ExchangeApi = retrofit.create(ExchangeApi::class.java)

    @Provides
    @Singleton
    fun provideCardApi(retrofit: Retrofit): CardApi = retrofit.create(CardApi::class.java)

    @Provides
    @Singleton
    fun provideLoanApi(retrofit: Retrofit): LoanApi = retrofit.create(LoanApi::class.java)

    @Provides
    @Singleton
    fun provideMarginApi(retrofit: Retrofit): MarginApi = retrofit.create(MarginApi::class.java)

    @Provides
    @Singleton
    fun provideListingApi(retrofit: Retrofit): ListingApi = retrofit.create(ListingApi::class.java)

    @Provides
    @Singleton
    fun provideOrderApi(retrofit: Retrofit): OrderApi = retrofit.create(OrderApi::class.java)

    @Provides
    @Singleton
    fun providePortfolioApi(retrofit: Retrofit): PortfolioApi = retrofit.create(PortfolioApi::class.java)

    @Provides
    @Singleton
    fun provideOptionApi(retrofit: Retrofit): OptionApi = retrofit.create(OptionApi::class.java)

    @Provides
    @Singleton
    fun provideActuaryApi(retrofit: Retrofit): ActuaryApi = retrofit.create(ActuaryApi::class.java)

    @Provides
    @Singleton
    fun provideTaxApi(retrofit: Retrofit): TaxApi = retrofit.create(TaxApi::class.java)

    @Provides
    @Singleton
    fun provideExchangeManagementApi(retrofit: Retrofit): ExchangeManagementApi =
        retrofit.create(ExchangeManagementApi::class.java)

    @Provides
    @Singleton
    fun provideOtcApi(retrofit: Retrofit): OtcApi = retrofit.create(OtcApi::class.java)

    @Provides
    @Singleton
    fun provideFundApi(retrofit: Retrofit): FundApi = retrofit.create(FundApi::class.java)

    @Provides
    @Singleton
    fun provideProfitBankApi(retrofit: Retrofit): ProfitBankApi =
        retrofit.create(ProfitBankApi::class.java)

    @Provides
    @Singleton
    fun provideInterbankApi(retrofit: Retrofit): InterbankApi =
        retrofit.create(InterbankApi::class.java)

    @Provides
    @Singleton
    fun provideEmployeeAdminApi(retrofit: Retrofit): EmployeeAdminApi =
        retrofit.create(EmployeeAdminApi::class.java)

    @Provides
    @Singleton
    fun provideClientApi(retrofit: Retrofit): ClientApi =
        retrofit.create(ClientApi::class.java)

    const val REFRESH_CLIENT = "refresh-okhttp"
}
