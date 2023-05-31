@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package org.utbot.go.api

import org.utbot.go.api.util.*
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

// NEVER and DEPENDS difference is useful in code generation of assert.Equals(...).
enum class ExplicitCastMode {
    REQUIRED, NEVER, DEPENDS
}

/**
 * Class for Go primitive model.
 */
open class GoUtPrimitiveModel(
    val value: Any,
    typeId: GoPrimitiveTypeId,
    val explicitCastMode: ExplicitCastMode = if (typeId.neverRequiresExplicitCast) {
        ExplicitCastMode.NEVER
    } else {
        ExplicitCastMode.DEPENDS
    },
    private val requiredPackages: Set<GoPackage> = emptySet(),
) : GoUtModel(typeId) {
    override val typeId: GoPrimitiveTypeId
        get() = super.typeId as GoPrimitiveTypeId

    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> = requiredPackages

    override fun isComparable(): Boolean = true

    override fun toString(): String = if (typeId == goStringTypeId) "\"${value}\"" else "$value"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtPrimitiveModel) return false

        return typeId == other.typeId && value == other.value
    }

    override fun hashCode(): Int = 31 * value.hashCode() + typeId.hashCode()
}

/**
 * Class for Go struct model.
 */
class GoUtStructModel(
    val value: LinkedHashMap<GoFieldId, GoUtModel>,
    typeId: GoStructTypeId,
) : GoUtModel(typeId) {
    override val typeId: GoStructTypeId
        get() = super.typeId as GoStructTypeId

    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> =
        value.values.fold(emptySet()) { acc, fieldModel ->
            acc + fieldModel.getRequiredPackages(destinationPackage)
        }

    override fun isComparable(): Boolean = value.values.all { it.isComparable() }

    override fun toString(): String =
        value.entries.joinToString(prefix = "struct{", postfix = "}") { (fieldId, model) ->
            "${fieldId.name}: $model"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtStructModel) return false

        return typeId == other.typeId && value == other.value
    }

    override fun hashCode(): Int = 31 * value.hashCode() + typeId.hashCode()
}

/**
 * Class for Go array model.
 */
class GoUtArrayModel(
    val value: Array<GoUtModel>,
    typeId: GoArrayTypeId,
) : GoUtModel(typeId) {
    val length: Int = typeId.length

    override val typeId: GoArrayTypeId
        get() = super.typeId as GoArrayTypeId

    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> {
        val elementNamedTypeId = typeId.elementTypeId as? GoNamedTypeId
        val imports = elementNamedTypeId?.getRequiredPackages(destinationPackage) ?: emptySet()
        return value.fold(imports) { acc, model ->
            acc + model.getRequiredPackages(destinationPackage)
        }
    }

    override fun isComparable(): Boolean = value.all { it.isComparable() }

    fun getElements(): List<GoUtModel> = value.toList()

    override fun toString(): String = getElements().joinToString(prefix = "$typeId{", postfix = "}") {
        it.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtArrayModel) return false

        return typeId == other.typeId && value.contentEquals(other.value) && length == other.length
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + length
        result = 31 * result + typeId.hashCode()
        return result
    }
}

/**
 * Class for Go slice model.
 */
class GoUtSliceModel(
    val value: Array<GoUtModel?>,
    typeId: GoSliceTypeId,
    val length: Int,
) : GoUtModel(typeId) {
    override val typeId: GoSliceTypeId
        get() = super.typeId as GoSliceTypeId

    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> {
        val elementNamedTypeId = typeId.elementTypeId as? GoNamedTypeId
        val imports = elementNamedTypeId?.getRequiredPackages(destinationPackage) ?: emptySet()
        return value.filterNotNull().fold(imports) { acc, model ->
            acc + model.getRequiredPackages(destinationPackage)
        }
    }

    override fun isComparable(): Boolean = value.all { it?.isComparable() ?: true }

    fun getElements(): List<GoUtModel> = value.map {
        it ?: typeId.elementTypeId!!.goDefaultValueModel()
    }

    override fun toString(): String = getElements().joinToString(prefix = "$typeId{", postfix = "}") {
        it.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtSliceModel) return false

        return typeId == other.typeId && value.contentEquals(other.value) && length == other.length
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + length
        result = 31 * result + typeId.hashCode()
        return result
    }
}

/**
 * Class for Go map model.
 */
class GoUtMapModel(
    val value: MutableMap<GoUtModel, GoUtModel>,
    typeId: GoMapTypeId,
) : GoUtModel(typeId) {
    override val typeId: GoMapTypeId
        get() = super.typeId as GoMapTypeId

    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> {
        val keyNamedTypeId = typeId.keyTypeId as? GoNamedTypeId
        var imports = keyNamedTypeId?.getRequiredPackages(destinationPackage) ?: emptySet()
        val elementNamedTypeId = typeId.elementTypeId as? GoNamedTypeId
        imports = imports + (elementNamedTypeId?.getRequiredPackages(destinationPackage) ?: emptySet())
        return value.values.fold(imports) { acc, model ->
            acc + model.getRequiredPackages(destinationPackage)
        }
    }

    override fun isComparable(): Boolean =
        value.keys.all { it.isComparable() } && value.values.all { it.isComparable() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtMapModel) return false

        return typeId == other.typeId && value == other.value
    }

    override fun hashCode(): Int = 31 * value.hashCode() + typeId.hashCode()
}

/**
 * Class for Go chan model.
 */
class GoUtChanModel(
    val value: Array<GoUtModel?>,
    typeId: GoChanTypeId
) : GoUtModel(typeId) {
    override val typeId: GoChanTypeId
        get() = super.typeId as GoChanTypeId

    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> {
        val elementNamedTypeId = typeId.elementTypeId as? GoNamedTypeId
        val imports = elementNamedTypeId?.getRequiredPackages(destinationPackage) ?: emptySet()
        return value.filterNotNull().fold(imports) { acc, model ->
            acc + model.getRequiredPackages(destinationPackage)
        }
    }

    override fun isComparable(): Boolean = value.all { it?.isComparable() ?: true }

    fun getElements(): List<GoUtModel> = value.filterNotNull()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtChanModel) return false

        return typeId == other.typeId && value.contentEquals(other.value)
    }

    override fun hashCode(): Int = 31 * value.hashCode() + typeId.hashCode()
}

/**
 * Class for Go model with the IEEE 754 “not-a-number” value.
 */
class GoUtFloatNaNModel(
    typeId: GoPrimitiveTypeId
) : GoUtPrimitiveModel(
    "math.NaN()",
    typeId,
    explicitCastMode = if (typeId != goFloat64TypeId) {
        ExplicitCastMode.REQUIRED
    } else {
        ExplicitCastMode.NEVER
    },
    requiredPackages = setOf(GoPackage("math", "math")),
) {
    override fun isComparable(): Boolean = false

    override fun equals(other: Any?): Boolean = this === other || other is GoUtFloatNaNModel

    override fun hashCode(): Int = typeId.hashCode()

    override fun toString(): String = "NaN"
}

/**
 * Class for Go model with infinity.
 */
class GoUtFloatInfModel(
    val sign: Int, typeId: GoPrimitiveTypeId
) : GoUtPrimitiveModel(
    "math.Inf($sign)",
    typeId,
    explicitCastMode = if (typeId != goFloat64TypeId) {
        ExplicitCastMode.REQUIRED
    } else {
        ExplicitCastMode.NEVER
    },
    requiredPackages = setOf(GoPackage("math", "math")),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtFloatInfModel) return false

        return sign == other.sign
    }

    override fun hashCode(): Int = sign.hashCode()

    override fun toString(): String = if (sign >= 0) {
        "+Inf"
    } else {
        "-Inf"
    }
}

/**
 * Class for Go models with complex numbers.
 */
class GoUtComplexModel(
    var realValue: GoUtPrimitiveModel,
    var imagValue: GoUtPrimitiveModel,
    typeId: GoPrimitiveTypeId,
) : GoUtPrimitiveModel(
    "complex($realValue, $imagValue)",
    typeId,
    explicitCastMode = ExplicitCastMode.NEVER
) {
    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> =
        realValue.getRequiredPackages(destinationPackage) + imagValue.getRequiredPackages(destinationPackage)

    override fun isComparable(): Boolean = realValue.isComparable() && imagValue.isComparable()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtComplexModel) return false

        return realValue == other.realValue && imagValue == other.imagValue
    }

    override fun hashCode(): Int = 31 * realValue.hashCode() + imagValue.hashCode()
}

/**
 * Class for Go model with nil.
 */
class GoUtNilModel(
    typeId: GoTypeId
) : GoUtModel(typeId) {
    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> = emptySet()

    override fun isComparable(): Boolean = true

    override fun toString() = "nil"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtNilModel) return false

        return typeId == other.typeId
    }

    override fun hashCode(): Int = typeId.hashCode()
}

/**
 * Class for Go named model.
 */
class GoUtNamedModel(
    var value: GoUtModel,
    typeId: GoNamedTypeId,
) : GoUtModel(typeId) {
    override val typeId: GoNamedTypeId
        get() = super.typeId as GoNamedTypeId

    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> =
        typeId.getRequiredPackages(destinationPackage) + value.getRequiredPackages(destinationPackage)

    override fun isComparable(): Boolean = value.isComparable()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtNamedModel) return false

        return typeId == other.typeId && value == other.value
    }

    override fun hashCode(): Int = 31 * value.hashCode() + typeId.hashCode()
}

/**
 * Class for Go pointer model.
 */
class GoUtPointerModel(
    var value: GoUtModel,
    typeId: GoPointerTypeId
) : GoUtModel(typeId) {
    override val typeId: GoPointerTypeId
        get() = super.typeId as GoPointerTypeId

    override fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage> =
        value.getRequiredPackages(destinationPackage)

    override fun isComparable(): Boolean = value.isComparable()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoUtPointerModel) return false

        return typeId == other.typeId && value == other.value
    }

    override fun hashCode(): Int = 31 * value.hashCode() + typeId.hashCode()
}