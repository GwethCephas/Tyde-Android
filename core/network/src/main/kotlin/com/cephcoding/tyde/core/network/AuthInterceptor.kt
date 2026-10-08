package com.cephcoding.tyde.core.network

import com.cephcoding.tyde.core.common.auth.AuthTokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authTokenProvider: AuthTokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authTokenProvider.accessToken() }
        val request = chain.request().let { original ->
            if (token.isNullOrBlank()) {
                original
            } else {
                original.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            }
        }
        return chain.proceed(request)
    }
}
