package com.hv.cabinet.data.api

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class BaseUrlInterceptor @Inject constructor(
    private val baseUrlProvider: BaseUrlProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = baseUrlProvider.getBaseUrl().toHttpUrlOrNull()
        if (base == null) {
            return chain.proceed(request)
        }

        val newUrl = request.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()

        val rebuilt = request.newBuilder().url(newUrl).build()
        return chain.proceed(rebuilt)
    }
}
