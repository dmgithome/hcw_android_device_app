package com.hv.cabinet.feature.take

import com.hv.cabinet.core.AppResult
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.data.api.CabinetRepository
import com.hv.cabinet.data.api.LocationOption
import com.hv.cabinet.data.api.ScanByBarcodeResponseDto
import com.hv.cabinet.data.mqtt.MqttManager
import com.hv.cabinet.data.store.AppPreferences
import com.hv.cabinet.domain.ConsumableUiModel
import com.hv.cabinet.domain.InventoryCoreState
import com.hv.cabinet.domain.MessageLevel
import com.hv.cabinet.domain.ScanWarning
import com.hv.cabinet.domain.ScanWarningType
import com.hv.cabinet.domain.UiMessage
import com.hv.cabinet.feature.BaseInventoryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TakeViewModel @Inject constructor(
    repository: CabinetRepository,
    appPreferences: AppPreferences,
    mqttManager: MqttManager
) : BaseInventoryViewModel(repository, appPreferences, mqttManager) {

    override val perfTag = "take"

    private val _takeState = MutableStateFlow(TakeUiState())
    val state: StateFlow<TakeUiState> = _takeState.asStateFlow()

    override fun stateCore(): InventoryCoreState = _takeState.value.core

    override fun updateCore(transform: (InventoryCoreState) -> InventoryCoreState) {
        val current = _takeState.value
        _takeState.value = current.copy(core = transform(current.core))
    }

    init {
        val cached = repository.peekTakeTargetLocationsCache()
        if (cached.isNotEmpty()) {
            _takeState.value = _takeState.value.copy(targetLocationOptions = cached)
        }
        loadTargetLocations(forceRefresh = cached.isEmpty())
    }

    fun updateTargetLocation(value: String) {
        val selected = _takeState.value.targetLocationOptions.firstOrNull { it.id == value }
        _takeState.value = _takeState.value.copy(
            targetLocationId = value,
            targetLocationName = selected?.name.orEmpty()
        )
    }

    override fun applyScanResult(data: ScanByBarcodeResponseDto) {
        val current = stateCore().items.associateBy { it.rfid.lowercase() }.toMutableMap()

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

        updateCore { core ->
            core.copy(
                items = current.values.sortedBy { it.name }.toList(),
                warnings = warnings,
                warningsNonce = System.nanoTime(),
                conflictRfids = core.conflictRfids.filter { conflict ->
                    current.values.any { it.rfid.equals(conflict, ignoreCase = true) }
                },
                message = if (warnings.isEmpty()) {
                    UiMessage("已更新 ${data.inStockList.size} 个取用项", MessageLevel.Success)
                } else {
                    UiMessage(warnings.joinToString("；") { it.message }, MessageLevel.Warning)
                }
            )
        }
    }

    override fun submitInternal(logoutAfter: Boolean, onDone: () -> Unit) {
        val location = _takeState.value.targetLocationId.trim()
        if (location.isBlank()) {
            updateCore { it.copy(message = UiMessage("请先选择目标位置", MessageLevel.Warning)) }
            return
        }
        if (stateCore().items.isEmpty()) {
            updateCore { it.copy(message = UiMessage("请先添加耗材", MessageLevel.Warning)) }
            return
        }
        performSubmit(
            payload = stateCore().items.map { it.payload },
            logoutAfter = logoutAfter,
            onDone = onDone,
            apiCall = { uid, items -> repository.submitTakeLog(uid, items, location) },
            successMessage = "取用提交成功",
            logoutMessage = "取用提交成功，已退出登录"
        )
    }

    private fun loadTargetLocations(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (_takeState.value.targetLocationOptions.isEmpty()) {
                _takeState.value = _takeState.value.copy(loadingTargetLocations = true)
            }
            PerfMonitor.mark("take_location_options_load_start")
            when (val result = repository.fetchTakeTargetLocations(forceRefresh = forceRefresh)) {
                is AppResult.Success -> {
                    val selectedId = _takeState.value.targetLocationId
                    val selectedName = result.value.firstOrNull { it.id == selectedId }?.name.orEmpty()
                    _takeState.value = _takeState.value.copy(
                        loadingTargetLocations = false,
                        targetLocationOptions = result.value,
                        targetLocationName = selectedName
                    )
                    PerfMonitor.mark("take_location_options_load_end")
                    PerfMonitor.measure(
                        start = "take_location_options_load_start",
                        end = "take_location_options_load_end",
                        label = "take_location_options_load_cost"
                    )
                }

                is AppResult.Failure -> {
                    _takeState.value = _takeState.value.copy(
                        loadingTargetLocations = false
                    )
                    updateCore { it.copy(message = UiMessage(result.error.message, MessageLevel.Warning)) }
                }
            }
        }
    }
}

data class TakeUiState(
    val core: InventoryCoreState = InventoryCoreState(),
    val targetLocationId: String = "",
    val targetLocationName: String = "",
    val targetLocationOptions: List<LocationOption> = emptyList(),
    val loadingTargetLocations: Boolean = false
) {
    val items get() = core.items
    val warnings get() = core.warnings
    val warningsNonce get() = core.warningsNonce
    val barcode get() = core.barcode
    val conflictRfids get() = core.conflictRfids
    val flowState get() = core.flowState
    val awaitingStartAck get() = core.awaitingStartAck
    val isInventoryBusy get() = core.isInventoryBusy
    val isSubmitting get() = core.isSubmitting
    val message get() = core.message
    val canStart get() = core.canStart
    val canSubmit get() = core.canSubmit
    val userName get() = core.userName
    val userRole get() = core.userRole
    val mqttConnected get() = core.mqttConnected
    val mqttStatusText get() = core.mqttStatusText
}
