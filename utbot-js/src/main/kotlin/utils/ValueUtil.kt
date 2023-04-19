package utils

import framework.api.js.JsClassId
import framework.api.js.util.isJsStdStructure
import framework.api.js.util.jsBooleanClassId
import framework.api.js.util.jsDoubleClassId
import framework.api.js.util.jsErrorClassId
import framework.api.js.util.jsNumberClassId
import framework.api.js.util.jsStringClassId
import framework.api.js.util.jsUndefinedClassId
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import utils.data.ResultData

fun ResultData.toJsAny(returnType: JsClassId = jsUndefinedClassId): Pair<Any?, JsClassId> {
    this.buildUniqueValue()?.let { return it }
    with(this.rawString) {
        return when {
            isError -> this to jsErrorClassId
            this == "true" || this == "false" -> toBoolean() to jsBooleanClassId
            this == "null" || this == "undefined" -> null to jsUndefinedClassId
            returnType.isJsStdStructure ->
                makeStructure(this, returnType) to returnType
            returnType == jsStringClassId || this@toJsAny.type == jsStringClassId.name ->
                this.replace("\"", "") to jsStringClassId
            else -> {
                if (contains('.')) {
                    (toDoubleOrNull() ?: toBigDecimal()) to jsDoubleClassId
                } else {
                    val value = toByteOrNull() ?: toShortOrNull() ?: toIntOrNull() ?: toLongOrNull()
                    ?: toBigIntegerOrNull() ?: toDoubleOrNull()
                    if (value != null) value to jsNumberClassId else {
                        val obj = makeObject(this)
                        if (obj != null) obj to returnType else {
                            throw IllegalStateException("Could not make js value from $this value with type ${this@toJsAny.type}")
                        }
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
            resMap[it] = ResultData(json.get(it).toString(), index = 0).toJsAny().first as Any
        }
        resMap
    } catch (e: JSONException) {
        null
    }
}

private fun makeStructure(structString: String, type: JsClassId): List<Any?> {
    val json = JSONArray(structString)
    return when (type.name) {
        "Array", "Set" -> {
            json.map { jsonObj ->
                ResultData(jsonObj as JSONObject).toJsAny().first
            }
        }
        "Map" -> {
            json.map { jsonObj ->
                val name = (jsonObj as JSONObject).get("name")
                name to ResultData(jsonObj.getJSONObject("json")).toJsAny().first
            }
        }
        else -> throw UnsupportedOperationException(
            "Can't make JavaScript structure from $structString with type ${type.name}"
        )
    }
}
