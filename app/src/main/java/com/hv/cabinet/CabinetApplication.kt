package com.hv.cabinet

import android.app.Application
import android.util.Log
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.data.store.AppPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CabinetApplication : Application() {
    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        val debugConfig = runCatching {
            runBlocking { appPreferences.debugConfigFlow.first() }
        }.getOrNull()

        PerfMonitor.setEnabled(debugConfig?.perfLogEnabled == true)
        Timber.plant(
            when {
                BuildConfig.DEBUG && debugConfig?.debugLogEnabled == true -> Timber.DebugTree()
                else -> QuietTree()
            }
        )
    }
}

private class QuietTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.WARN) return
        Log.println(priority, tag ?: "Cabinet", message)
        t?.let { Log.println(priority, tag ?: "Cabinet", Log.getStackTraceString(it)) }
    }
}
