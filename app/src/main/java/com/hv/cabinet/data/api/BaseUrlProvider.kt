package com.hv.cabinet.data.api

import com.hv.cabinet.BuildConfig
import com.hv.cabinet.data.store.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlProvider @Inject constructor(
    appPreferences: AppPreferences
) {
    @Volatile
    private var currentBaseUrl: String = ensureTrailingSlash(BuildConfig.API_BASE_URL)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            appPreferences.deviceConfigFlow.collectLatest { cfg ->
                val normalized = ensureTrailingSlash(cfg.apiBaseUrl)
                if (normalized.toHttpUrlOrNull() != null) {
                    currentBaseUrl = normalized
                }
            }
        }
    }

    fun getBaseUrl(): String = currentBaseUrl

    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith('/')) url else "$url/"
    }
}
