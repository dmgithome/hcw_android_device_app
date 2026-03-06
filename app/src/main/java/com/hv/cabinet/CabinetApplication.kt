package com.hv.cabinet

import android.app.Application
import android.util.Log
import com.hv.cabinet.core.AppResult
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.data.mqtt.MqttManager
import com.hv.cabinet.data.store.DebugConfigProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CabinetApplication : Application() {
    @Inject
    lateinit var debugConfigProvider: DebugConfigProvider

    @Inject
    lateinit var mqttManager: MqttManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        PerfMonitor.setEnabled(false)
        Timber.plant(
            if (BuildConfig.DEBUG) ConfigurableDebugTree(debugConfigProvider) else QuietTree()
        )

        applicationScope.launch {
            debugConfigProvider.state.collectLatest { cfg ->
                PerfMonitor.setEnabled(cfg.perfLogEnabled)
            }
        }
        applicationScope.launch {
            val result = mqttManager.subscribeNfcTopic()
            if (result is AppResult.Failure) {
                Timber.w("NFC MQTT subscribe failed on app start: %s", result.error.message)
            }
        }
    }
}

private class ConfigurableDebugTree(
    private val debugConfigProvider: DebugConfigProvider
) : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val debugEnabled = debugConfigProvider.isDebugLogEnabled()
        if (!debugEnabled && priority < Log.WARN) return
        super.log(priority, tag, message, t)
    }
}

private class QuietTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.WARN) return
        Log.println(priority, tag ?: "Cabinet", message)
        t?.let { Log.println(priority, tag ?: "Cabinet", Log.getStackTraceString(it)) }
    }
}
