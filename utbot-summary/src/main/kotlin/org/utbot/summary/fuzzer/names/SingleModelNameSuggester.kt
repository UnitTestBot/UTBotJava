package org.utbot.summary.fuzzer.names

import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.jcdb.api.ifArrayGetElementClass
import org.utbot.jcdb.api.isPrimitive

interface SingleModelNameSuggester {
    fun suggest(description: FuzzedMethodDescription, value: FuzzedValue): String?
}

object PrimitiveModelNameSuggester : SingleModelNameSuggester {
    override fun suggest(description: FuzzedMethodDescription, value: FuzzedValue): String? {
        val model = value.model
        if (model is UtPrimitiveModel) {
            val v = model.value
            return when {
                v == "" -> "EmptyString"
                v is String && v.isBlank() -> "BlankString"
                v is String && v.isNotEmpty() -> "NonEmptyString"
                v is Char && (v == Char.MIN_VALUE || v == Char.MAX_VALUE) -> "CornerCase"
                v is Byte && (v == Byte.MIN_VALUE || v == Byte.MAX_VALUE) -> "CornerCase"
                v is Short && (v == Short.MIN_VALUE || v == Short.MAX_VALUE) -> "CornerCase"
                v is Int && (v == Int.MIN_VALUE || v == Int.MAX_VALUE) -> "CornerCase"
                v is Long && (v == Long.MIN_VALUE || v == Long.MAX_VALUE) -> "CornerCase"
                v is Float && v.isInfinite()  || v is Double && v.isInfinite() -> "CornerCase"
                v is Float && v.isNaN() || v is Double && v.isNaN() -> "CornerCase"
                v is Number && v.toDouble() == 0.0 -> "CornerCase"
                else -> null
            }
        }
        return null
    }
}

object ArrayModelNameSuggester : SingleModelNameSuggester {
    override fun suggest(description: FuzzedMethodDescription, value: FuzzedValue): String? {
        val model = value.model
        if (model is UtArrayModel) {
            return buildString {
                if (model.length > 0) {
                    append("Non")
                }
                append("Empty")
                append(if (model.classId.ifArrayGetElementClass()?.isPrimitive == true ) "Primitive" else "Object")
                append("Array")
            }
        }
        return null
    }
}