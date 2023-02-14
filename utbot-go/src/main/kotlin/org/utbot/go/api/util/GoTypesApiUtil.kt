package org.utbot.go.api.util

import org.utbot.go.api.*
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel
import kotlin.reflect.KClass

val goByteTypeId = GoPrimitiveTypeId("byte")
val goBoolTypeId = GoPrimitiveTypeId("bool")

val goComplex128TypeId = GoPrimitiveTypeId("complex128")
val goComplex64TypeId = GoPrimitiveTypeId("complex64")

val goFloat32TypeId = GoPrimitiveTypeId("float32")
val goFloat64TypeId = GoPrimitiveTypeId("float64")

val goIntTypeId = GoPrimitiveTypeId("int")
val goInt16TypeId = GoPrimitiveTypeId("int16")
val goInt32TypeId = GoPrimitiveTypeId("int32")
val goInt64TypeId = GoPrimitiveTypeId("int64")
val goInt8TypeId = GoPrimitiveTypeId("int8")

val goRuneTypeId = GoPrimitiveTypeId("rune") // = int32
val goStringTypeId = GoPrimitiveTypeId("string")

val goUintTypeId = GoPrimitiveTypeId("uint")
val goUint16TypeId = GoPrimitiveTypeId("uint16")
val goUint32TypeId = GoPrimitiveTypeId("uint32")
val goUint64TypeId = GoPrimitiveTypeId("uint64")
val goUint8TypeId = GoPrimitiveTypeId("uint8")
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
 * For example, to build more proper GoUtPrimitiveModel-s with GoFuzzedFunctionsExecutor.
 * Note, that for now such conversion is not required and is done for convenience only.
 *
 * About corresponding types: int and uint / uintptr types sizes in Go are platform dependent,
 * but are supposed to fit in Long and ULong respectively.
 */
val GoPrimitiveTypeId.correspondingKClass: KClass<out Any>
    get() = when (this) {
        goByteTypeId, goUint8TypeId -> UByte::class
        goBoolTypeId -> Boolean::class
        goFloat32TypeId -> Float::class
        goFloat64TypeId -> Double::class
        goInt16TypeId -> Short::class
        goInt32TypeId, goRuneTypeId -> Int::class
        goIntTypeId, goInt64TypeId -> Long::class
        goInt8TypeId -> Byte::class
        goStringTypeId -> String::class
        goUint32TypeId -> UInt::class
        goUint16TypeId -> UShort::class
        goUintTypeId, goUint64TypeId, goUintPtrTypeId -> ULong::class
        else -> String::class // default way to hold GoUtPrimitiveModel's value is to use String
    }

fun GoTypeId.goDefaultValueModel(destinationPackage: GoPackage): GoUtModel = when (this) {
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
            goFloat64TypeId.goDefaultValueModel(destinationPackage) as GoUtPrimitiveModel,
            goFloat64TypeId.goDefaultValueModel(destinationPackage) as GoUtPrimitiveModel,
            this
        )

        goStringTypeId -> GoUtPrimitiveModel("", this)
        goUintPtrTypeId -> GoUtPrimitiveModel(0, this)

        else -> error("Go primitive ${this.javaClass} is not supported")
    }

    is GoStructTypeId -> GoUtStructModel(listOf(), this, destinationPackage, "")
    is GoArrayTypeId -> GoUtArrayModel(hashMapOf(), this, destinationPackage)
    else -> GoUtNilModel(this)
}