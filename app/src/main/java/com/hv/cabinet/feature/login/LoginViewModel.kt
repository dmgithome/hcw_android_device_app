package com.hv.cabinet.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hv.cabinet.core.AppResult
import com.hv.cabinet.data.api.CabinetRepository
import com.hv.cabinet.data.mqtt.InventoryMqttEvent
import com.hv.cabinet.data.mqtt.MqttManager
import com.hv.cabinet.data.store.AppPreferences
import com.hv.cabinet.data.store.DeviceConfig
import com.hv.cabinet.domain.MessageLevel
import com.hv.cabinet.domain.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            appPreferences.deviceConfigFlow.collectLatest { cfg ->
                _state.value = _state.value.copy(
                    apiBaseUrl = cfg.apiBaseUrl,
                    mqttBrokerUri = cfg.mqttBrokerUri,
                    mqttUsername = cfg.mqttUsername,
                    mqttPassword = cfg.mqttPassword,
                    cabinetIpsRaw = cfg.cabinetIps.joinToString(",")
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
                if (event is InventoryMqttEvent.NfcCard && event.cardNo.isNotBlank()) {
                    _state.value = _state.value.copy(nfcCardNo = event.cardNo)
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

    fun updateNfcIp(value: String) {
        _state.value = _state.value.copy(nfcIp = value)
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
                    cabinetIps = if (ips.isEmpty()) listOf("127.0.0.1") else ips
                )
            )
            _state.value = _state.value.copy(
                savingConfig = false,
                message = UiMessage("设备配置已保存", MessageLevel.Success)
            )
        }
    }

    fun login() {
        when (_state.value.tab) {
            LoginTab.Account -> loginByAccount()
            LoginTab.Nfc -> loginByNfc()
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
        val cardNo = _state.value.nfcCardNo.trim()
        val ip = _state.value.nfcIp.trim()
        if (ip.isBlank() || cardNo.isBlank()) {
            _state.value = _state.value.copy(
                message = UiMessage("请输入NFC读卡IP和卡号", MessageLevel.Warning)
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = UiMessage())
            when (val result = repository.loginByNfc(ip = ip, cardNumber = cardNo)) {
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
}

data class LoginUiState(
    val tab: LoginTab = LoginTab.Account,
    val userName: String = "",
    val password: String = "",
    val nfcCardNo: String = "",
    val nfcIp: String = "",
    val apiBaseUrl: String = "",
    val mqttBrokerUri: String = "",
    val mqttUsername: String = "",
    val mqttPassword: String = "",
    val cabinetIpsRaw: String = "127.0.0.1",
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
