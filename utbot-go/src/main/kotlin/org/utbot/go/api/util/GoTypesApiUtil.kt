package org.utbot.go.api.util

import org.utbot.go.api.*
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel
import kotlin.reflect.KClass

val goByteTypeId = GoPrimitiveTypeId("byte")
val goBoolTypeId = GoPrimitiveTypeId("bool")

val goComplex128TypeId = GoPrimitiveTypeId("complex128")
val goComplex64TypeId = GoPrimitiveTypeId("complex64")

val goFloat32TypeId = GoPrimitiveTypeId("float32")
val goFloat64TypeId = GoPrimitiveTypeId("float64")

val goInt8TypeId = GoPrimitiveTypeId("int8")
val goInt16TypeId = GoPrimitiveTypeId("int16")
val goInt32TypeId = GoPrimitiveTypeId("int32")
val goIntTypeId = GoPrimitiveTypeId("int")
val goInt64TypeId = GoPrimitiveTypeId("int64")

val goRuneTypeId = GoPrimitiveTypeId("rune") // = int32
val goStringTypeId = GoPrimitiveTypeId("string")

val goUint8TypeId = GoPrimitiveTypeId("uint8")
val goUint16TypeId = GoPrimitiveTypeId("uint16")
val goUint32TypeId = GoPrimitiveTypeId("uint32")
val goUintTypeId = GoPrimitiveTypeId("uint")
val goUint64TypeId = GoPrimitiveTypeId("uint64")
val goUintPtrTypeId = GoPrimitiveTypeId("uintptr")

val goPrimitives = setOf(
    goByteTypeId,
    goBoolTypeId,
    goComplex128TypeId,
    goComplex64TypeId,
    goFloat32TypeId,
    goFloat64TypeId,
    goIntTypeId,
    goInt16TypeId,
    goInt32TypeId,
    goInt64TypeId,
    goInt8TypeId,
    goRuneTypeId,
    goStringTypeId,
    goUintTypeId,
    goUint16TypeId,
    goUint32TypeId,
    goUint64TypeId,
    goUint8TypeId,
    goUintPtrTypeId,
)

val goSupportedConstantTypes = setOf(
    goByteTypeId,
    goFloat32TypeId,
    goFloat64TypeId,
    goIntTypeId,
    goInt8TypeId,
    goInt16TypeId,
    goInt32TypeId,
    goInt64TypeId,
    goRuneTypeId,
    goStringTypeId,
    goUintTypeId,
    goUint8TypeId,
    goUint16TypeId,
    goUint32TypeId,
    goUint64TypeId,
    goUintPtrTypeId,
)

val GoTypeId.isPrimitiveGoType: Boolean
    get() = this in goPrimitives

private val goTypesNeverRequireExplicitCast = setOf(
    goBoolTypeId,
    goComplex128TypeId,
    goComplex64TypeId,
    goFloat64TypeId,
    goIntTypeId,
    goStringTypeId,
)

val GoPrimitiveTypeId.neverRequiresExplicitCast: Boolean
    get() = this in goTypesNeverRequireExplicitCast

/**
 * This method is useful for converting the string representation of a Go value to its more accurate representation.
 */
private fun GoPrimitiveTypeId.correspondingKClass(intSize: Int): KClass<out Any> = when (this) {
    goBoolTypeId -> Boolean::class
    goFloat32TypeId -> Float::class
    goFloat64TypeId -> Double::class
    goInt8TypeId -> Byte::class
    goInt16TypeId -> Short::class
    goInt32TypeId, goRuneTypeId -> Int::class
    goIntTypeId -> if (intSize == 32) Int::class else Long::class
    goInt64TypeId -> Long::class
    goStringTypeId -> String::class
    goUint8TypeId, goByteTypeId -> UByte::class
    goUint16TypeId -> UShort::class
    goUint32TypeId -> UInt::class
    goUintTypeId -> if (intSize == 32) UInt::class else ULong::class
    goUint64TypeId -> ULong::class
    goUintPtrTypeId -> if (intSize == 32) UInt::class else ULong::class
    else -> String::class // default way to hold GoUtPrimitiveModel's value is to use String
}

fun rawValueOfGoPrimitiveTypeToValue(typeId: GoPrimitiveTypeId, rawValue: String, intSize: Int): Any =
    when (typeId.correspondingKClass(intSize)) {
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

fun GoTypeId.goDefaultValueModel(): GoUtModel = when (this) {
    is GoPrimitiveTypeId -> when (this) {
        goBoolTypeId -> GoUtPrimitiveModel(false, this)
        goRuneTypeId, goIntTypeId, goInt8TypeId, goInt16TypeId, goInt32TypeId, goInt64TypeId -> GoUtPrimitiveModel(
            0,
            this
        )

        goByteTypeId, goUintTypeId, goUint8TypeId, goUint16TypeId, goUint32TypeId, goUint64TypeId -> GoUtPrimitiveModel(
            0,
            this
        )

        goFloat32TypeId, goFloat64TypeId -> GoUtPrimitiveModel(0.0, this)
        goComplex64TypeId, goComplex128TypeId -> GoUtComplexModel(
            goFloat64TypeId.goDefaultValueModel() as GoUtPrimitiveModel,
            goFloat64TypeId.goDefaultValueModel() as GoUtPrimitiveModel,
            this
        )

        goStringTypeId -> GoUtPrimitiveModel("", this)
        goUintPtrTypeId -> GoUtPrimitiveModel(0, this)

        else -> error("Go primitive ${this.javaClass} is not supported")
    }

    is GoStructTypeId -> GoUtStructModel(listOf(), this)
    is GoArrayTypeId -> GoUtArrayModel(hashMapOf(), this)
    is GoSliceTypeId -> GoUtSliceModel(hashMapOf(), this, 0)
    else -> GoUtNilModel(this)
}

fun GoTypeId.getAllStructTypes(): Set<GoStructTypeId> = when (this) {
    is GoStructTypeId -> fields.fold(setOf(this)) { acc: Set<GoStructTypeId>, field ->
        acc + (field.declaringType).getAllStructTypes()
    }

    is GoArrayTypeId, is GoSliceTypeId -> elementTypeId!!.getAllStructTypes()
    else -> emptySet()
}

fun List<GoTypeId>.getAllStructTypes(): Set<GoStructTypeId> = this.fold(emptySet()) { acc, type ->
    acc + type.getAllStructTypes()
}
