package com.hv.cabinet.data.mqtt

import android.content.Context
import com.hv.cabinet.BuildConfig
import com.hv.cabinet.core.AppDispatchers
import com.hv.cabinet.core.AppError
import com.hv.cabinet.core.AppResult
import com.hv.cabinet.data.store.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed class InventoryMqttEvent {
    data class Tag(val epc: String) : InventoryMqttEvent()
    data class InventoryStatus(val type: String) : InventoryMqttEvent()
    data class NfcCard(val cardNo: String) : InventoryMqttEvent()
}

@Singleton
class MqttManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val dispatchers: AppDispatchers
) {
    companion object {
        const val TOPIC_RFID_TAGS = "table/rfid/fast_tag/#"
        const val TOPIC_RFID_INVENTORY_STATUS = "table/rfid/inventory_status/#"
        const val TOPIC_NFC_CARD = "dk25_nfc/card/#"
    }

    private var client: MqttAndroidClient? = null
    private var clientServerUri: String? = null
    private var connected = false
    private val subscribedTopics = linkedSetOf<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<InventoryMqttEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<InventoryMqttEvent> = _events.asSharedFlow()

    suspend fun ensureConnected(): AppResult<Unit> = withContext(dispatchers.io) {
        runCatching {
            val cfg = appPreferences.deviceConfigFlow.first()
            val serverUri = cfg.mqttBrokerUri.trim()
            if (serverUri.isBlank()) {
                return@withContext AppResult.Failure(AppError.Business("MQTT 地址不能为空"))
            }

            if (client != null && clientServerUri != serverUri) {
                recreateClient(serverUri)
            } else if (connected && client?.isConnected == true) {
                return@withContext AppResult.Success(Unit)
            }

            val clientId = BuildConfig.MQTT_CLIENT_ID_PREFIX + UUID.randomUUID().toString().take(8)

            if (client == null) {
                client = MqttAndroidClient(context, serverUri, clientId).apply {
                    setCallback(object : MqttCallbackExtended {
                        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                            connected = true
                            Timber.i("MQTT connected: reconnect=$reconnect, uri=$serverURI")
                            if (reconnect) {
                                scope.launch {
                                    resubscribeTrackedTopics()
                                }
                            }
                        }

                        override fun connectionLost(cause: Throwable?) {
                            connected = false
                            Timber.e(cause, "MQTT lost")
                        }

                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            handleIncoming(topic, message)
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
                    })
                }
                clientServerUri = serverUri
            }

            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
                if (cfg.mqttUsername.isNotBlank()) userName = cfg.mqttUsername
                if (cfg.mqttPassword.isNotBlank()) password = cfg.mqttPassword.toCharArray()
            }

            val connectResult = suspendCancellableCoroutine<Boolean> { cont ->
                client?.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        connected = false
                        Timber.e(exception, "MQTT connect failed")
                        if (cont.isActive) cont.resume(false)
                    }
                })
            }

            if (!connectResult) {
                connected = false
                return@withContext AppResult.Failure(AppError.Network("MQTT 连接失败"))
            }
            connected = true
            AppResult.Success(Unit)
        }.getOrElse {
            Timber.e(it, "ensureConnected failed")
            AppResult.Failure(AppError.Network(it.message ?: "MQTT 连接失败"))
        }
    }

    suspend fun subscribeInventoryTopics(): AppResult<Unit> = withContext(dispatchers.io) {
        val conn = ensureConnected()
        if (conn is AppResult.Failure) {
            return@withContext conn
        }
        val tagResult = subscribe(TOPIC_RFID_TAGS)
        if (tagResult is AppResult.Failure) {
            return@withContext tagResult
        }

        val statusResult = subscribe(TOPIC_RFID_INVENTORY_STATUS)
        if (statusResult is AppResult.Failure) {
            unsubscribe(TOPIC_RFID_TAGS)
            return@withContext statusResult
        }

        AppResult.Success(Unit)
    }

    suspend fun subscribeNfcTopic(): AppResult<Unit> = withContext(dispatchers.io) {
        val conn = ensureConnected()
        if (conn is AppResult.Failure) {
            return@withContext conn
        }
        subscribe(TOPIC_NFC_CARD)
    }

    suspend fun unsubscribeInventoryTopics() = withContext(dispatchers.io) {
        unsubscribe(TOPIC_RFID_TAGS)
        unsubscribe(TOPIC_RFID_INVENTORY_STATUS)
    }

    private suspend fun subscribe(topic: String): AppResult<Unit> = withContext(dispatchers.io) {
        runCatching {
            val c = client ?: return@withContext AppResult.Failure(AppError.Network("MQTT 未初始化"))
            val result = suspendCancellableCoroutine<Boolean> { cont ->
                c.subscribe(topic, 1, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Timber.e(exception, "MQTT subscribe failed: $topic")
                        if (cont.isActive) cont.resume(false)
                    }
                })
            }
            if (result) {
                subscribedTopics.add(topic)
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.Network("MQTT 订阅失败"))
            }
        }.getOrElse {
            AppResult.Failure(AppError.Network(it.message ?: "MQTT 订阅失败"))
        }
    }

    private suspend fun unsubscribe(topic: String) = withContext(dispatchers.io) {
        runCatching {
            client?.unsubscribe(topic)
            subscribedTopics.remove(topic)
        }.onFailure {
            Timber.w(it, "MQTT unsubscribe failed: $topic")
        }
    }

    private fun recreateClient(newServerUri: String) {
        runCatching {
            client?.setCallback(null)
            if (client?.isConnected == true) {
                client?.unregisterResources()
                client?.disconnect()
            }
            client?.close()
        }.onFailure {
            Timber.w(it, "MQTT close old client failed")
        }

        client = null
        clientServerUri = newServerUri
        connected = false
    }

    private suspend fun resubscribeTrackedTopics() = withContext(dispatchers.io) {
        if (subscribedTopics.isEmpty()) return@withContext
        subscribedTopics.toList().forEach { topic ->
            val result = subscribe(topic)
            if (result is AppResult.Failure) {
                Timber.w("MQTT resubscribe failed: %s", topic)
            }
        }
    }

    private fun handleIncoming(topic: String?, message: MqttMessage?) {
        if (topic == null || message == null) return
        val payload = message.toString().trim()

        runCatching {
            when {
                topic.startsWith("table/rfid/fast_tag/") -> {
                    val epc = parseEpc(payload)
                    if (epc.isNotBlank()) {
                        _events.tryEmit(InventoryMqttEvent.Tag(normalizeEpc(epc)))
                    }
                }

                topic.startsWith("table/rfid/inventory_status/") -> {
                    val statusType = parseStatusType(payload)
                    if (statusType.isNotBlank()) {
                        _events.tryEmit(InventoryMqttEvent.InventoryStatus(statusType))
                    }
                }

                topic.startsWith("dk25_nfc/card/") -> {
                    val card = parseCardNo(payload)
                    if (card.isNotBlank()) {
                        _events.tryEmit(InventoryMqttEvent.NfcCard(card))
                    }
                }
            }
        }.onFailure {
            Timber.w(it, "MQTT payload parse failed: topic=$topic payload=$payload")
        }
    }

    private fun parseEpc(payload: String): String {
        if (payload.isBlank()) return ""
        return runCatching {
            val obj = JSONObject(payload)
            obj.optString("EPC").ifBlank { obj.optString("epc") }
        }.getOrDefault(payload)
    }

    private fun parseStatusType(payload: String): String {
        if (payload.isBlank()) return ""
        return runCatching {
            val obj = JSONObject(payload)
            obj.optString("Type").ifBlank { obj.optString("type") }
        }.getOrDefault(payload)
    }

    private fun parseCardNo(payload: String): String {
        if (payload.isBlank()) return ""
        return runCatching {
            val obj = JSONObject(payload)
            obj.optString("cardNumber")
                .ifBlank { obj.optString("cardNo") }
                .ifBlank { obj.optString("value") }
        }.getOrDefault(payload)
    }

    private fun normalizeEpc(raw: String): String {
        return raw.replace(" ", "").replace("-", "").trim().uppercase()
    }
}
