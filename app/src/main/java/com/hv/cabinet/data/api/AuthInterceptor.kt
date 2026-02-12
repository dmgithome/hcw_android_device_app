package com.hv.cabinet.data.api

import com.hv.cabinet.data.store.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val appPreferences: AppPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = runBlocking { appPreferences.sessionFlow.first() }
        val request = chain.request()
        val requestBuilder = request.newBuilder()
        val path = request.url.encodedPath

        val skipAuth = path.startsWith("/api/v1/amis/login") ||
            path.startsWith("/api/nfc/loginByNFC")

        if (!skipAuth && session.token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${session.token}")
        }

        return chain.proceed(requestBuilder.build())
    }
}
