package utils

import framework.api.js.JsClassId
import framework.api.js.util.jsBooleanClassId
import framework.api.js.util.jsDoubleClassId
import framework.api.js.util.jsErrorClassId
import framework.api.js.util.jsNumberClassId
import framework.api.js.util.jsStringClassId
import framework.api.js.util.jsUndefinedClassId
import org.json.JSONException
import org.json.JSONObject

fun ResultData.toJsAny(returnType: JsClassId = jsUndefinedClassId): Pair<Any?, JsClassId> {
    this.buildUniqueValue()?.let { return it }
    with(this.rawString) {
        return when {
            this == "true" || this == "false" -> toBoolean() to jsBooleanClassId
            this == "null" || this == "undefined" -> null to jsUndefinedClassId
            this@toJsAny.isError -> this to jsErrorClassId
            returnType == jsStringClassId -> this.replace("\"", "") to jsStringClassId
            else -> {
                if (contains('.')) {
                    (toDoubleOrNull() ?: toBigDecimal()) to jsDoubleClassId
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
}

private fun ResultData.buildUniqueValue(): Pair<Any?, JsClassId>? {
    return when {
        isInf -> specSign * Double.POSITIVE_INFINITY to jsDoubleClassId
        isNan -> Double.NaN to jsDoubleClassId
        else -> null
    }
}

private fun makeObject(objString: String): Map<String, Any>? {
    return try {
        val trimmed = objString.substringAfter(" ")
        val json = JSONObject(trimmed)
        val resMap = mutableMapOf<String, Any>()
        json.keySet().forEach {
            resMap[it] = ResultData(json.get(it).toString(), index = 0).toJsAny(jsUndefinedClassId).first as Any
        }
        resMap
    } catch (e: JSONException) {
        null
    }
}
