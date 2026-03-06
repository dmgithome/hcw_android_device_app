package com.hv.cabinet.domain

import androidx.compose.runtime.Immutable
import com.hv.cabinet.data.api.ConsumableDto

enum class InventoryMode {
    TAKE,
    RETURN
}

enum class InventoryFlowState {
    Idle,
    Starting,
    WaitingAck,
    Inventorying,
    Submitting,
    Completed,
    Error
}

enum class MessageLevel {
    Info,
    Success,
    Warning,
    Error
}

@Immutable
data class UiMessage(
    val text: String = "",
    val level: MessageLevel = MessageLevel.Info,
    val nonce: Long = System.nanoTime()
) {
    val visible: Boolean
        get() = text.isNotBlank()
}

enum class ScanWarningType {
    AlreadyTaken,
    AlreadyInStock,
    NotInStock,
    Consumed,
    UsedReturn,
    Unknown
}

@Immutable
data class ScanWarning(
    val type: ScanWarningType,
    val count: Int,
    val message: String
)

@Immutable
data class InventoryCoreState(
    val items: List<ConsumableUiModel> = emptyList(),
    val warnings: List<ScanWarning> = emptyList(),
    val warningsNonce: Long = 0L,
    val barcode: String = "",
    val conflictRfids: List<String> = emptyList(),
    val flowState: InventoryFlowState = InventoryFlowState.Idle,
    val awaitingStartAck: Boolean = false,
    val isInventoryBusy: Boolean = false,
    val isSubmitting: Boolean = false,
    val message: UiMessage = UiMessage(),
    val userName: String = "",
    val userRole: String = "",
    val mqttConnected: Boolean? = null,
    val mqttStatusText: String = "MQTT 探测中"
) {
    val canStart: Boolean
        get() = !isSubmitting && !isInventoryBusy && flowState != InventoryFlowState.Starting && flowState != InventoryFlowState.WaitingAck
    val canSubmit: Boolean
        get() = !isSubmitting && items.isNotEmpty()
}

@Immutable
data class ConsumableUiModel(
    val id: Int,
    val rfid: String,
    val code: String,
    val name: String,
    val cabinetId: Int,
    val currentLocationName: String,
    val currentLocationId: String,
    val payload: ConsumableDto
) {
    companion object {
        fun from(dto: ConsumableDto): ConsumableUiModel {
            return ConsumableUiModel(
                id = dto.id,
                rfid = dto.rfid,
                code = dto.code,
                name = dto.name,
                cabinetId = dto.cabinetID,
                currentLocationName = dto.current_location_name,
                currentLocationId = dto.current_location_id,
                payload = dto
            )
        }
    }
}

data class PerfTarget(
    val pointerFeedbackMsP75: Int = 100,
    val interactiveMsP75: Int = 250,
    val jankyFramesPercent: Int = 35
)
