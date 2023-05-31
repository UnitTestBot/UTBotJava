package org.utbot.go.api.util

import org.utbot.go.api.*
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel
import kotlin.properties.Delegates
import kotlin.reflect.KClass

var intSize by Delegates.notNull<Int>()

val goByteTypeId = GoPrimitiveTypeId("byte")
val goBoolTypeId = GoPrimitiveTypeId("bool")

val goComplex64TypeId = GoPrimitiveTypeId("complex64")
val goComplex128TypeId = GoPrimitiveTypeId("complex128")

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
private fun GoPrimitiveTypeId.correspondingKClass(): KClass<out Any> = when (this) {
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

fun rawValueOfGoPrimitiveTypeToValue(typeId: GoPrimitiveTypeId, rawValue: String): Any =
    when (typeId.correspondingKClass()) {
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

/**
 * This method is useful for creating a GoUtModel with a default value.
 */
fun GoTypeId.goDefaultValueModel(): GoUtModel = when (this) {
    is GoPrimitiveTypeId -> when (this) {
        goByteTypeId -> GoUtPrimitiveModel("0".toUByte(), this)
        goBoolTypeId -> GoUtPrimitiveModel(false, this)
        goComplex64TypeId -> GoUtComplexModel(
            goFloat32TypeId.goDefaultValueModel() as GoUtPrimitiveModel,
            goFloat32TypeId.goDefaultValueModel() as GoUtPrimitiveModel,
            this
        )

        goComplex128TypeId -> GoUtComplexModel(
            goFloat64TypeId.goDefaultValueModel() as GoUtPrimitiveModel,
            goFloat64TypeId.goDefaultValueModel() as GoUtPrimitiveModel,
            this
        )

        goFloat32TypeId -> GoUtPrimitiveModel(0.0f, this)
        goFloat64TypeId -> GoUtPrimitiveModel(0.0, this)
        goInt8TypeId -> GoUtPrimitiveModel("0".toByte(), this)
        goInt16TypeId -> GoUtPrimitiveModel("0".toShort(), this)
        goInt32TypeId -> GoUtPrimitiveModel("0".toInt(), this)
        goIntTypeId -> if (intSize == 32) {
            GoUtPrimitiveModel("0".toInt(), this)
        } else {
            GoUtPrimitiveModel("0".toLong(), this)
        }

        goInt64TypeId -> GoUtPrimitiveModel("0".toLong(), this)

        goRuneTypeId -> GoUtPrimitiveModel("0".toInt(), this)
        goStringTypeId -> GoUtPrimitiveModel("", this)
        goUint8TypeId -> GoUtPrimitiveModel("0".toUByte(), this)
        goUint16TypeId -> GoUtPrimitiveModel("0".toUShort(), this)
        goUint32TypeId -> GoUtPrimitiveModel("0".toUInt(), this)
        goUintTypeId -> if (intSize == 32) {
            GoUtPrimitiveModel("0".toUInt(), this)
        } else {
            GoUtPrimitiveModel("0".toULong(), this)
        }

        goUint64TypeId -> GoUtPrimitiveModel("0".toULong(), this)
        goUintPtrTypeId -> GoUtPrimitiveModel("0".toULong(), this)

        else -> error("Generating Go default value model for ${this.javaClass} is not supported")
    }

    is GoArrayTypeId -> GoUtArrayModel(
        value = (0 until this.length)
            .map { this.elementTypeId!!.goDefaultValueModel() }
            .toTypedArray(),
        typeId = this,
    )

    is GoStructTypeId -> GoUtStructModel(linkedMapOf(), this)
    is GoSliceTypeId -> GoUtNilModel(this)
    is GoMapTypeId -> GoUtNilModel(this)
    is GoChanTypeId -> GoUtNilModel(this)
    is GoPointerTypeId -> GoUtNilModel(this)
    is GoNamedTypeId -> GoUtNamedModel(this.underlyingTypeId.goDefaultValueModel(), this)
    is GoInterfaceTypeId -> GoUtNilModel(this)
    else -> error("Generating Go default value model for ${this.javaClass} is not supported")
}

fun GoTypeId.getAllVisibleNamedTypes(goPackage: GoPackage, visitedTypes: MutableSet<GoTypeId>): Set<GoNamedTypeId> {
    if (visitedTypes.contains(this)) {
        return emptySet()
    }
    visitedTypes.add(this)
    return when (this) {
        is GoNamedTypeId -> if (this.sourcePackage == goPackage || this.sourcePackage.isBuiltin || this.exported()) {
            setOf(this) + underlyingTypeId.getAllVisibleNamedTypes(goPackage, visitedTypes)
        } else {
            emptySet()
        }

        is GoStructTypeId -> fields.fold(emptySet()) { acc: Set<GoNamedTypeId>, field ->
            acc + (field.declaringType).getAllVisibleNamedTypes(goPackage, visitedTypes)
        }

        is GoArrayTypeId, is GoSliceTypeId, is GoChanTypeId, is GoPointerTypeId ->
            elementTypeId!!.getAllVisibleNamedTypes(goPackage, visitedTypes)

        is GoMapTypeId -> keyTypeId.getAllVisibleNamedTypes(goPackage, visitedTypes) +
                elementTypeId!!.getAllVisibleNamedTypes(goPackage, visitedTypes)

        is GoInterfaceTypeId -> implementations.fold(emptySet()) { acc, type ->
            acc + type.getAllVisibleNamedTypes(goPackage, visitedTypes)
        }

        else -> emptySet()
    }
}

fun List<GoTypeId>.getAllVisibleNamedTypes(goPackage: GoPackage): Set<GoNamedTypeId> {
    val visitedTypes = mutableSetOf<GoTypeId>()
    return this.fold(emptySet()) { acc, type ->
        acc + type.getAllVisibleNamedTypes(goPackage, visitedTypes)
    }
}

fun GoNamedTypeId.getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> =
    if (!this.sourcePackage.isBuiltin && this.sourcePackage != destinationPackage) {
        setOf(this.sourcePackage)
    } else {
        emptySet()
    }