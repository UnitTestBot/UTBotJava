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
) : RawValue(type)

data class NamedValue(
    override val type: String,
    val value: RawValue,
) : RawValue(type)

data class StructValue(
    override val type: String,
    val value: List<FieldValue>
) : RawValue(type) {
    data class FieldValue(
        val name: String,
        val value: RawValue,
        val isExported: Boolean
    )
}

data class ArrayValue(
    override val type: String,
    val elementType: String,
    val length: Int,
    val value: List<RawValue>
) : RawValue(type)

data class SliceValue(
    override val type: String,
    val elementType: String,
    val length: Int,
    val value: List<RawValue>
) : RawValue(type)

data class MapValue(
    override val type: String,
    val keyType: String,
    val elementType: String,
    val value: List<KeyValue>
) : RawValue(type) {
    data class KeyValue(
        val key: RawValue,
        val value: RawValue
    )
}

data class ChanValue(
    override val type: String,
    val elementType: String,
    val direction: String,
    val length: Int,
    val value: List<RawValue>
) : RawValue(type)

data class NilValue(override val type: String) : RawValue(type)

data class InterfaceValue(override val type: String) : RawValue(type)

data class PointerValue(override val type: String, val elementType: String, val value: RawValue) : RawValue(type)

@TypeFor(field = "type", adapter = RawValueAdapter::class)
abstract class RawValue(open val type: String)

class RawValueAdapter : TypeAdapter<RawValue> {
    override fun classFor(type: Any): KClass<out RawValue> {
        val typeName = type as String
        return when {
            typeName == "nil" -> NilValue::class
            typeName == "interface{}" -> InterfaceValue::class
            typeName == "struct{}" -> StructValue::class
            typeName.startsWith("map[") -> MapValue::class
            typeName.startsWith("[]") -> SliceValue::class
            typeName.startsWith("[") -> ArrayValue::class
            typeName.startsWith("<-chan") || typeName.startsWith("chan") -> ChanValue::class
            typeName.startsWith("*") -> PointerValue::class
            goPrimitives.map { it.name }.contains(typeName) -> PrimitiveValue::class
            else -> NamedValue::class
        }
    }
}

data class RawPanicMessage(
    val rawResultValue: RawValue, val implementsError: Boolean
)

data class RawExecutionResult(
    val timeoutExceeded: Boolean,
    val rawResultValues: List<RawValue>,
    val panicMessage: RawPanicMessage?,
    val coverTab: Map<String, Int>
)

private object RawValuesCodes {
    const val NAN_VALUE = "NaN"
    const val POS_INF_VALUE = "+Inf"
    const val NEG_INF_VALUE = "-Inf"
    const val COMPLEX_PARTS_DELIMITER = "@"
}

class GoWorkerFailedException(s: String) : Exception(s)

fun convertRawExecutionResultToExecutionResult(
    rawExecutionResult: RawExecutionResult, functionResultTypes: List<GoTypeId>, intSize: Int, timeoutMillis: Long
): GoUtExecutionResult {
    if (rawExecutionResult.timeoutExceeded) {
        return GoUtTimeoutExceeded(timeoutMillis)
    }
    if (rawExecutionResult.panicMessage != null) {
        val (rawResultValue, implementsError) = rawExecutionResult.panicMessage
        val panicValue = if (goPrimitives.map { it.simpleName }.contains(rawResultValue.type)) {
            createGoUtPrimitiveModelFromRawValue(
                rawResultValue as PrimitiveValue, GoPrimitiveTypeId(rawResultValue.type), intSize
            )
        } else {
            error("Only primitive panic value is currently supported")
        }
        return GoUtPanicFailure(panicValue, implementsError)
    }
    if (rawExecutionResult.rawResultValues.size != functionResultTypes.size) {
        error("Function completed execution must have as many result raw values as result types.")
    }
    var executedWithNonNilErrorString = false
    val resultValues = rawExecutionResult.rawResultValues.zip(functionResultTypes).map { (rawResultValue, resultType) ->
        val model = createGoUtModelFromRawValue(rawResultValue, resultType, intSize)
        if (resultType.implementsError && (model is GoUtNamedModel && model.value.typeId == goStringTypeId)) {
            executedWithNonNilErrorString = true
        }
        return@map model
    }
    return if (executedWithNonNilErrorString) {
        GoUtExecutionWithNonNilError(resultValues)
    } else {
        GoUtExecutionSuccess(resultValues)
    }
}

private fun createGoUtModelFromRawValue(
    rawValue: RawValue, typeId: GoTypeId, intSize: Int
): GoUtModel = if (rawValue is NilValue) {
    GoUtNilModel(typeId)
} else {
    when (typeId) {
        is GoNamedTypeId -> createGoUtNamedModelFromRawValue(rawValue as NamedValue, typeId, intSize)
        // Only for error interface
        is GoInterfaceTypeId -> GoUtPrimitiveModel((rawValue as PrimitiveValue).value, goStringTypeId)

        is GoStructTypeId -> createGoUtStructModelFromRawValue(rawValue as StructValue, typeId, intSize)

        is GoArrayTypeId -> createGoUtArrayModelFromRawValue(rawValue as ArrayValue, typeId, intSize)

        is GoSliceTypeId -> createGoUtSliceModelFromRawValue(rawValue as SliceValue, typeId, intSize)

        is GoMapTypeId -> createGoUtMapModelFromRawValue(rawValue as MapValue, typeId, intSize)

        is GoPrimitiveTypeId -> createGoUtPrimitiveModelFromRawValue(rawValue as PrimitiveValue, typeId, intSize)

        is GoPointerTypeId -> createGoUtPointerModelFromRawValue(rawValue as PointerValue, typeId, intSize)

        is GoChanTypeId -> createGoUtChanModelFromRawValue(rawValue as ChanValue, typeId, intSize)

        else -> error("Creating a model from raw value of [${typeId.javaClass}] type is not supported")
    }
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
    val value = resultValue.value.map {
        createGoUtModelFromRawValue(it, resultTypeId.elementTypeId!!, intSize)
    }.toTypedArray<GoUtModel?>()
    return GoUtArrayModel(value, resultTypeId)
}

private fun createGoUtSliceModelFromRawValue(
    resultValue: SliceValue, resultTypeId: GoSliceTypeId, intSize: Int
): GoUtSliceModel {
    val value = resultValue.value.map {
        createGoUtModelFromRawValue(it, resultTypeId.elementTypeId!!, intSize)
    }.toTypedArray<GoUtModel?>()
    return GoUtSliceModel(value, resultTypeId, resultValue.length)
}

private fun createGoUtMapModelFromRawValue(
    resultValue: MapValue, resultTypeId: GoMapTypeId, intSize: Int
): GoUtMapModel {
    val value = resultValue.value.associate {
        val key = createGoUtModelFromRawValue(it.key, resultTypeId.keyTypeId, intSize)
        val value = createGoUtModelFromRawValue(it.value, resultTypeId.elementTypeId!!, intSize)
        key to value
    }.toMutableMap()
    return GoUtMapModel(value, resultTypeId)
}

private fun createGoUtNamedModelFromRawValue(
    resultValue: NamedValue, resultTypeId: GoNamedTypeId, intSize: Int
): GoUtNamedModel {
    val value = createGoUtModelFromRawValue(resultValue.value, resultTypeId.underlyingTypeId, intSize)
    return GoUtNamedModel(value, resultTypeId)
}

private fun createGoUtPointerModelFromRawValue(
    resultValue: PointerValue, resultTypeId: GoPointerTypeId, intSize: Int
): GoUtPointerModel {
    val value = createGoUtModelFromRawValue(resultValue.value, resultTypeId.elementTypeId!!, intSize)
    return GoUtPointerModel(value, resultTypeId)
}

private fun createGoUtChanModelFromRawValue(
    resultValue: ChanValue, resultTypeId: GoChanTypeId, intSize: Int
): GoUtChanModel {
    val value = resultValue.value.map {
        createGoUtModelFromRawValue(it, resultTypeId.elementTypeId!!, intSize)
    }.toTypedArray<GoUtModel?>()
    return GoUtChanModel(value, resultTypeId)
}