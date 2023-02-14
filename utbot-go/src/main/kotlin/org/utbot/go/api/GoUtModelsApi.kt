@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package org.utbot.go.api

import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.api.util.goFloat64TypeId
import org.utbot.go.api.util.goStringTypeId
import org.utbot.go.api.util.neverRequiresExplicitCast
import org.utbot.go.framework.api.go.*

// NEVER and DEPENDS difference is useful in code generation of assert.Equals(...).
enum class ExplicitCastMode {
    REQUIRED, NEVER, DEPENDS
}

open class GoUtPrimitiveModel(
    val value: Any,
    typeId: GoPrimitiveTypeId,
    val explicitCastMode: ExplicitCastMode =
        if (typeId.neverRequiresExplicitCast) {
            ExplicitCastMode.NEVER
        } else {
            ExplicitCastMode.DEPENDS
        },
    private val requiredPackages: Set<GoPackage> = emptySet(),
) : GoUtModel(typeId) {
    override val typeId: GoPrimitiveTypeId
        get() = super.typeId as GoPrimitiveTypeId

    override fun getRequiredPackages(): Set<GoPackage> = requiredPackages

    override fun isComparable(): Boolean = true

    override fun toString(): String = when (explicitCastMode) {
        ExplicitCastMode.REQUIRED -> toCastedValueGoCode()
        ExplicitCastMode.DEPENDS, ExplicitCastMode.NEVER -> toValueGoCode()
    }

    open fun toValueGoCode(): String = if (typeId == goStringTypeId) "\"$value\"" else "$value"
    fun toCastedValueGoCode(): String = "$typeId(${toValueGoCode()})"
}

abstract class GoUtCompositeModel(
    typeId: GoTypeId,
    val destinationPackage: GoPackage,
) : GoUtModel(typeId)

class GoUtStructModel(
    val value: List<GoUtFieldModel>,
    typeId: GoStructTypeId,
    destinationPackage: GoPackage,
    private val alias: String,
) : GoUtCompositeModel(typeId, destinationPackage) {
    override val typeId: GoStructTypeId
        get() = super.typeId as GoStructTypeId

    // TODO delete this method because it seems like all fields are visible
    private fun getVisibleFields(): List<GoUtFieldModel> =
        value.filter { destinationPackage == typeId.sourcePackage || it.fieldId.isExported }

    override fun getRequiredPackages(): Set<GoPackage> = getVisibleFields()
        .fold(setOf(typeId.sourcePackage)) { acc, fieldModel -> acc + fieldModel.getRequiredPackages() }

    override fun isComparable(): Boolean = value.all { it.isComparable() }

    fun toStringWithoutStructName(): String = getVisibleFields()
        .joinToString(prefix = "{", postfix = "}") { "${it.fieldId.name}: ${it.model}" }

    override fun toString(): String =
        "${typeId.getRelativeName(destinationPackage, alias)}${toStringWithoutStructName()}"
}

class GoUtArrayModel(
    val value: MutableMap<Int, GoUtModel>,
    typeId: GoArrayTypeId,
    destinationPackage: GoPackage,
) : GoUtCompositeModel(typeId, destinationPackage) {
    val length: Int = typeId.length

    override val typeId: GoArrayTypeId
        get() = super.typeId as GoArrayTypeId

    override fun getRequiredPackages(): Set<GoPackage> {
        val elementStructTypeId = typeId.elementTypeId as? GoStructTypeId
        val imports = if (elementStructTypeId != null && elementStructTypeId.sourcePackage != destinationPackage) {
            mutableSetOf(elementStructTypeId.sourcePackage)
        } else {
            mutableSetOf()
        }
        value.values.map { it.getRequiredPackages() }.forEach { imports += it }
        return imports
    }

    override fun isComparable(): Boolean = value.values.all { it.isComparable() }

    fun getElements(typeId: GoTypeId): List<GoUtModel> = (0 until length).map {
        value[it] ?: typeId.goDefaultValueModel(destinationPackage)
    }

    fun toStringWithoutTypeName(): String = when (val typeId = typeId.elementTypeId!!) {
        is GoStructTypeId -> getElements(typeId).joinToString(prefix = "{", postfix = "}") {
            (it as GoUtStructModel).toStringWithoutStructName()
        }

        is GoArrayTypeId -> getElements(typeId).joinToString(prefix = "{", postfix = "}") {
            (it as GoUtArrayModel).toStringWithoutTypeName()
        }

        else -> getElements(typeId).joinToString(prefix = "{", postfix = "}")
    }

    override fun toString(): String = when (val typeId = typeId.elementTypeId!!) {
        is GoStructTypeId -> getElements(typeId)
            .joinToString(prefix = "[$length]${typeId.getRelativeName(destinationPackage)}{", postfix = "}") {
                (it as GoUtStructModel).toStringWithoutStructName()
            }

        is GoArrayTypeId -> getElements(typeId)
            .joinToString(prefix = "[$length]${typeId.getRelativeName(destinationPackage)}{", postfix = "}") {
                (it as GoUtArrayModel).toStringWithoutTypeName()
            }

        else -> getElements(typeId)
            .joinToString(prefix = "[$length]${typeId.getRelativeName(destinationPackage)}{", postfix = "}")
    }
}

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
}

class GoUtFloatInfModel(
    val sign: Int,
    typeId: GoPrimitiveTypeId
) : GoUtPrimitiveModel(
    "math.Inf($sign)",
    typeId,
    explicitCastMode = if (typeId != goFloat64TypeId) {
        ExplicitCastMode.REQUIRED
    } else {
        ExplicitCastMode.NEVER
    },
    requiredPackages = setOf(GoPackage("math", "math")),
)

class GoUtComplexModel(
    var realValue: GoUtPrimitiveModel,
    var imagValue: GoUtPrimitiveModel,
    typeId: GoPrimitiveTypeId,
) : GoUtPrimitiveModel(
    "complex($realValue, $imagValue)",
    typeId,
    requiredPackages = realValue.getRequiredPackages() + imagValue.getRequiredPackages(),
    explicitCastMode = ExplicitCastMode.NEVER
) {
    override fun isComparable(): Boolean = realValue.isComparable() && imagValue.isComparable()
    override fun toValueGoCode(): String = "complex($realValue, $imagValue)"
}

class GoUtNilModel(
    typeId: GoTypeId
) : GoUtModel(typeId) {
    override fun isComparable(): Boolean = true
    override fun toString() = "nil"
}