package com.hv.cabinet.feature.take

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hv.cabinet.core.AppResult
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.data.api.CabinetRepository
import com.hv.cabinet.data.api.ScanByBarcodeResponseDto
import com.hv.cabinet.data.mqtt.InventoryMqttEvent
import com.hv.cabinet.data.mqtt.MqttManager
import com.hv.cabinet.data.store.AppPreferences
import com.hv.cabinet.domain.ConsumableUiModel
import com.hv.cabinet.domain.InventoryFlowState
import com.hv.cabinet.domain.MessageLevel
import com.hv.cabinet.domain.ScanWarning
import com.hv.cabinet.domain.ScanWarningType
import com.hv.cabinet.domain.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TakeViewModel @Inject constructor(
    private val repository: CabinetRepository,
    private val appPreferences: AppPreferences,
    private val mqttManager: MqttManager
) : ViewModel() {
    companion object {
        private const val START_ACK_TIMEOUT_MS = 3000L
    }

    private val _state = MutableStateFlow(TakeUiState())
    val state: StateFlow<TakeUiState> = _state.asStateFlow()

    private var pendingProcessJob: Job? = null
    private var startAckTimeoutJob: Job? = null
    private val pendingCodes = linkedSetOf<String>()
    private var userId: Int = 0

    init {
        viewModelScope.launch {
            appPreferences.sessionFlow.collectLatest { session ->
                userId = session.userId.toIntOrNull() ?: 0
            }
        }

        viewModelScope.launch {
            mqttManager.events.collectLatest { event ->
                when (event) {
                    is InventoryMqttEvent.Tag -> {
                        if (_state.value.isInventoryBusy || _state.value.awaitingStartAck) {
                            enqueueCode(event.epc)
                        }
                    }

                    is InventoryMqttEvent.InventoryStatus -> {
                        when (event.type) {
                            "fast_inventory_start" -> {
                                startAckTimeoutJob?.cancel()
                                _state.value = _state.value.copy(
                                    flowState = InventoryFlowState.Inventorying,
                                    isInventoryBusy = true,
                                    awaitingStartAck = false,
                                    message = UiMessage("盘点已开始，可持续放置耗材", MessageLevel.Success)
                                )
                            }

                            "fast_inventory_end" -> {
                                _state.value = _state.value.copy(
                                    flowState = InventoryFlowState.Idle,
                                    isInventoryBusy = false,
                                    awaitingStartAck = false,
                                    message = UiMessage("盘点结束", MessageLevel.Info)
                                )
                            }
                        }
                    }

                    is InventoryMqttEvent.NfcCard -> Unit
                }
            }
        }
    }

    fun updateBarcode(value: String) {
        _state.value = _state.value.copy(barcode = value)
    }

    fun updateTargetLocation(value: String) {
        _state.value = _state.value.copy(targetLocationId = value)
    }

    fun removeItem(rfid: String) {
        _state.value = _state.value.copy(items = _state.value.items.filterNot { it.rfid == rfid })
    }

    fun addBarcode() {
        val code = _state.value.barcode.trim()
        if (code.isBlank()) {
            _state.value = _state.value.copy(message = UiMessage("请输入条码", MessageLevel.Warning))
            return
        }
        _state.value = _state.value.copy(barcode = "")
        enqueueCode(code)
    }

    fun startInventory(forceRetry: Boolean = false) {
        val current = _state.value
        if (!forceRetry && (!current.canStart || current.awaitingStartAck)) {
            return
        }

        viewModelScope.launch {
            PerfMonitor.mark("take_inventory_start_click")
            _state.value = _state.value.copy(
                flowState = InventoryFlowState.Starting,
                awaitingStartAck = true,
                message = UiMessage("正在启动盘点...", MessageLevel.Info)
            )

            val subscribe = mqttManager.subscribeInventoryTopics()
            if (subscribe is AppResult.Failure) {
                _state.value = _state.value.copy(
                    flowState = InventoryFlowState.Error,
                    awaitingStartAck = false,
                    message = UiMessage(subscribe.error.message, MessageLevel.Error)
                )
                return@launch
            }

            val ips = appPreferences.deviceConfigFlow.first().cabinetIps
            when (val result = repository.callInventory(ips)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        flowState = InventoryFlowState.WaitingAck,
                        awaitingStartAck = true,
                        message = UiMessage("已发送盘点命令，等待设备响应", MessageLevel.Info)
                    )
                    scheduleAckTimeout()
                }

                is AppResult.Failure -> {
                    _state.value = _state.value.copy(
                        flowState = InventoryFlowState.Error,
                        awaitingStartAck = false,
                        message = UiMessage(result.error.message, MessageLevel.Error)
                    )
                }
            }
        }
    }

    fun retryStartInventory() {
        startInventory(forceRetry = true)
    }

    fun submit() {
        submitInternal(logoutAfter = false, onDone = {})
    }

    fun submitAndLogout(onDone: () -> Unit) {
        submitInternal(logoutAfter = true, onDone = onDone)
    }

    fun onScreenLeave() {
        pendingProcessJob?.cancel()
        startAckTimeoutJob?.cancel()
        viewModelScope.launch {
            mqttManager.unsubscribeInventoryTopics()
        }
    }

    private fun submitInternal(
        logoutAfter: Boolean,
        onDone: () -> Unit
    ) {
        val location = _state.value.targetLocationId.trim()
        if (location.isBlank()) {
            _state.value = _state.value.copy(
                message = UiMessage("请先输入目标位置ID", MessageLevel.Warning)
            )
            return
        }

        if (_state.value.items.isEmpty()) {
            _state.value = _state.value.copy(
                message = UiMessage("请先添加耗材", MessageLevel.Warning)
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                flowState = InventoryFlowState.Submitting,
                isSubmitting = true
            )
            val payload = _state.value.items.map { it.payload }
            when (val result = repository.submitTakeLog(userId, payload, location)) {
                is AppResult.Success -> {
                    if (logoutAfter) {
                        repository.logout()
                    }
                    _state.value = _state.value.copy(
                        flowState = InventoryFlowState.Completed,
                        items = emptyList(),
                        warnings = emptyList(),
                        isSubmitting = false,
                        message = UiMessage(
                            if (logoutAfter) "取用提交成功，已退出登录" else "取用提交成功",
                            MessageLevel.Success
                        )
                    )
                    if (logoutAfter) {
                        onDone()
                    } else {
                        delay(400)
                        _state.value = _state.value.copy(flowState = InventoryFlowState.Idle)
                    }
                }

                is AppResult.Failure -> {
                    _state.value = _state.value.copy(
                        flowState = InventoryFlowState.Error,
                        isSubmitting = false,
                        message = UiMessage(result.error.message, MessageLevel.Error)
                    )
                }
            }
        }
    }

    private fun scheduleAckTimeout() {
        startAckTimeoutJob?.cancel()
        startAckTimeoutJob = viewModelScope.launch {
            delay(START_ACK_TIMEOUT_MS)
            if (_state.value.awaitingStartAck) {
                _state.value = _state.value.copy(
                    flowState = InventoryFlowState.Error,
                    awaitingStartAck = false,
                    message = UiMessage("盘点启动超时，请重试", MessageLevel.Warning)
                )
            }
        }
    }

    private fun enqueueCode(code: String) {
        if (_state.value.items.any { it.rfid.equals(code, ignoreCase = true) }) {
            return
        }

        pendingCodes.add(code)
        if (pendingProcessJob?.isActive == true) return
        pendingProcessJob = viewModelScope.launch { resolvePendingLoop() }
    }

    private suspend fun resolvePendingLoop() {
        while (true) {
            val batch = pendingCodes.toList()
            pendingCodes.clear()
            if (batch.isEmpty()) {
                pendingProcessJob = null
                return
            }

            when (val result = repository.resolveConsumables(batch)) {
                is AppResult.Success -> applyScanResult(result.value)
                is AppResult.Failure -> {
                    _state.value = _state.value.copy(
                        flowState = InventoryFlowState.Error,
                        message = UiMessage(result.error.message, MessageLevel.Error)
                    )
                }
            }
        }
    }

    private fun applyScanResult(data: ScanByBarcodeResponseDto) {
        val current = _state.value.items.associateBy { it.rfid.lowercase() }.toMutableMap()

        data.inStockList.forEach { row ->
            if (row.rfid.isNotBlank()) {
                current.putIfAbsent(row.rfid.lowercase(), ConsumableUiModel.from(row))
            }
        }

        val warnings = buildList {
            if (data.takenList.isNotEmpty()) {
                add(ScanWarning(ScanWarningType.AlreadyTaken, data.takenList.size, "${data.takenList.size} 个耗材已在取用流程中"))
            }
            if (data.notInStockList.isNotEmpty()) {
                add(ScanWarning(ScanWarningType.NotInStock, data.notInStockList.size, "${data.notInStockList.size} 个耗材未入库"))
            }
            if (data.consumedList.isNotEmpty()) {
                add(ScanWarning(ScanWarningType.Consumed, data.consumedList.size, "${data.consumedList.size} 个耗材已消耗"))
            }
            if (data.usedReturnList.isNotEmpty()) {
                add(ScanWarning(ScanWarningType.UsedReturn, data.usedReturnList.size, "${data.usedReturnList.size} 个耗材为消耗退回"))
            }
        }

        _state.value = _state.value.copy(
            items = current.values.sortedBy { it.name }.toList(),
            warnings = warnings,
            message = if (warnings.isEmpty()) {
                UiMessage("已更新 ${data.inStockList.size} 个取用项", MessageLevel.Success)
            } else {
                UiMessage(warnings.joinToString("；") { it.message }, MessageLevel.Warning)
            }
        )
    }
}

data class TakeUiState(
    val items: List<ConsumableUiModel> = emptyList(),
    val warnings: List<ScanWarning> = emptyList(),
    val barcode: String = "",
    val targetLocationId: String = "",
    val flowState: InventoryFlowState = InventoryFlowState.Idle,
    val awaitingStartAck: Boolean = false,
    val isInventoryBusy: Boolean = false,
    val isSubmitting: Boolean = false,
    val message: UiMessage = UiMessage()
) {
    val canStart: Boolean
        get() = !isSubmitting && !isInventoryBusy && flowState != InventoryFlowState.Starting && flowState != InventoryFlowState.WaitingAck

    val canSubmit: Boolean
        get() = !isSubmitting && items.isNotEmpty()
}
