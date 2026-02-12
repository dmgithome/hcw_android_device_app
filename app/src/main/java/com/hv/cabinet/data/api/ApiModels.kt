package com.hv.cabinet.data.api

import com.hv.cabinet.core.IntOrStringSerializer
import com.hv.cabinet.core.StringOrNumberSerializer
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val userName: String,
    val password: String
)

@Serializable
data class NfcLoginRequest(
    val IP: String,
    val cardNumber: String
)

@Serializable
data class LoginToken(
    val token: String = "",
    val refreshToken: String = ""
)

@Serializable
data class UserInfoDto(
    @Serializable(with = StringOrNumberSerializer::class)
    val userId: String = "",
    val userName: String = "",
    val userRole: String = ""
)

@Serializable
data class BarcodeBatchRequest(
    val dataList: List<String>
)

@Serializable
data class ConsumableDto(
    @Serializable(with = IntOrStringSerializer::class)
    val id: Int = 0,
    val rfid: String = "",
    val name: String = "",
    val code: String = "",
    val spec: String = "",
    val batch: String = "",
    val brand: String = "",
    val manufacturer: String = "",
    val unit: String = "",
    @Serializable(with = IntOrStringSerializer::class)
    val cabinetID: Int = 0,
    @Serializable(with = StringOrNumberSerializer::class)
    val current_location_id: String = "",
    val current_location_name: String = "",
    @Serializable(with = StringOrNumberSerializer::class)
    val from_location_id: String = "",
    val from_location_name: String = ""
)

@Serializable
data class ScanByBarcodeResponseDto(
    val notInStockList: List<String> = emptyList(),
    val inStockList: List<ConsumableDto> = emptyList(),
    val takenList: List<ConsumableDto> = emptyList(),
    val consumedList: List<ConsumableDto> = emptyList(),
    val usedReturnList: List<ConsumableDto> = emptyList()
)

@Serializable
data class SubmitLineItem(
    val id: Int,
    val rfid: String,
    val batch: String = "",
    val code: String = "",
    val name: String = "",
    val spec: String = "",
    val manufacturer: String = "",
    val brand: String = "",
    val unit: String = "",
    val number: Int = 1,
    val expected_current_location_id: String? = null
)

@Serializable
data class CreateTakeAndReturnLogRequest(
    val cabinetID: Int,
    val userID: Int,
    val openDoorTime: String,
    val closeDoorTime: String,
    val takeList: List<SubmitLineItem>,
    val returnList: List<SubmitLineItem>,
    val to_location_id: String? = null
)
