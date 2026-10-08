package com.cephcoding.tyde.core.network

import com.cephcoding.tyde.core.common.auth.AuthTokenProvider
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    single {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    single { AuthInterceptor(get<AuthTokenProvider>()) }

    single {
        OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
