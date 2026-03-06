package com.hv.cabinet.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hv.cabinet.core.AppError
import com.hv.cabinet.core.AppResult
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.data.api.CabinetRepository
import com.hv.cabinet.data.api.ConsumableDto
import com.hv.cabinet.data.api.ScanByBarcodeResponseDto
import com.hv.cabinet.data.mqtt.InventoryMqttEvent
import com.hv.cabinet.data.mqtt.MqttManager
import com.hv.cabinet.data.store.AppPreferences
import com.hv.cabinet.domain.InventoryCoreState
import com.hv.cabinet.domain.InventoryEventBatcher
import com.hv.cabinet.domain.InventoryFlowState
import com.hv.cabinet.domain.MessageLevel
import com.hv.cabinet.domain.UiMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.Collections

abstract class BaseInventoryViewModel(
    protected val repository: CabinetRepository,
    protected val appPreferences: AppPreferences,
    protected val mqttManager: MqttManager
) : ViewModel() {
    companion object {
        private const val START_ACK_TIMEOUT_MS = 3000L
    }

    protected abstract val perfTag: String

    protected abstract fun stateCore(): InventoryCoreState
    protected abstract fun updateCore(transform: (InventoryCoreState) -> InventoryCoreState)
    protected abstract fun applyScanResult(data: ScanByBarcodeResponseDto)
    protected abstract fun submitInternal(logoutAfter: Boolean, onDone: () -> Unit)

    private var pendingProcessJob: Job? = null
    private var startAckTimeoutJob: Job? = null
    private val screenActive = MutableStateFlow(false)
    private val pendingCodes: MutableSet<String> = Collections.synchronizedSet(linkedSetOf())
    private val inventoryBatcher = InventoryEventBatcher(viewModelScope) { batch ->
        enqueueCodes(batch)
    }
    protected var userId: Int = 0

    init {
        viewModelScope.launch {
            appPreferences.sessionFlow.collectLatest { session ->
                userId = session.userId.toIntOrNull() ?: 0
                // yield to ensure subclass properties are initialized before calling updateCore
                yield()
                updateCore { it.copy(userName = session.userName, userRole = session.userRole) }
            }
        }

        viewModelScope.launch {
            mqttManager.connectionStatus.collectLatest { status ->
                // yield to ensure subclass properties are initialized before calling updateCore
                yield()
                updateCore {
                    it.copy(
                        mqttConnected = status.connected,
                        mqttStatusText = status.badgeText
                    )
                }
            }
        }

        viewModelScope.launch {
            mqttManager.events.collectLatest { event ->
                if (!screenActive.value) return@collectLatest
                when (event) {
                    is InventoryMqttEvent.Tag -> {
                        val core = stateCore()
                        if (core.isInventoryBusy || core.awaitingStartAck) {
                            inventoryBatcher.offer(event.epc)
                        }
                    }

                    is InventoryMqttEvent.InventoryStatus -> {
                        when (event.type) {
                            "fast_inventory_start" -> {
                                startAckTimeoutJob?.cancel()
                                updateCore {
                                    it.copy(
                                        flowState = InventoryFlowState.Inventorying,
                                        isInventoryBusy = true,
                                        awaitingStartAck = false,
                                        message = UiMessage("盘点已开始，可持续放置耗材", MessageLevel.Success)
                                    )
                                }
                            }

                            "fast_inventory_end" -> {
                                updateCore {
                                    it.copy(
                                        flowState = InventoryFlowState.Idle,
                                        isInventoryBusy = false,
                                        awaitingStartAck = false,
                                        message = UiMessage("盘点结束", MessageLevel.Info)
                                    )
                                }
                            }
                        }
                    }

                    is InventoryMqttEvent.NfcCard -> Unit
                }
            }
        }
    }

    fun updateBarcode(value: String) {
        updateCore { it.copy(barcode = value) }
    }

    fun onScreenActiveChanged(active: Boolean) {
        screenActive.value = active
    }

    fun removeItem(rfid: String) {
        updateCore { core ->
            core.copy(
                items = core.items.filterNot { it.rfid == rfid },
                conflictRfids = core.conflictRfids.filterNot { it.equals(rfid, ignoreCase = true) }
            )
        }
    }

    fun addBarcode() {
        val code = stateCore().barcode.trim()
        if (code.isBlank()) {
            updateCore { it.copy(message = UiMessage("请输入条码", MessageLevel.Warning)) }
            return
        }
        updateCore { it.copy(barcode = "") }
        enqueueCode(code)
    }

    fun startInventory(forceRetry: Boolean = false) {
        if (!screenActive.value) return
        val core = stateCore()
        if (!forceRetry && (!core.canStart || core.awaitingStartAck)) return

        viewModelScope.launch {
            PerfMonitor.mark("${perfTag}_inventory_start_click")
            updateCore {
                it.copy(
                    flowState = InventoryFlowState.Starting,
                    awaitingStartAck = true,
                    message = UiMessage("正在启动盘点...", MessageLevel.Info)
                )
            }

            val subscribe = mqttManager.subscribeInventoryTopics()
            if (subscribe is AppResult.Failure) {
                updateCore {
                    it.copy(
                        flowState = InventoryFlowState.Error,
                        awaitingStartAck = false,
                        message = UiMessage(subscribe.error.message, MessageLevel.Error)
                    )
                }
                return@launch
            }

            val fallbackIps = appPreferences.deviceConfigFlow.first().cabinetIps
            when (val ipResult = repository.resolveInventoryCabinetIps(fallbackIps)) {
                is AppResult.Success -> when (val result = repository.callInventory(ipResult.value)) {
                    is AppResult.Success -> {
                        val latest = stateCore()
                        if (!latest.awaitingStartAck || latest.isInventoryBusy || latest.flowState == InventoryFlowState.Inventorying) {
                            // 设备已先返回开始状态时，不要回退到等待状态，避免出现“盘点中 + 启动超时”矛盾提示。
                            updateCore { it.copy(conflictRfids = emptyList()) }
                        } else {
                            updateCore {
                                it.copy(
                                    flowState = InventoryFlowState.WaitingAck,
                                    awaitingStartAck = true,
                                    message = UiMessage("已发送盘点命令，等待设备响应", MessageLevel.Info),
                                    conflictRfids = emptyList()
                                )
                            }
                            scheduleAckTimeout()
                        }
                    }

                    is AppResult.Failure -> {
                        updateCore {
                            it.copy(
                                flowState = InventoryFlowState.Error,
                                awaitingStartAck = false,
                                message = UiMessage(result.error.message, MessageLevel.Error)
                            )
                        }
                    }
                }

                is AppResult.Failure -> {
                    updateCore {
                        it.copy(
                            flowState = InventoryFlowState.Error,
                            awaitingStartAck = false,
                            message = UiMessage(ipResult.error.message, MessageLevel.Error)
                        )
                    }
                }
            }
        }
    }

    fun retryStartInventory() = startInventory(forceRetry = true)

    fun submit() = submitInternal(logoutAfter = false, onDone = {})

    fun submitAndLogout(onDone: () -> Unit) = submitInternal(logoutAfter = true, onDone = onDone)

    fun onScreenLeave() {
        pendingProcessJob?.cancel()
        pendingProcessJob = null
        startAckTimeoutJob?.cancel()
        startAckTimeoutJob = null
        synchronized(pendingCodes) { pendingCodes.clear() }
        inventoryBatcher.clear()
        viewModelScope.launch { mqttManager.unsubscribeInventoryTopics() }
    }

    protected fun performSubmit(
        payload: List<ConsumableDto>,
        logoutAfter: Boolean,
        onDone: () -> Unit,
        apiCall: suspend (Int, List<ConsumableDto>) -> AppResult<Unit>,
        successMessage: String,
        logoutMessage: String
    ) {
        viewModelScope.launch {
            updateCore { it.copy(flowState = InventoryFlowState.Submitting, isSubmitting = true) }
            when (val result = apiCall(userId, payload)) {
                is AppResult.Success -> {
                    if (logoutAfter) repository.logout()
                    updateCore {
                        it.copy(
                            flowState = InventoryFlowState.Completed,
                            items = emptyList(),
                            warnings = emptyList(),
                            conflictRfids = emptyList(),
                            isSubmitting = false,
                            barcode = "",
                            message = UiMessage(
                                if (logoutAfter) logoutMessage else successMessage,
                                MessageLevel.Success
                            )
                        )
                    }
                    if (logoutAfter) {
                        onDone()
                    } else {
                        delay(400)
                        updateCore { it.copy(flowState = InventoryFlowState.Idle) }
                    }
                }

                is AppResult.Failure -> {
                    val conflictRfids = (result.error as? AppError.Conflict)?.rfids.orEmpty()
                    val conflictMessage = when {
                        result.error is AppError.Conflict && conflictRfids.isNotEmpty() ->
                            "${result.error.message}（冲突RFID ${conflictRfids.size} 条）"
                        result.error is AppError.Conflict -> "${result.error.message}（并发冲突）"
                        else -> result.error.message
                    }
                    updateCore {
                        it.copy(
                            flowState = InventoryFlowState.Error,
                            isSubmitting = false,
                            conflictRfids = conflictRfids,
                            message = UiMessage(conflictMessage, MessageLevel.Error)
                        )
                    }
                }
            }
        }
    }

    private fun scheduleAckTimeout() {
        startAckTimeoutJob?.cancel()
        startAckTimeoutJob = viewModelScope.launch {
            delay(START_ACK_TIMEOUT_MS)
            if (stateCore().awaitingStartAck) {
                updateCore {
                    it.copy(
                        flowState = InventoryFlowState.Error,
                        isInventoryBusy = false,
                        awaitingStartAck = false,
                        message = UiMessage("盘点启动超时，请重试", MessageLevel.Warning)
                    )
                }
            }
        }
    }

    private fun enqueueCode(code: String) = enqueueCodes(listOf(code))

    private fun enqueueCodes(codes: List<String>) {
        val currentItems = stateCore().items
        val normalized = codes
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { code -> currentItems.any { it.rfid.equals(code, ignoreCase = true) } }
        if (normalized.isEmpty()) return
        synchronized(pendingCodes) { pendingCodes.addAll(normalized) }
        if (pendingProcessJob?.isActive == true) return
        pendingProcessJob = viewModelScope.launch { resolvePendingLoop() }
    }

    private suspend fun resolvePendingLoop() {
        while (true) {
            val batch = synchronized(pendingCodes) {
                val list = pendingCodes.toList()
                pendingCodes.clear()
                list
            }
            if (batch.isEmpty()) {
                pendingProcessJob = null
                return
            }
            when (val result = repository.resolveConsumables(batch)) {
                is AppResult.Success -> applyScanResult(result.value)
                is AppResult.Failure -> {
                    updateCore {
                        it.copy(
                            flowState = InventoryFlowState.Error,
                            message = UiMessage(result.error.message, MessageLevel.Error)
                        )
                    }
                }
            }
        }
    }
}
