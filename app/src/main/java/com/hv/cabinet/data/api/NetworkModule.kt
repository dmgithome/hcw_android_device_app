package com.hv.cabinet.data.api

import com.hv.cabinet.BuildConfig
import com.hv.cabinet.data.store.DebugConfigProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        baseUrlInterceptor: BaseUrlInterceptor,
        debugConfigProvider: DebugConfigProvider
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(DynamicHttpLoggingInterceptor(debugConfigProvider))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        return Retrofit.Builder()
            .baseUrl("http://127.0.0.1:3000/")
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideCabinetApi(retrofit: Retrofit): CabinetApi {
        return retrofit.create(CabinetApi::class.java)
    }

    private class DynamicHttpLoggingInterceptor(
        private val debugConfigProvider: DebugConfigProvider
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val level = when {
                !BuildConfig.DEBUG -> HttpLoggingInterceptor.Level.NONE
                debugConfigProvider.isHttpBodyLogEnabled() -> HttpLoggingInterceptor.Level.BODY
                else -> HttpLoggingInterceptor.Level.BASIC
            }
            if (level == HttpLoggingInterceptor.Level.NONE) {
                return chain.proceed(chain.request())
            }
            val delegate = HttpLoggingInterceptor().apply { this.level = level }
            return delegate.intercept(chain)
        }
    }
}
