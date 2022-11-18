package utils

import framework.api.ts.TsClassId
import framework.api.ts.util.tsBooleanClassId
import framework.api.ts.util.tsErrorClassId
import framework.api.ts.util.tsNumberClassId
import framework.api.ts.util.tsStringClassId
import framework.api.ts.util.tsUndefinedClassId
import org.json.JSONException
import org.json.JSONObject

fun String.toTsAny(returnType: TsClassId): Pair<Any?, TsClassId> {
    return when {
        this == "true" || this == "false" -> toBoolean() to tsBooleanClassId
        this == "null" || this == "undefined" -> null to tsUndefinedClassId
        Regex("^.*Error:.*").matches(this) -> this.replace("Error:", "") to tsErrorClassId
        returnType == tsStringClassId -> this to tsStringClassId
        else -> {
            if (contains('.')) {
                (toDoubleOrNull() ?: toBigDecimal()) to tsNumberClassId
            } else {
                val value = toByteOrNull() ?: toShortOrNull() ?: toIntOrNull() ?: toLongOrNull()
                ?: toBigIntegerOrNull() ?: toDoubleOrNull()
                if (value != null) value to tsNumberClassId else {
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
            resMap[it] = json.get(it).toString().toTsAny(tsUndefinedClassId).first as Any
        }
        resMap
    } catch (e: JSONException) {
        null
    }
}