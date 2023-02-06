@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package org.utbot.go.api

import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.api.util.goFloat64TypeId
import org.utbot.go.api.util.goStringTypeId
import org.utbot.go.api.util.neverRequiresExplicitCast
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtFieldModel
import org.utbot.go.framework.api.go.GoUtModel

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
    private val requiredImports: Set<String> = emptySet(),
) : GoUtModel(typeId) {
    override val typeId: GoPrimitiveTypeId
        get() = super.typeId as GoPrimitiveTypeId

    override fun getRequiredImports(): Set<String> = requiredImports

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
    val packageName: String,
) : GoUtModel(typeId)

class GoUtStructModel(
    val value: List<GoUtFieldModel>,
    typeId: GoStructTypeId,
    packageName: String,
) : GoUtCompositeModel(typeId, packageName) {
    override val typeId: GoStructTypeId
        get() = super.typeId as GoStructTypeId

    override fun getRequiredImports(): Set<String> {
        val imports = if (typeId.packageName != packageName) {
            mutableSetOf(typeId.packagePath)
        } else {
            mutableSetOf()
        }
        value.filter { packageName == typeId.packageName || it.fieldId.isExported }
            .map { it.getRequiredImports() }
            .forEach { imports += it }
        return imports
    }

    override fun isComparable(): Boolean = value.all { it.isComparable() }

    fun toStringWithoutStructName(): String =
        value.filter { packageName == typeId.packageName || it.fieldId.isExported }
            .joinToString(prefix = "{", postfix = "}") { "${it.fieldId.name}: ${it.model}" }

    override fun toString(): String =
        "${typeId.getRelativeName(packageName)}${toStringWithoutStructName()}"
}

class GoUtArrayModel(
    val value: MutableMap<Int, GoUtModel>,
    typeId: GoArrayTypeId,
    packageName: String,
) : GoUtCompositeModel(typeId, packageName) {
    val length: Int = typeId.length

    override val typeId: GoArrayTypeId
        get() = super.typeId as GoArrayTypeId

    override fun getRequiredImports(): Set<String> {
        val elementStructTypeId = typeId.elementTypeId as? GoStructTypeId
        val imports = if (elementStructTypeId != null && elementStructTypeId.packageName != packageName) {
            mutableSetOf(elementStructTypeId.packagePath)
        } else {
            mutableSetOf()
        }
        value.values.map { it.getRequiredImports() }.forEach { imports += it }
        return imports
    }

    override fun isComparable(): Boolean = value.values.all { it.isComparable() }

    fun getElements(typeId: GoTypeId): List<GoUtModel> = (0 until length).map {
        value[it] ?: typeId.goDefaultValueModel(packageName)
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
            .joinToString(prefix = "[$length]${typeId.getRelativeName(packageName)}{", postfix = "}") {
                (it as GoUtStructModel).toStringWithoutStructName()
            }

        is GoArrayTypeId -> getElements(typeId)
            .joinToString(prefix = "[$length]${typeId.getRelativeName(packageName)}{", postfix = "}") {
                (it as GoUtArrayModel).toStringWithoutTypeName()
            }

        else -> getElements(typeId)
            .joinToString(prefix = "[$length]${typeId.getRelativeName(packageName)}{", postfix = "}")
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
    requiredImports = setOf("math"),
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
    requiredImports = setOf("math"),
)

class GoUtComplexModel(
    var realValue: GoUtPrimitiveModel,
    var imagValue: GoUtPrimitiveModel,
    typeId: GoPrimitiveTypeId,
) : GoUtPrimitiveModel(
    "complex($realValue, $imagValue)",
    typeId,
    requiredImports = realValue.getRequiredImports() + imagValue.getRequiredImports(),
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