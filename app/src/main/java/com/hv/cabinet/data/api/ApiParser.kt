package com.hv.cabinet.data.api

import kotlinx.serialization.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

object ApiParser {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun isSuccess(element: JsonElement): Boolean {
        val obj = element as? JsonObject ?: return true
        val status = (obj["status"] as? JsonPrimitive)?.intOrNull
        val code = (obj["code"] as? JsonPrimitive)?.intOrNull
        val success = (obj["success"] as? JsonPrimitive)?.booleanOrNull

        return when {
            success != null -> success
            status != null -> status in 200..299
            code != null -> code == 0 || code == 200
            else -> true
        }
    }

    fun message(element: JsonElement): String {
        val obj = element as? JsonObject ?: return ""
        return (obj["message"] as? JsonPrimitive)?.content
            ?: (obj["msg"] as? JsonPrimitive)?.content
            ?: ""
    }

    fun statusCode(element: JsonElement): Int? {
        val obj = element as? JsonObject ?: return null
        val statusValue = (obj["status"] as? JsonPrimitive)?.content?.toIntOrNull()
        val codeValue = (obj["code"] as? JsonPrimitive)?.content?.toIntOrNull()
        return statusValue ?: codeValue
    }

    fun conflictRfids(element: JsonElement): List<String> {
        val root = element as? JsonObject ?: return emptyList()
        val data = root["data"] as? JsonObject ?: return emptyList()
        val rfids = data["rfids"] as? JsonArray ?: return emptyList()
        return rfids.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotBlank) }
    }

    inline fun <reified T> decodeData(element: JsonElement): T {
        val kSerializer = serializer<T>()
        val obj = element as? JsonObject
        if (obj != null && obj["data"] != null) {
            runCatching {
                return json.decodeFromJsonElement(kSerializer, obj["data"]!!)
            }
        }
        return json.decodeFromJsonElement(kSerializer, element)
    }
}
