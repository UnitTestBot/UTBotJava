package org.utbot.go.worker

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
    val value: String,
) : RawValue(type) {
    override fun checkIsEqualTypes(type: GoTypeId): Boolean {
        if (type is GoNamedTypeId) {
            return checkIsEqualTypes(type.underlyingTypeId)
        }
        if (!type.isPrimitiveGoType && type !is GoInterfaceTypeId) {
            return false
        }
        // for error support
        if (this.type == "string" && type is GoInterfaceTypeId) {
            return true
        }
        return GoPrimitiveTypeId(this.type) == type
    }
}

data class NamedValue(
    override val type: String,
    val value: RawValue,
) : RawValue(type) {
    override fun checkIsEqualTypes(type: GoTypeId): Boolean = error("Not supported")
}

data class StructValue(
    override val type: String,
    val value: List<FieldValue>
) : RawValue(type) {
    data class FieldValue(
        val name: String,
        val value: RawValue,
        val isExported: Boolean
    )

    override fun checkIsEqualTypes(type: GoTypeId): Boolean {
        if (type is GoNamedTypeId) {
            return this.type == type.canonicalName && checkIsEqualTypes(type.underlyingTypeId)
        }
        if (type !is GoStructTypeId) {
            return false
        }
        if (value.size != type.fields.size) {
            return false
        }
        value.zip(type.fields).forEach { (fieldValue, fieldId) ->
            if (fieldValue.name != fieldId.name) {
                return false
            }
            if (!fieldValue.value.checkIsEqualTypes(fieldId.declaringType)) {
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
    val value: List<RawValue>
) : RawValue(type) {
    override fun checkIsEqualTypes(type: GoTypeId): Boolean {
        if (type is GoNamedTypeId) {
            return checkIsEqualTypes(type.underlyingTypeId)
        }
        if (type !is GoArrayTypeId) {
            return false
        }
        if (length != type.length || elementType != type.elementTypeId!!.canonicalName) {
            return false
        }
        return value.all { it.checkIsEqualTypes(type.elementTypeId) }
    }
}

data class SliceValue(
    override val type: String,
    val elementType: String,
    val length: Int,
    val value: List<RawValue>
) : RawValue(type) {
    override fun checkIsEqualTypes(type: GoTypeId): Boolean {
        if (type is GoNamedTypeId) {
            return checkIsEqualTypes(type.underlyingTypeId)
        }
        if (type !is GoSliceTypeId) {
            return false
        }
        if (elementType != type.elementTypeId!!.canonicalName) {
            return false
        }
        return value.all { it.checkIsEqualTypes(type.elementTypeId) }
    }
}

data class NilValue(
    override val type: String
) : RawValue(type) {
    override fun checkIsEqualTypes(type: GoTypeId): Boolean = error("Not supported")
}

@TypeFor(field = "type", adapter = RawResultValueAdapter::class)
abstract class RawValue(open val type: String) {
    abstract fun checkIsEqualTypes(type: GoTypeId): Boolean
}

class RawResultValueAdapter : TypeAdapter<RawValue> {
    override fun classFor(type: Any): KClass<out RawValue> {
        val typeName = type as String
        return when {
            typeName.startsWith("map[") -> error("Map result type not supported")
            typeName.startsWith("[]") -> SliceValue::class
            typeName.startsWith("[") -> ArrayValue::class
            goPrimitives.map { it.name }.contains(typeName) -> PrimitiveValue::class
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

class GoWorkerFailedException(s: String) : Exception(s)

fun convertRawExecutionResultToExecutionResult(
    rawExecutionResult: RawExecutionResult,
    functionResultTypes: List<GoTypeId>,
    intSize: Int,
    timeoutMillis: Long
): GoUtExecutionResult {
    if (rawExecutionResult.timeoutExceeded) {
        return GoUtTimeoutExceeded(timeoutMillis, rawExecutionResult.trace)
    }
    if (rawExecutionResult.panicMessage != null) {
        val (rawResultValue, implementsError) = rawExecutionResult.panicMessage
        val panicValue = if (goPrimitives.map { it.simpleName }.contains(rawResultValue.type)) {
            createGoUtPrimitiveModelFromRawValue(
                rawResultValue as PrimitiveValue,
                GoPrimitiveTypeId(rawResultValue.type),
                intSize
            )
        } else {
            error("Only primitive panic value is currently supported")
        }
        return GoUtPanicFailure(panicValue, implementsError, rawExecutionResult.trace)
    }
    if (rawExecutionResult.rawResultValues.size != functionResultTypes.size) {
        error("Function completed execution must have as many result raw values as result types.")
    }
    rawExecutionResult.rawResultValues.zip(functionResultTypes).forEach { (rawResultValue, resultType) ->
        if (rawResultValue != null && !rawResultValue.checkIsEqualTypes(resultType)) {
            error("Result of function execution must have same type as function result")
        }
    }
    var executedWithNonNilErrorString = false
    val resultValues =
        rawExecutionResult.rawResultValues.zip(functionResultTypes).map { (rawResultValue, resultType) ->
            if (resultType.implementsError && rawResultValue != null) {
                executedWithNonNilErrorString = true
            }
            createGoUtModelFromRawValue(rawResultValue, resultType, intSize)
        }
    return if (executedWithNonNilErrorString) {
        GoUtExecutionWithNonNilError(resultValues, rawExecutionResult.trace)
    } else {
        GoUtExecutionSuccess(resultValues, rawExecutionResult.trace)
    }
}

private fun createGoUtModelFromRawValue(
    rawValue: RawValue?, typeId: GoTypeId, intSize: Int
): GoUtModel = when (typeId) {
    is GoNamedTypeId -> GoUtNamedModel(createGoUtModelFromRawValue(rawValue, typeId.underlyingTypeId, intSize), typeId)

    // Only for error interface
    is GoInterfaceTypeId -> if (rawValue == null) {
        GoUtNilModel(typeId)
    } else {
        GoUtPrimitiveModel((rawValue as PrimitiveValue).value, goStringTypeId)
    }

    is GoStructTypeId -> createGoUtStructModelFromRawValue(rawValue as StructValue, typeId, intSize)

    is GoArrayTypeId -> createGoUtArrayModelFromRawValue(rawValue as ArrayValue, typeId, intSize)

    is GoSliceTypeId -> createGoUtSliceModelFromRawValue(rawValue as SliceValue, typeId, intSize)

    is GoPrimitiveTypeId -> createGoUtPrimitiveModelFromRawValue(rawValue as PrimitiveValue, typeId, intSize)

    else -> error("Creating a model from raw value of [${typeId.javaClass}] type is not supported")
}

private fun createGoUtPrimitiveModelFromRawValue(
    resultValue: PrimitiveValue, typeId: GoPrimitiveTypeId, intSize: Int
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
    val value = rawValueOfGoPrimitiveTypeToValue(typeId, rawValue, intSize)
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
    resultValue: StructValue, resultTypeId: GoStructTypeId, intSize: Int
): GoUtStructModel {
    val value = resultValue.value.zip(resultTypeId.fields).map { (value, fieldId) ->
        GoUtFieldModel(createGoUtModelFromRawValue(value.value, fieldId.declaringType, intSize), fieldId)
    }
    return GoUtStructModel(value, resultTypeId)
}

private fun createGoUtArrayModelFromRawValue(
    resultValue: ArrayValue, resultTypeId: GoArrayTypeId, intSize: Int
): GoUtArrayModel {
    val value = (0 until resultTypeId.length).associateWith { index ->
        createGoUtModelFromRawValue(resultValue.value[index], resultTypeId.elementTypeId!!, intSize)
    }.toMutableMap()
    return GoUtArrayModel(value, resultTypeId)
}

private fun createGoUtSliceModelFromRawValue(
    resultValue: SliceValue, resultTypeId: GoSliceTypeId, intSize: Int
): GoUtSliceModel {
    val value = (0 until resultValue.length).associateWith { index ->
        createGoUtModelFromRawValue(resultValue.value[index], resultTypeId.elementTypeId!!, intSize)
    }.toMutableMap()
    return GoUtSliceModel(value, resultTypeId, resultValue.length)
}