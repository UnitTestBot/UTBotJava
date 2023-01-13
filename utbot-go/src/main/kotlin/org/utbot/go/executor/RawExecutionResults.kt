package org.utbot.go.executor

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import org.utbot.go.api.GoArrayTypeId
import org.utbot.go.api.GoInterfaceTypeId
import org.utbot.go.api.GoStructTypeId
import org.utbot.go.api.GoPrimitiveTypeId
import org.utbot.go.api.util.goByteTypeId
import org.utbot.go.api.util.goPrimitives
import org.utbot.go.api.util.goRuneTypeId
import org.utbot.go.api.util.isPrimitiveGoType
import org.utbot.go.framework.api.go.GoTypeId
import kotlin.reflect.KClass

data class PrimitiveValue(
    override val type: String,
    override val value: String,
) : RawResultValue(type, value) {
    override fun checkIsEqualTypes(type: GoTypeId): Boolean {
        if (!type.isPrimitiveGoType && type !is GoInterfaceTypeId && !type.implementsError) {
            return false
        }
        if (this.type == "uint8" && type == goByteTypeId || this.type == "int32" && type == goRuneTypeId) {
            return true
        }
        if (this.type == "string" && type is GoInterfaceTypeId && type.implementsError) {
            return true
        }
        return this.type == type.simpleName
    }
}

data class StructValue(
    override val type: String,
    override val value: List<FieldValue>
) : RawResultValue(type, value) {
    data class FieldValue(
        val name: String,
        val value: RawResultValue,
        val isExported: Boolean
    )

    override fun checkIsEqualTypes(type: GoTypeId): Boolean {
        if (type !is GoStructTypeId) {
            return false
        }
        if (this.type != type.canonicalName) {
            return false
        }
        if (value.size != type.fields.size) {
            return false
        }
        value.zip(type.fields).forEach { (fieldValue, fieldId) ->
            if (fieldValue.name != fieldId.name) {
                return false
            }
            if (!fieldValue.value.checkIsEqualTypes(fieldId.declaringClass as GoTypeId)) {
                return false
            }
            if (fieldValue.isExported != fieldId.isExported) {
                return false
            }
        }
        return true
    }
}

data class ArrayValue(
    override val type: String,
    val elementType: String,
    val length: Int,
    override val value: List<RawResultValue>
) : RawResultValue(type, value) {
    override fun checkIsEqualTypes(type: GoTypeId): Boolean {
        if (type !is GoArrayTypeId) {
            return false
        }
        if (length != type.length || elementType != type.elementTypeId.canonicalName) {
            return false
        }
        value.forEach { arrayElementValue ->
            if (!arrayElementValue.checkIsEqualTypes(type.elementTypeId)) {
                return false
            }
        }
        return true
    }
}

@TypeFor(field = "type", adapter = RawResultValueAdapter::class)
abstract class RawResultValue(open val type: String, open val value: Any) {
    abstract fun checkIsEqualTypes(type: GoTypeId): Boolean
}

class RawResultValueAdapter : TypeAdapter<RawResultValue> {
    override fun classFor(type: Any): KClass<out RawResultValue> {
        val nameOfType = type as String
        return when {
            nameOfType.startsWith("map[") -> error("Map result type not supported")
            nameOfType.startsWith("[]") -> error("Slice result type not supported")
            nameOfType.startsWith("[") -> ArrayValue::class
            goPrimitives.map { it.name }.contains(nameOfType) -> PrimitiveValue::class
            else -> StructValue::class
        }
    }
}

internal data class RawPanicMessage(
    val rawResultValue: RawResultValue,
    val implementsError: Boolean
)

internal data class RawExecutionResult(
    val functionName: String,
    val timeoutExceeded: Boolean,
    val resultRawValues: List<RawResultValue?>,
    val panicMessage: RawPanicMessage?,
    val trace: List<Int>
)

internal data class RawExecutionResults(val results: List<RawExecutionResult>)