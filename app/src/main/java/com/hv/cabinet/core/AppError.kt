package com.hv.cabinet.core

sealed class AppError(
    open val message: String,
    open val code: Int? = null,
    open val recoverable: Boolean = true
) {
    data class Network(
        override val message: String = "网络异常",
        override val code: Int? = null
    ) : AppError(message = message, code = code, recoverable = true)

    data class Business(
        override val message: String,
        override val code: Int? = null
    ) : AppError(message = message, code = code, recoverable = true)

    data class Conflict(
        override val message: String = "位置不一致",
        val rfids: List<String> = emptyList(),
        override val code: Int? = 409
    ) : AppError(message = message, code = code, recoverable = true)

    data class Unknown(
        override val message: String = "未知错误",
        override val code: Int? = null
    ) : AppError(message = message, code = code, recoverable = false)
}
