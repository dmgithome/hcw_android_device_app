package com.hv.cabinet.data.api

import com.hv.cabinet.core.AppDispatchers
import com.hv.cabinet.core.AppError
import com.hv.cabinet.core.AppResult
import com.hv.cabinet.data.store.AppPreferences
import com.hv.cabinet.data.store.SessionInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.InterruptedIOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CabinetRepository @Inject constructor(
    private val api: CabinetApi,
    private val appPreferences: AppPreferences,
    private val dispatchers: AppDispatchers
) {
    suspend fun loginByAccount(userName: String, password: String): AppResult<SessionInfo> =
        performLogin(
            loginCall = { api.login(LoginRequest(userName = userName, password = password)) },
            fallbackMessage = "账号登录失败"
        )

    suspend fun loginByNfc(ip: String, cardNumber: String, mac: String = ""): AppResult<SessionInfo> =
        performLogin(
            loginCall = { api.loginByNfc(NfcLoginRequest(IP = ip, cardNumber = cardNumber, MAC = mac)) },
            fallbackMessage = "NFC 登录失败"
        )

    private suspend fun performLogin(
        loginCall: suspend () -> JsonElement,
        fallbackMessage: String
    ): AppResult<SessionInfo> = withContext(dispatchers.io) {
        runCatching {
            val tokenResponse = loginCall()
            if (!ApiParser.isSuccess(tokenResponse)) {
                return@withContext AppResult.Failure(
                    AppError.Business(ApiParser.message(tokenResponse).ifBlank { fallbackMessage })
                )
            }

            val token = ApiParser.decodeData<LoginToken>(tokenResponse)
            if (token.token.isBlank()) {
                return@withContext AppResult.Failure(AppError.Business("登录令牌为空"))
            }

            appPreferences.saveSession(
                SessionInfo(
                    token = token.token,
                    refreshToken = token.refreshToken,
                    userId = "",
                    userName = "",
                    userRole = ""
                )
            )

            val userInfoResponse = api.fetchUserInfo()
            if (!ApiParser.isSuccess(userInfoResponse)) {
                appPreferences.clearSession()
                return@withContext AppResult.Failure(
                    AppError.Business(ApiParser.message(userInfoResponse).ifBlank { "获取用户信息失败" })
                )
            }
            val user = ApiParser.decodeData<UserInfoDto>(userInfoResponse)
            val session = SessionInfo(
                token = token.token,
                refreshToken = token.refreshToken,
                userId = user.userId,
                userName = user.userName,
                userRole = user.userRole
            )
            appPreferences.saveSession(session)
            AppResult.Success(session)
        }.getOrElse {
            runCatching { appPreferences.clearSession() }
            Timber.e(it, "login failed: $fallbackMessage")
            AppResult.Failure(mapThrowable(it, fallback = fallbackMessage))
        }
    }

    suspend fun findCabinetIpByMac(macAddress: String): AppResult<String> = withContext(dispatchers.io) {
        val mac = macAddress.trim()
        if (mac.isBlank()) {
            return@withContext AppResult.Failure(AppError.Business("未获取到读卡器编号"))
        }
        when (val result = fetchCabinets()) {
            is AppResult.Success -> {
                val ip = result.value.firstOrNull {
                    it.macAddress.equals(mac, ignoreCase = true)
                }?.ipAddress?.trim().orEmpty()
                if (ip.isBlank()) {
                    AppResult.Failure(AppError.Business("未找到该地址对应的设备信息，请先将设备添加到系统中"))
                } else {
                    AppResult.Success(ip)
                }
            }

            is AppResult.Failure -> AppResult.Failure(result.error)
        }
    }

    suspend fun resolveInventoryCabinetIps(fallbackIps: List<String>): AppResult<List<String>> = withContext(dispatchers.io) {
        val fallback = fallbackIps.map { it.trim() }.filter { it.isNotBlank() }
        when (val result = fetchCabinets()) {
            is AppResult.Success -> {
                val devices = result.value
                if (devices.isNotEmpty()) {
                    val mainIp = devices.firstOrNull {
                        it.type.equals("main", ignoreCase = true) && it.ipAddress.isNotBlank()
                    }?.ipAddress?.trim()
                    if (!mainIp.isNullOrBlank()) {
                        return@withContext AppResult.Success(listOf(mainIp))
                    }
                    val allIps = devices.mapNotNull { it.ipAddress.trim().takeIf(String::isNotBlank) }.distinct()
                    if (allIps.isNotEmpty()) {
                        return@withContext AppResult.Success(allIps)
                    }
                }
                if (fallback.isNotEmpty()) AppResult.Success(fallback)
                else AppResult.Failure(AppError.Business("未找到可用柜体IP"))
            }

            is AppResult.Failure -> {
                if (fallback.isNotEmpty()) AppResult.Success(fallback) else AppResult.Failure(result.error)
            }
        }
    }

    suspend fun fetchTakeTargetLocations(): AppResult<List<LocationOption>> = withContext(dispatchers.io) {
        runCatching {
            val listResp = api.listLocations()
            if (!ApiParser.isSuccess(listResp)) {
                return@withContext AppResult.Failure(
                    AppError.Business(ApiParser.message(listResp).ifBlank { "获取地点列表失败" })
                )
            }

            val defaultLocationId = runCatching {
                val defaultResp = api.getDefaultLocation()
                if (ApiParser.isSuccess(defaultResp)) decodeDefaultLocationId(defaultResp) else null
            }.getOrNull()

            val items = decodeLocationItems(listResp)
                .filter { it.is_active }
                .filter { (it.type ?: "").equals("room", ignoreCase = true) }
                .filterNot { it.is_default_inbound_return || (defaultLocationId != null && it.id == defaultLocationId) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.lowercase(Locale.getDefault()) })
                .map { LocationOption(id = it.id, name = it.name.ifBlank { it.id }) }

            AppResult.Success(items)
        }.getOrElse {
            Timber.e(it, "fetchTakeTargetLocations failed")
            AppResult.Failure(mapThrowable(it, fallback = "获取地点列表失败"))
        }
    }

    private suspend fun fetchCabinets(cabinetGroupID: Int = 1): AppResult<List<CabinetDeviceDto>> = withContext(dispatchers.io) {
        runCatching {
            val resp = api.fetchCabinets(cabinetGroupID)
            if (!ApiParser.isSuccess(resp)) {
                return@withContext AppResult.Failure(
                    AppError.Business(ApiParser.message(resp).ifBlank { "获取设备列表失败" })
                )
            }
            AppResult.Success(ApiParser.decodeData<List<CabinetDeviceDto>>(resp))
        }.getOrElse {
            Timber.e(it, "fetchCabinets failed")
            AppResult.Failure(mapThrowable(it, fallback = "获取设备列表失败"))
        }
    }

    suspend fun logout() = withContext(dispatchers.io) {
        appPreferences.clearSession()
    }

    suspend fun callInventory(cabinetIps: List<String>): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            val ips = cabinetIps.filter { it.isNotBlank() }
            if (ips.isEmpty()) return@withContext AppResult.Success(Unit)

            val results = coroutineScope {
                ips.map { ip ->
                    async { ip to api.callInventory(ip) }
                }.map { it.await() }
            }

            for ((ip, resp) in results) {
                if (!ApiParser.isSuccess(resp)) {
                    return@withContext AppResult.Failure(
                        AppError.Business(ApiParser.message(resp).ifBlank { "启动盘点失败: $ip" })
                    )
                }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(mapThrowable(e, fallback = "启动盘点失败"))
        }
    }

    suspend fun resolveConsumables(codes: List<String>): AppResult<ScanByBarcodeResponseDto> = withContext(dispatchers.io) {
        try {
            val normalized = codes.map { it.trim() }.filter { it.isNotBlank() }
            if (normalized.isEmpty()) {
                return@withContext AppResult.Success(ScanByBarcodeResponseDto())
            }
            val resp = api.resolveConsumables(BarcodeBatchRequest(normalized))
            if (!ApiParser.isSuccess(resp)) {
                return@withContext AppResult.Failure(
                    AppError.Business(ApiParser.message(resp).ifBlank { "耗材解析失败" })
                )
            }
            AppResult.Success(ApiParser.decodeData<ScanByBarcodeResponseDto>(resp))
        } catch (e: Exception) {
            Timber.e(e, "resolveConsumables failed")
            AppResult.Failure(mapThrowable(e, fallback = "耗材解析失败"))
        }
    }

    suspend fun submitTakeLog(
        userId: Int,
        items: List<ConsumableDto>,
        toLocationId: String
    ): AppResult<Unit> = withContext(dispatchers.io) {
        submitByCabinet(userId, items, takeMode = true, toLocationId = toLocationId)
    }

    suspend fun submitReturnLog(userId: Int, items: List<ConsumableDto>): AppResult<Unit> = withContext(dispatchers.io) {
        submitByCabinet(userId, items, takeMode = false, toLocationId = null)
    }

    private suspend fun submitByCabinet(
        userId: Int,
        items: List<ConsumableDto>,
        takeMode: Boolean,
        toLocationId: String?
    ): AppResult<Unit> {
        if (items.isEmpty()) {
            return AppResult.Failure(AppError.Business("请先添加耗材"))
        }

        return try {
            val now = nowTime()
            val grouped = items.groupBy { it.cabinetID }
            for ((cabinetId, rows) in grouped) {
                val lineItems = rows.map { row ->
                    SubmitLineItem(
                        id = row.id,
                        rfid = row.rfid,
                        batch = row.batch,
                        code = row.code,
                        name = row.name,
                        spec = row.spec,
                        manufacturer = row.manufacturer,
                        brand = row.brand,
                        unit = row.unit,
                        number = 1,
                        expected_current_location_id = row.current_location_id.ifBlank { null }
                    )
                }

                val request = CreateTakeAndReturnLogRequest(
                    cabinetID = cabinetId,
                    userID = userId,
                    openDoorTime = now,
                    closeDoorTime = now,
                    takeList = if (takeMode) lineItems else emptyList(),
                    returnList = if (takeMode) emptyList() else lineItems,
                    to_location_id = if (takeMode) toLocationId else null
                )

                val resp = api.createTakeAndReturnLog(request)
                if (!ApiParser.isSuccess(resp)) {
                    val apiMessage = ApiParser.message(resp)
                    val apiCode = ApiParser.statusCode(resp)
                    val conflictRfids = ApiParser.conflictRfids(resp)
                    val conflict = apiCode == 409 || apiMessage.contains("位置不一致") || conflictRfids.isNotEmpty()
                    return AppResult.Failure(
                        if (conflict) {
                            AppError.Conflict(
                                message = apiMessage.ifBlank { "位置不一致" },
                                rfids = conflictRfids
                            )
                        } else {
                            AppError.Business(
                                message = if (apiMessage.isNotBlank()) apiMessage else "提交失败: 柜体 $cabinetId",
                                code = apiCode
                            )
                        }
                    )
                }
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "submitByCabinet failed")
            if (e is HttpException && e.code() == 409) {
                val rawBody = readHttpErrorBody(e)
                val rfids = parseConflictRfidsFromRaw(rawBody)
                val message = parseHttpMessageRaw(rawBody).ifBlank { "位置不一致" }
                return AppResult.Failure(AppError.Conflict(message = message, rfids = rfids))
            }
            AppResult.Failure(mapThrowable(e, fallback = "提交失败"))
        }
    }

    private fun nowTime(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    private fun decodeLocationItems(element: JsonElement): List<LocationItemDto> {
        val serializer = ListSerializer(LocationItemDto.serializer())
        val root = element as? JsonObject
        val candidates = buildList {
            if (root != null) {
                root["data"]?.let { add(it) }
                root["items"]?.let { add(it) }
                (root["data"] as? JsonObject)?.get("items")?.let { add(it) }
            } else {
                add(element)
            }
        }
        candidates.forEach { candidate ->
            if (candidate is JsonArray) {
                runCatching {
                    return ApiParser.json.decodeFromJsonElement(serializer, candidate)
                }
            }
        }
        return emptyList()
    }

    private fun decodeDefaultLocationId(element: JsonElement): String? {
        val root = element as? JsonObject ?: return null
        val data = root["data"] ?: root
        return when (data) {
            is JsonObject -> runCatching {
                ApiParser.json.decodeFromJsonElement(DefaultLocationDto.serializer(), data).id.ifBlank { null }
            }.getOrNull()

            else -> null
        }
    }

    private fun mapThrowable(throwable: Throwable, fallback: String): AppError {
        return when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                val bodyMessage = parseHttpMessage(throwable)
                val message = when (code) {
                    401 -> bodyMessage.ifBlank { "登录已失效，请重新登录" }
                    403 -> bodyMessage.ifBlank { "当前账号无访问权限(403)" }
                    404 -> bodyMessage.ifBlank { "接口地址不可用(404)，请检查后端地址与路由" }
                    408 -> bodyMessage.ifBlank { "请求超时，请重试" }
                    in 500..599 -> bodyMessage.ifBlank { "服务器异常($code)，请稍后重试" }
                    else -> bodyMessage.ifBlank { "$fallback ($code)" }
                }
                AppError.Network(message = message, code = code)
            }

            is SocketTimeoutException,
            is InterruptedIOException -> AppError.Network("请求超时，请检查网络后重试")

            is UnknownHostException -> AppError.Network("无法解析服务器地址，请检查 API 配置")

            is ConnectException -> AppError.Network("连接服务器失败，请确认后端已启动且网络可达")

            else -> AppError.Network(throwable.message ?: fallback)
        }
    }

    private fun parseHttpMessage(throwable: HttpException): String {
        val raw = readHttpErrorBody(throwable)
        return parseHttpMessageRaw(raw)
    }

    private fun parseHttpMessageRaw(raw: String): String {
        if (raw.isBlank()) return ""
        return runCatching {
            val json = JSONObject(raw)
            json.optString("message")
                .ifBlank { json.optString("msg") }
                .ifBlank { json.optString("error") }
                .ifBlank { raw.take(120) }
        }.getOrDefault(raw.take(120))
    }

    private fun parseConflictRfidsFromRaw(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val root = ApiParser.json.parseToJsonElement(raw)
            ApiParser.conflictRfids(root)
        }.getOrDefault(emptyList())
    }

    private fun readHttpErrorBody(throwable: HttpException): String {
        return runCatching { throwable.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
    }
}
