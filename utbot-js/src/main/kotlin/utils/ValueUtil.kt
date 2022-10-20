package utils

import org.json.JSONException
import org.json.JSONObject
import framework.api.js.JsClassId
import framework.api.js.util.jsBooleanClassId
import framework.api.js.util.jsErrorClassId
import framework.api.js.util.jsNumberClassId
import framework.api.js.util.jsStringClassId
import framework.api.js.util.jsUndefinedClassId

fun String.toJsAny(returnType: JsClassId): Pair<Any?, JsClassId> {
    return when {
        this == "true" || this == "false" -> toBoolean() to jsBooleanClassId
        this == "null" || this == "undefined" -> null to jsUndefinedClassId
        Regex("^.*Error:.*").matches(this) -> this.replace("Error:", "") to jsErrorClassId
        Regex("\".*\"").matches(this) -> this.replace("\"", "") to jsStringClassId
        else -> {
            if (contains('.')) {
                (toDoubleOrNull() ?: toBigDecimal()) to jsNumberClassId
            } else {
                val value = toByteOrNull() ?: toShortOrNull() ?: toIntOrNull() ?: toLongOrNull()
                ?: toBigIntegerOrNull() ?: toDoubleOrNull()
                if (value != null) value to jsNumberClassId else {
                    val obj = makeObject(this)
                    if (obj != null) obj to returnType else throw IllegalStateException()
                }
            }
        }
    }
}

private fun makeObject(objString: String): Map<String, Any>? {
    return try {
        val trimmed = objString.substringAfter(" ")
        val json = JSONObject(trimmed)
        val resMap = mutableMapOf<String, Any>()
        json.keySet().forEach {
            resMap[it] = json.get(it).toString().toJsAny(jsUndefinedClassId).first as Any
        }
        resMap
    } catch (e: JSONException) {
        null
    }
}