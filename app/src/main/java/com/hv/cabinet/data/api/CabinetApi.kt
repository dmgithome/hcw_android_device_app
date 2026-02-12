package com.hv.cabinet.data.api

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CabinetApi {
    @POST("/api/v1/amis/login")
    suspend fun login(@Body request: LoginRequest): JsonElement

    @POST("/api/nfc/loginByNFC")
    suspend fun loginByNfc(@Body request: NfcLoginRequest): JsonElement

    @GET("/api/v1/amis/user_info")
    suspend fun fetchUserInfo(): JsonElement

    @GET("/api/inventory/callInventory")
    suspend fun callInventory(@Query("IP") ip: String): JsonElement

    @POST("/api/stock/getConsumeOrReturnConsumablesByBarcode")
    suspend fun resolveConsumables(@Body request: BarcodeBatchRequest): JsonElement

    @POST("/api/stock/createTakeAndReturnLog")
    suspend fun createTakeAndReturnLog(@Body request: CreateTakeAndReturnLogRequest): JsonElement
}
