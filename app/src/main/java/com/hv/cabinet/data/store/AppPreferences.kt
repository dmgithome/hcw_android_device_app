package com.hv.cabinet.data.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hv.cabinet.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "cabinet_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val token = stringPreferencesKey("token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val userId = stringPreferencesKey("user_id")
        val userName = stringPreferencesKey("user_name")
        val userRole = stringPreferencesKey("user_role")

        val apiBaseUrl = stringPreferencesKey("api_base_url")
        val mqttBrokerUri = stringPreferencesKey("mqtt_broker_uri")
        val mqttUsername = stringPreferencesKey("mqtt_username")
        val mqttPassword = stringPreferencesKey("mqtt_password")
        val cabinetIps = stringPreferencesKey("cabinet_ips")
        val mqttClientId = stringPreferencesKey("mqtt_client_id")
        val nfcFormatTransform = booleanPreferencesKey("nfc_format_transform")
        val nfcFormatTransformStrict = booleanPreferencesKey("nfc_format_transform_strict")
        val debugLogEnabled = booleanPreferencesKey("debug_log_enabled")
        val perfLogEnabled = booleanPreferencesKey("perf_log_enabled")
        val httpBodyLogEnabled = booleanPreferencesKey("http_body_log_enabled")
    }

    val sessionFlow: Flow<SessionInfo> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            SessionInfo(
                token = prefs[Keys.token].orEmpty(),
                refreshToken = prefs[Keys.refreshToken].orEmpty(),
                userId = prefs[Keys.userId].orEmpty(),
                userName = prefs[Keys.userName].orEmpty(),
                userRole = prefs[Keys.userRole].orEmpty()
            )
        }

    val deviceConfigFlow: Flow<DeviceConfig> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            DeviceConfig(
                apiBaseUrl = prefs[Keys.apiBaseUrl] ?: BuildConfig.API_BASE_URL,
                mqttBrokerUri = prefs[Keys.mqttBrokerUri] ?: BuildConfig.MQTT_BROKER_URI,
                mqttUsername = prefs[Keys.mqttUsername].orEmpty(),
                mqttPassword = prefs[Keys.mqttPassword].orEmpty(),
                cabinetIps = prefs[Keys.cabinetIps]
                    ?.split(',')
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.ifEmpty { listOf("127.0.0.1") }
                    ?: listOf("127.0.0.1"),
                mqttClientId = prefs[Keys.mqttClientId].orEmpty(),
                nfcFormatTransform = prefs[Keys.nfcFormatTransform] ?: true,
                nfcFormatTransformStrict = prefs[Keys.nfcFormatTransformStrict] ?: true
            )
        }

    val debugConfigFlow: Flow<DebugConfig> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            DebugConfig(
                debugLogEnabled = prefs[Keys.debugLogEnabled] ?: false,
                perfLogEnabled = prefs[Keys.perfLogEnabled] ?: false,
                httpBodyLogEnabled = prefs[Keys.httpBodyLogEnabled] ?: false
            )
        }

    suspend fun saveSession(session: SessionInfo) {
        context.dataStore.edit { prefs ->
            prefs[Keys.token] = session.token
            prefs[Keys.refreshToken] = session.refreshToken
            prefs[Keys.userId] = session.userId
            prefs[Keys.userName] = session.userName
            prefs[Keys.userRole] = session.userRole
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.token)
            prefs.remove(Keys.refreshToken)
            prefs.remove(Keys.userId)
            prefs.remove(Keys.userName)
            prefs.remove(Keys.userRole)
        }
    }

    suspend fun saveDeviceConfig(config: DeviceConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.apiBaseUrl] = config.apiBaseUrl
            prefs[Keys.mqttBrokerUri] = config.mqttBrokerUri
            prefs[Keys.mqttUsername] = config.mqttUsername
            prefs[Keys.mqttPassword] = config.mqttPassword
            prefs[Keys.cabinetIps] = config.cabinetIps.joinToString(",")
            prefs[Keys.nfcFormatTransform] = config.nfcFormatTransform
            prefs[Keys.nfcFormatTransformStrict] = config.nfcFormatTransformStrict
        }
    }

    suspend fun getOrCreateMqttClientId(generator: () -> String): String {
        var resolved = ""
        context.dataStore.edit { prefs ->
            val stored = prefs[Keys.mqttClientId].orEmpty()
            resolved = if (stored.isNotBlank()) {
                stored
            } else {
                generator().also { prefs[Keys.mqttClientId] = it }
            }
        }
        return resolved
    }

    suspend fun saveDebugConfig(config: DebugConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.debugLogEnabled] = config.debugLogEnabled
            prefs[Keys.perfLogEnabled] = config.perfLogEnabled
            prefs[Keys.httpBodyLogEnabled] = config.httpBodyLogEnabled
        }
    }
}

data class SessionInfo(
    val token: String,
    val refreshToken: String,
    val userId: String,
    val userName: String,
    val userRole: String
) {
    val isLoggedIn: Boolean get() = token.isNotBlank()
}

data class DeviceConfig(
    val apiBaseUrl: String,
    val mqttBrokerUri: String,
    val mqttUsername: String,
    val mqttPassword: String,
    val cabinetIps: List<String>,
    val mqttClientId: String = "",
    val nfcFormatTransform: Boolean = true,
    val nfcFormatTransformStrict: Boolean = true
)

data class DebugConfig(
    val debugLogEnabled: Boolean = false,
    val perfLogEnabled: Boolean = false,
    val httpBodyLogEnabled: Boolean = false
)
