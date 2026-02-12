package com.hv.cabinet.domain

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

data class UiMessage(
    val text: String = "",
    val level: MessageLevel = MessageLevel.Info
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

data class ScanWarning(
    val type: ScanWarningType,
    val count: Int,
    val message: String
)

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
