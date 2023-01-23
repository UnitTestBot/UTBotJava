package org.utbot.go.executor

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import org.utbot.go.api.*
import org.utbot.go.api.util.*
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtFieldModel
import org.utbot.go.framework.api.go.GoUtModel
import kotlin.reflect.KClass

data class PrimitiveValue(
    override val type: String,
    override val value: String,
) : RawValue(type, value) {
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
) : RawValue(type, value) {
    data class FieldValue(
        val name: String,
        val value: RawValue,
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
    override val value: List<RawValue>
) : RawValue(type, value) {
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
abstract class RawValue(open val type: String, open val value: Any) {
    abstract fun checkIsEqualTypes(type: GoTypeId): Boolean
}

class RawResultValueAdapter : TypeAdapter<RawValue> {
    override fun classFor(type: Any): KClass<out RawValue> {
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

data class RawPanicMessage(
    val rawResultValue: RawValue,
    val implementsError: Boolean
)

data class RawExecutionResult(
    val timeoutExceeded: Boolean,
    val rawResultValues: List<RawValue?>,
    val panicMessage: RawPanicMessage?,
    val trace: List<Int>
)

private object RawValuesCodes {
    const val NAN_VALUE = "NaN"
    const val POS_INF_VALUE = "+Inf"
    const val NEG_INF_VALUE = "-Inf"
    const val COMPLEX_PARTS_DELIMITER = "@"
}

fun convertRawExecutionResultToExecutionResult(
    packageName: String,
    rawExecutionResult: RawExecutionResult,
    functionResultTypes: List<GoTypeId>,
    timeoutMillis: Long
): GoUtExecutionResult {
    if (rawExecutionResult.timeoutExceeded) {
        return GoUtTimeoutExceeded(timeoutMillis, rawExecutionResult.trace)
    }
    if (rawExecutionResult.panicMessage != null) {
        val (rawResultValue, _) = rawExecutionResult.panicMessage
        val panicValue = if (goPrimitives.map { it.simpleName }.contains(rawResultValue.type)) {
            createGoUtPrimitiveModelFromRawValue(
                rawResultValue as PrimitiveValue,
                GoPrimitiveTypeId(rawResultValue.type)
            )
        } else {
            error("Only primitive panic value is currently supported")
        }
        return GoUtPanicFailure(panicValue, GoPrimitiveTypeId(rawResultValue.type), rawExecutionResult.trace)
    }
    if (rawExecutionResult.rawResultValues.size != functionResultTypes.size) {
        error("Function completed execution must have as many result raw values as result types.")
    }
    rawExecutionResult.rawResultValues.zip(functionResultTypes).forEach { (rawResultValue, resultType) ->
        if (rawResultValue == null) {
            if (resultType !is GoInterfaceTypeId) {
                error("Result of function execution must have same type as function result")
            }
            return@forEach
        }
        if (!rawResultValue.checkIsEqualTypes(resultType)) {
            error("Result of function execution must have same type as function result")
        }
    }
    var executedWithNonNilErrorString = false
    val resultValues =
        rawExecutionResult.rawResultValues.zip(functionResultTypes).map { (rawResultValue, resultType) ->
            if (resultType.implementsError && rawResultValue != null) {
                executedWithNonNilErrorString = true
            }
            createGoUtModelFromRawValue(rawResultValue, resultType, packageName)
        }
    return if (executedWithNonNilErrorString) {
        GoUtExecutionWithNonNilError(resultValues, rawExecutionResult.trace)
    } else {
        GoUtExecutionSuccess(resultValues, rawExecutionResult.trace)
    }
}

private fun createGoUtModelFromRawValue(
    rawValue: RawValue?, typeId: GoTypeId, packageName: String
): GoUtModel = when (typeId) {
    // Only for error interface
    is GoInterfaceTypeId -> if (rawValue == null) {
        GoUtNilModel(typeId)
    } else {
        GoUtPrimitiveModel((rawValue as PrimitiveValue).value, goStringTypeId)
    }

    is GoStructTypeId -> createGoUtStructModelFromRawValue(rawValue as StructValue, typeId, packageName)

    is GoArrayTypeId -> createGoUtArrayModelFromRawValue(rawValue as ArrayValue, typeId, packageName)

    is GoPrimitiveTypeId -> createGoUtPrimitiveModelFromRawValue(rawValue as PrimitiveValue, typeId)

    else -> error("Creating a model from raw value of [${typeId.javaClass}] type is not supported")
}

private fun createGoUtPrimitiveModelFromRawValue(
    resultValue: PrimitiveValue, typeId: GoPrimitiveTypeId
): GoUtPrimitiveModel {
    val rawValue = resultValue.value
    if (typeId == goFloat64TypeId || typeId == goFloat32TypeId) {
        return convertRawFloatValueToGoUtPrimitiveModel(rawValue, typeId)
    }
    if (typeId == goComplex128TypeId || typeId == goComplex64TypeId) {
        val correspondingFloatType = if (typeId == goComplex128TypeId) goFloat64TypeId else goFloat32TypeId
        val (realPartModel, imagPartModel) = rawValue.split(RawValuesCodes.COMPLEX_PARTS_DELIMITER).map {
            convertRawFloatValueToGoUtPrimitiveModel(it, correspondingFloatType, typeId == goComplex64TypeId)
        }
        return GoUtComplexModel(realPartModel, imagPartModel, typeId)
    }
    val value = when (typeId.correspondingKClass) {
        UByte::class -> rawValue.toUByte()
        Boolean::class -> rawValue.toBoolean()
        Float::class -> rawValue.toFloat()
        Double::class -> rawValue.toDouble()
        Int::class -> rawValue.toInt()
        Short::class -> rawValue.toShort()
        Long::class -> rawValue.toLong()
        Byte::class -> rawValue.toByte()
        UInt::class -> rawValue.toUInt()
        UShort::class -> rawValue.toUShort()
        ULong::class -> rawValue.toULong()
        else -> rawValue
    }
    return GoUtPrimitiveModel(value, typeId)
}

private fun convertRawFloatValueToGoUtPrimitiveModel(
    rawValue: String, typeId: GoPrimitiveTypeId, explicitCastRequired: Boolean = false
): GoUtPrimitiveModel {
    return when (rawValue) {
        RawValuesCodes.NAN_VALUE -> GoUtFloatNaNModel(typeId)
        RawValuesCodes.POS_INF_VALUE -> GoUtFloatInfModel(1, typeId)
        RawValuesCodes.NEG_INF_VALUE -> GoUtFloatInfModel(-1, typeId)
        else -> {
            val typedValue = if (typeId == goFloat64TypeId) rawValue.toDouble() else rawValue.toFloat()
            if (explicitCastRequired) {
                GoUtPrimitiveModel(typedValue, typeId, explicitCastMode = ExplicitCastMode.REQUIRED)
            } else {
                GoUtPrimitiveModel(typedValue, typeId)
            }
        }
    }
}

private fun createGoUtStructModelFromRawValue(
    resultValue: StructValue, resultTypeId: GoStructTypeId, packageName: String
): GoUtStructModel {
    val value = resultValue.value.zip(resultTypeId.fields).map { (value, fieldId) ->
        GoUtFieldModel(
            createGoUtModelFromRawValue(
                value.value,
                fieldId.declaringClass as GoPrimitiveTypeId,
                packageName
            ), fieldId
        )
    }
    return GoUtStructModel(value, resultTypeId, packageName)
}

private fun createGoUtArrayModelFromRawValue(
    resultValue: ArrayValue, resultTypeId: GoArrayTypeId, packageName: String
): GoUtArrayModel {
    val value = (0 until resultTypeId.length).associateWith { index ->
        createGoUtModelFromRawValue(resultValue.value[index], resultTypeId.elementTypeId, packageName)
    }.toMutableMap()
    return GoUtArrayModel(value, resultTypeId, packageName)
}