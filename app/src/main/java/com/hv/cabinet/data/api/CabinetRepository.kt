package com.hv.cabinet.data.api

import com.hv.cabinet.core.AppDispatchers
import com.hv.cabinet.core.AppError
import com.hv.cabinet.core.AppResult
import com.hv.cabinet.data.store.AppPreferences
import com.hv.cabinet.data.store.SessionInfo
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.InterruptedIOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CabinetRepository @Inject constructor(
    private val api: CabinetApi,
    private val appPreferences: AppPreferences,
    private val dispatchers: AppDispatchers
) {
    suspend fun loginByAccount(userName: String, password: String): AppResult<SessionInfo> = withContext(dispatchers.io) {
        runCatching {
            val tokenResponse = api.login(LoginRequest(userName = userName, password = password))
            if (!ApiParser.isSuccess(tokenResponse)) {
                return@withContext AppResult.Failure(
                    AppError.Business(ApiParser.message(tokenResponse).ifBlank { "账号登录失败" })
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
            Timber.e(it, "loginByAccount failed")
            AppResult.Failure(mapThrowable(it, fallback = "账号登录失败"))
        }
    }

    suspend fun loginByNfc(ip: String, cardNumber: String): AppResult<SessionInfo> = withContext(dispatchers.io) {
        runCatching {
            val tokenResponse = api.loginByNfc(NfcLoginRequest(IP = ip, cardNumber = cardNumber))
            if (!ApiParser.isSuccess(tokenResponse)) {
                return@withContext AppResult.Failure(
                    AppError.Business(ApiParser.message(tokenResponse).ifBlank { "NFC 登录失败" })
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
            Timber.e(it, "loginByNfc failed")
            AppResult.Failure(mapThrowable(it, fallback = "NFC 登录失败"))
        }
    }

    suspend fun logout() = withContext(dispatchers.io) {
        appPreferences.clearSession()
    }

    suspend fun callInventory(cabinetIps: List<String>): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            cabinetIps.filter { it.isNotBlank() }.forEach { ip ->
                val resp = api.callInventory(ip)
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
                    return AppResult.Failure(
                        AppError.Business(ApiParser.message(resp).ifBlank { "提交失败: 柜体 $cabinetId" })
                    )
                }
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "submitByCabinet failed")
            AppResult.Failure(mapThrowable(e, fallback = "提交失败"))
        }
    }

    private fun nowTime(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
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
        val raw = runCatching { throwable.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
        if (raw.isBlank()) return ""
        return runCatching {
            val json = JSONObject(raw)
            json.optString("message")
                .ifBlank { json.optString("msg") }
                .ifBlank { json.optString("error") }
                .ifBlank { raw.take(120) }
        }.getOrDefault(raw.take(120))
    }
}
