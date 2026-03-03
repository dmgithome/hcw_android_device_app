package com.hv.cabinet.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hv.cabinet.core.AppResult
import com.hv.cabinet.data.api.CabinetRepository
import com.hv.cabinet.data.mqtt.InventoryMqttEvent
import com.hv.cabinet.data.mqtt.MqttManager
import com.hv.cabinet.data.store.AppPreferences
import com.hv.cabinet.data.store.DebugConfig
import com.hv.cabinet.data.store.DeviceConfig
import com.hv.cabinet.domain.MessageLevel
import com.hv.cabinet.domain.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: CabinetRepository,
    private val appPreferences: AppPreferences,
    private val mqttManager: MqttManager
) : ViewModel() {
    companion object {
        private const val NFC_LOGIN_DEBOUNCE_MS = 500L
    }

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect = _effect.asSharedFlow()
    private val lastNfcLoginTimestamps = object : LinkedHashMap<String, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean = size > 32
    }
    private var nfcLoginJob: Job? = null

    init {
        viewModelScope.launch {
            appPreferences.deviceConfigFlow.collectLatest { cfg ->
                _state.value = _state.value.copy(
                    apiBaseUrl = cfg.apiBaseUrl,
                    mqttBrokerUri = cfg.mqttBrokerUri,
                    mqttUsername = cfg.mqttUsername,
                    mqttPassword = cfg.mqttPassword,
                    cabinetIpsRaw = cfg.cabinetIps.joinToString(","),
                    nfcFormatTransform = cfg.nfcFormatTransform,
                    nfcFormatTransformStrict = cfg.nfcFormatTransformStrict
                )
            }
        }
        viewModelScope.launch {
            appPreferences.debugConfigFlow.collectLatest { cfg ->
                _state.value = _state.value.copy(
                    debugLogEnabled = cfg.debugLogEnabled,
                    perfLogEnabled = cfg.perfLogEnabled,
                    httpBodyLogEnabled = cfg.httpBodyLogEnabled
                )
            }
        }
        viewModelScope.launch {
            val subscribe = mqttManager.subscribeNfcTopic()
            if (subscribe is AppResult.Failure) {
                _state.value = _state.value.copy(
                    message = UiMessage(subscribe.error.message, MessageLevel.Warning)
                )
            }
            mqttManager.events.collectLatest { event ->
                if (event is InventoryMqttEvent.NfcCard && event.value.isNotBlank()) {
                    onNfcCardEvent(event)
                }
            }
        }
    }

    fun updateUserName(value: String) {
        _state.value = _state.value.copy(userName = value)
    }

    fun updatePassword(value: String) {
        _state.value = _state.value.copy(password = value)
    }

    fun updateNfcCardNo(value: String) {
        _state.value = _state.value.copy(nfcCardNo = value)
    }

    fun updateTab(tab: LoginTab) {
        _state.value = _state.value.copy(
            tab = tab,
            message = UiMessage()
        )
    }

    fun updateApiBaseUrl(value: String) {
        _state.value = _state.value.copy(apiBaseUrl = value)
    }

    fun updateMqttUri(value: String) {
        _state.value = _state.value.copy(mqttBrokerUri = value)
    }

    fun updateMqttUser(value: String) {
        _state.value = _state.value.copy(mqttUsername = value)
    }

    fun updateMqttPassword(value: String) {
        _state.value = _state.value.copy(mqttPassword = value)
    }

    fun updateCabinetIps(value: String) {
        _state.value = _state.value.copy(cabinetIpsRaw = value)
    }

    fun updateNfcFormatTransform(enabled: Boolean) {
        _state.value = _state.value.copy(nfcFormatTransform = enabled)
    }

    fun updateNfcFormatTransformStrict(enabled: Boolean) {
        _state.value = _state.value.copy(nfcFormatTransformStrict = enabled)
    }

    fun updateDebugLogEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(debugLogEnabled = enabled)
    }

    fun updatePerfLogEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(perfLogEnabled = enabled)
    }

    fun updateHttpBodyLogEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(httpBodyLogEnabled = enabled)
    }

    fun toggleConfigExpanded() {
        _state.value = _state.value.copy(configExpanded = !_state.value.configExpanded)
    }

    fun saveConfig() {
        viewModelScope.launch {
            val current = _state.value
            val apiBaseUrl = current.apiBaseUrl.trim()
            val mqttBrokerUri = current.mqttBrokerUri.trim()
            if (!apiBaseUrl.startsWith("http://") && !apiBaseUrl.startsWith("https://")) {
                _state.value = _state.value.copy(
                    message = UiMessage("API 地址格式错误，请使用 http:// 或 https://", MessageLevel.Error)
                )
                return@launch
            }

            if (mqttBrokerUri.isBlank()) {
                _state.value = _state.value.copy(
                    message = UiMessage("MQTT 地址不能为空", MessageLevel.Error)
                )
                return@launch
            }

            val ips = current.cabinetIpsRaw.split(',').map { it.trim() }.filter { it.isNotBlank() }
            _state.value = _state.value.copy(savingConfig = true)
            appPreferences.saveDeviceConfig(
                DeviceConfig(
                    apiBaseUrl = apiBaseUrl,
                    mqttBrokerUri = mqttBrokerUri,
                    mqttUsername = current.mqttUsername.trim(),
                    mqttPassword = current.mqttPassword,
                    cabinetIps = if (ips.isEmpty()) listOf("127.0.0.1") else ips,
                    nfcFormatTransform = current.nfcFormatTransform,
                    nfcFormatTransformStrict = current.nfcFormatTransformStrict
                )
            )
            appPreferences.saveDebugConfig(
                DebugConfig(
                    debugLogEnabled = current.debugLogEnabled,
                    perfLogEnabled = current.perfLogEnabled,
                    httpBodyLogEnabled = current.httpBodyLogEnabled
                )
            )
            _state.value = _state.value.copy(
                savingConfig = false,
                message = UiMessage("设备与调试配置已保存（网络日志级别重启后生效）", MessageLevel.Success)
            )
        }
    }

    fun login() {
        when (_state.value.tab) {
            LoginTab.Account -> loginByAccount()
            LoginTab.Nfc -> {
                _state.value = _state.value.copy(
                    message = UiMessage("NFC登录为自动触发，请刷卡", MessageLevel.Info)
                )
            }
        }
    }

    private fun loginByAccount() {
        val username = _state.value.userName.trim()
        val password = _state.value.password
        if (username.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(
                message = UiMessage("请输入账号和密码", MessageLevel.Warning)
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = UiMessage())
            when (val result = repository.loginByAccount(username, password)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        loading = false,
                        message = UiMessage("登录成功", MessageLevel.Success)
                    )
                    _effect.emit(LoginEffect.LoginSuccess)
                }

                is AppResult.Failure -> {
                    _state.value = _state.value.copy(
                        loading = false,
                        message = UiMessage(result.error.message, MessageLevel.Error)
                    )
                }
            }
        }
    }

    private fun loginByNfc() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = UiMessage())
            val cardNo = _state.value.nfcCardNo.trim()
            val ip = _state.value.nfcResolvedIp.trim()
            if (ip.isBlank() || cardNo.isBlank()) {
                _state.value = _state.value.copy(
                    loading = false,
                    message = UiMessage("未获取到有效刷卡信息，请重试", MessageLevel.Warning)
                )
                return@launch
            }
            when (val result = repository.loginByNfc(ip = ip, cardNumber = cardNo, mac = _state.value.nfcReaderId.trim())) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        loading = false,
                        message = UiMessage("登录成功", MessageLevel.Success)
                    )
                    _effect.emit(LoginEffect.LoginSuccess)
                }

                is AppResult.Failure -> {
                    _state.value = _state.value.copy(
                        loading = false,
                        message = UiMessage(result.error.message, MessageLevel.Error)
                    )
                }
            }
        }
    }

    private fun onNfcCardEvent(event: InventoryMqttEvent.NfcCard) {
        val rawValue = event.value.trim()
        if (rawValue.isBlank()) return
        val readerId = event.readerId.trim()
        val now = System.currentTimeMillis()
        if (readerId.isNotBlank()) {
            val lastTs = lastNfcLoginTimestamps[readerId]
            if (lastTs != null && now - lastTs < NFC_LOGIN_DEBOUNCE_MS) {
                return
            }
            lastNfcLoginTimestamps[readerId] = now
        }

        val current = _state.value
        val transformed = transformNfcIfNeeded(rawValue, current.nfcFormatTransform)
        val resolvedCard = when {
            !current.nfcFormatTransform -> rawValue
            transformed != null -> transformed
            current.nfcFormatTransformStrict -> {
                _state.value = current.copy(
                    nfcReaderId = readerId,
                    nfcRawValue = rawValue,
                    message = UiMessage("NFC格式转换失败，请检查设备上报的Value", MessageLevel.Error)
                )
                return
            }
            else -> rawValue
        }

        _state.value = _state.value.copy(
            nfcReaderId = readerId,
            nfcRawValue = rawValue,
            nfcCardNo = resolvedCard
        )

        if (readerId.isBlank()) {
            _state.value = _state.value.copy(
                message = UiMessage("未获取到读卡器编号 ReaderId", MessageLevel.Error)
            )
            return
        }
        if (nfcLoginJob?.isActive == true || _state.value.loading) {
            return
        }

        nfcLoginJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                message = UiMessage("已收到刷卡，正在匹配设备并登录...", MessageLevel.Info),
                nfcResolvedIp = ""
            )

            when (val ipResult = repository.findCabinetIpByMac(readerId)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(nfcResolvedIp = ipResult.value)
                    loginByNfc()
                }

                is AppResult.Failure -> {
                    _state.value = _state.value.copy(
                        loading = false,
                        message = UiMessage(ipResult.error.message, MessageLevel.Error)
                    )
                }
            }
        }
    }

    private fun transformNfcIfNeeded(rawValue: String, enabled: Boolean): String? {
        if (!enabled) return rawValue
        val src = rawValue.trim()
        if (src.length < 14) return null
        val segment = src.substring(6, 14)
        if (!segment.matches(Regex("^[0-9a-fA-F]{8}$"))) return null
        return runCatching {
            segment.toLong(16).toString()
        }.getOrNull()
    }
}

data class LoginUiState(
    val tab: LoginTab = LoginTab.Account,
    val userName: String = "",
    val password: String = "",
    val nfcCardNo: String = "",
    val nfcReaderId: String = "",
    val nfcRawValue: String = "",
    val nfcResolvedIp: String = "",
    val apiBaseUrl: String = "",
    val mqttBrokerUri: String = "",
    val mqttUsername: String = "",
    val mqttPassword: String = "",
    val cabinetIpsRaw: String = "127.0.0.1",
    val nfcFormatTransform: Boolean = true,
    val nfcFormatTransformStrict: Boolean = true,
    val debugLogEnabled: Boolean = false,
    val perfLogEnabled: Boolean = false,
    val httpBodyLogEnabled: Boolean = false,
    val configExpanded: Boolean = false,
    val loading: Boolean = false,
    val savingConfig: Boolean = false,
    val message: UiMessage = UiMessage()
)

enum class LoginTab {
    Account,
    Nfc
}

sealed interface LoginEffect {
    data object LoginSuccess : LoginEffect
}
