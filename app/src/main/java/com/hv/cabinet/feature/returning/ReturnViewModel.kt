package com.hv.cabinet.feature.returning

import com.hv.cabinet.data.api.CabinetRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ReturnViewModel @Inject constructor(
    repository: CabinetRepository,
    appPreferences: AppPreferences,
    mqttManager: MqttManager
) : BaseInventoryViewModel(repository, appPreferences, mqttManager) {

    override val perfTag = "return"

    private val _returnState = MutableStateFlow(ReturnUiState())
    val state: StateFlow<ReturnUiState> = _returnState.asStateFlow()

    override fun stateCore(): InventoryCoreState = _returnState.value.core

    override fun updateCore(transform: (InventoryCoreState) -> InventoryCoreState) {
        val current = _returnState.value
        _returnState.value = current.copy(core = transform(current.core))
    }

    override fun applyScanResult(data: ScanByBarcodeResponseDto) {
        val current = stateCore().items.associateBy { it.rfid.lowercase() }.toMutableMap()
        val returnable = data.takenList + data.usedReturnList

        returnable.forEach { row ->
            if (row.rfid.isNotBlank()) {
                current.putIfAbsent(row.rfid.lowercase(), ConsumableUiModel.from(row))
            }
        }

        val warnings = buildList {
            if (data.inStockList.isNotEmpty()) {
                add(ScanWarning(ScanWarningType.AlreadyInStock, data.inStockList.size, "${data.inStockList.size} 个耗材已在库内，无需归还"))
            }
            if (data.notInStockList.isNotEmpty()) {
                add(ScanWarning(ScanWarningType.NotInStock, data.notInStockList.size, "${data.notInStockList.size} 个耗材无入库记录"))
            }
            if (data.consumedList.isNotEmpty()) {
                add(ScanWarning(ScanWarningType.Consumed, data.consumedList.size, "${data.consumedList.size} 个耗材已消耗"))
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
                    UiMessage("已更新 ${returnable.size} 个归还项", MessageLevel.Success)
                } else {
                    UiMessage(warnings.joinToString("；") { it.message }, MessageLevel.Warning)
                }
            )
        }
    }

    override fun submitInternal(logoutAfter: Boolean, onDone: () -> Unit) {
        if (stateCore().items.isEmpty()) {
            updateCore { it.copy(message = UiMessage("请先添加耗材", MessageLevel.Warning)) }
            return
        }
        performSubmit(
            payload = stateCore().items.map { it.payload },
            logoutAfter = logoutAfter,
            onDone = onDone,
            apiCall = { uid, items -> repository.submitReturnLog(uid, items) },
            successMessage = "归还提交成功",
            logoutMessage = "归还提交成功，已退出登录"
        )
    }
}

data class ReturnUiState(
    val core: InventoryCoreState = InventoryCoreState()
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
}
