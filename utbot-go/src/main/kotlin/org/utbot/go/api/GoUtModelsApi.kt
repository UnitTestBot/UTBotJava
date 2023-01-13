@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package org.utbot.go.api

import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.api.util.goFloat64TypeId
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
    val typeId: GoPrimitiveTypeId,
    val explicitCastMode: ExplicitCastMode =
        if (typeId.neverRequiresExplicitCast) {
            ExplicitCastMode.NEVER
        } else {
            ExplicitCastMode.DEPENDS
        },
    requiredImports: Set<String> = emptySet(),
) : GoUtModel(typeId, requiredImports) {
    override fun toString(): String = when (explicitCastMode) {
        ExplicitCastMode.REQUIRED -> toCastedValueGoCode()
        ExplicitCastMode.DEPENDS, ExplicitCastMode.NEVER -> toValueGoCode()
    }

    override fun canNotBeEqual(): Boolean = false

    open fun toValueGoCode(): String = "$value"
    fun toCastedValueGoCode(): String = "$typeId(${toValueGoCode()})"
}

abstract class GoUtCompositeModel(
    typeId: GoTypeId,
    val packageName: String,
) : GoUtModel(typeId) {
    abstract override val requiredImports: Set<String>
}

class GoUtStructModel(
    val value: List<GoUtFieldModel>,
    typeId: GoStructTypeId,
    packageName: String,
) : GoUtCompositeModel(typeId, packageName) {
    override val classId: GoStructTypeId
        get() = super.classId as GoStructTypeId

    override val requiredImports: Set<String>
        get() {
            val structTypeId = classId
            val imports =
                if (structTypeId.packageName != packageName) {
                    mutableSetOf(structTypeId.packagePath)
                } else {
                    mutableSetOf()
                }
            value.map { it.model.requiredImports }.forEach { imports += it }
            return imports
        }

    fun toStringWithoutStructName(): String = value.filter { it.fieldId.isExported }
        .joinToString(prefix = "{", postfix = "}") { "${it.fieldId.name}: ${it.model}" }

    override fun toString(): String =
        "${classId.getRelativeName(packageName)}${toStringWithoutStructName()}"

    override fun canNotBeEqual(): Boolean = value.any { it.model.canNotBeEqual() }
}

class GoUtArrayModel(
    val value: MutableMap<Int, GoUtModel>,
    typeId: GoArrayTypeId,
    packageName: String,
) : GoUtCompositeModel(typeId, packageName) {
    val length: Int = typeId.length

    override val classId: GoArrayTypeId
        get() = super.classId as GoArrayTypeId

    override val requiredImports: Set<String>
        get() {
            val structTypeId = classId.elementTypeId as? GoStructTypeId
            val imports = if (structTypeId != null && structTypeId.packageName != packageName) {
                mutableSetOf(structTypeId.packagePath)
            } else {
                mutableSetOf()
            }
            value.entries.map { it.value.requiredImports }.forEach { imports += it }
            return imports
        }

    private fun getElements(typeId: GoTypeId): List<GoUtModel> = (0 until length).map {
        value[it] ?: typeId.goDefaultValueModel(packageName)
    }

    fun toStringWithoutTypeName(): String = when (val typeId = classId.elementTypeId) {
        is GoStructTypeId -> getElements(typeId).joinToString(prefix = "{", postfix = "}") {
            (it as GoUtStructModel).toStringWithoutStructName()
        }

        is GoArrayTypeId -> getElements(typeId).joinToString(prefix = "{", postfix = "}") {
            (it as GoUtArrayModel).toStringWithoutTypeName()
        }

        else -> getElements(typeId).joinToString(prefix = "{", postfix = "}")
    }

    override fun toString(): String = when (val typeId = classId.elementTypeId) {
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

    override fun canNotBeEqual(): Boolean = value.values.any { it.canNotBeEqual() }
}

class GoUtFloatNaNModel(
    typeId: GoPrimitiveTypeId
) : GoUtPrimitiveModel(
    "math.NaN()",
    typeId,
    requiredImports = setOf("math"),
    explicitCastMode = if (typeId != goFloat64TypeId) {
        ExplicitCastMode.REQUIRED
    } else {
        ExplicitCastMode.NEVER
    }
) {
    override fun canNotBeEqual(): Boolean = true
}

class GoUtFloatInfModel(
    val sign: Int,
    typeId: GoPrimitiveTypeId
) : GoUtPrimitiveModel(
    "math.Inf($sign)",
    typeId,
    requiredImports = setOf("math"),
    explicitCastMode = if (typeId != goFloat64TypeId) {
        ExplicitCastMode.REQUIRED
    } else {
        ExplicitCastMode.NEVER
    }
)

class GoUtComplexModel(
    val realValue: GoUtPrimitiveModel,
    val imagValue: GoUtPrimitiveModel,
    typeId: GoPrimitiveTypeId,
) : GoUtPrimitiveModel(
    "complex($realValue, $imagValue)",
    typeId,
    requiredImports = realValue.requiredImports + imagValue.requiredImports,
    explicitCastMode = ExplicitCastMode.NEVER
) {
    override fun canNotBeEqual(): Boolean = realValue.canNotBeEqual() || imagValue.canNotBeEqual()
}

class GoUtNilModel(
    typeId: GoTypeId
) : GoUtModel(typeId, emptySet()) {
    override fun toString() = "nil"

    override fun canNotBeEqual(): Boolean = false
}