@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package org.utbot.go.api

import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.api.util.goFloat64TypeId
import org.utbot.go.api.util.neverRequiresExplicitCast
import org.utbot.go.framework.api.go.GoUtModel

// NEVER and DEPENDS difference is useful in code generation of assert.Equals(...).
enum class ExplicitCastMode {
    REQUIRED, NEVER, DEPENDS
}

open class GoUtPrimitiveModel(
    val value: Any,
    val typeId: GoTypeId,
    requiredImports: Set<String> = emptySet(),
    val explicitCastMode: ExplicitCastMode =
        if (typeId.neverRequiresExplicitCast) {
            ExplicitCastMode.NEVER
        } else {
            ExplicitCastMode.DEPENDS
        }
) : GoUtModel(typeId, requiredImports) {
    override fun toString() = when (explicitCastMode) {
        ExplicitCastMode.REQUIRED -> toCastedValueGoCode()
        ExplicitCastMode.DEPENDS, ExplicitCastMode.NEVER -> toValueGoCode()
    }

    override fun canNotBeEqual(): Boolean = false

    open fun toValueGoCode(): String = "$value"
    fun toCastedValueGoCode(): String = "$typeId(${toValueGoCode()})"
}

class GoUtStructModel(
    val value: List<Pair<String, GoUtModel>>,
    typeId: GoTypeId,
    requiredImports: Set<String> = emptySet()
) : GoUtModel(typeId, requiredImports) {
    fun toStringWithoutStructName(): String = "{${value.joinToString { "${it.first}: ${it.second}" }}}"
    override fun toString(): String = "${classId.name}${toStringWithoutStructName()}"

    override fun canNotBeEqual(): Boolean = value.any { (_, model) -> model.canNotBeEqual() }
}

class GoUtArrayModel(
    val value: MutableMap<Int, GoUtModel>,
    typeId: GoArrayTypeId,
    val length: Int,
    requiredImports: Set<String> = emptySet()
) : GoUtModel(typeId, requiredImports) {
    override val classId: GoArrayTypeId
        get() = super.classId as GoArrayTypeId

//

    override fun toString(): String = when (classId.elementTypeId) {
        is GoStructTypeId -> (0 until length).map {
            value[it] ?: this.classId.getDefaultValueModelForElement()
        }.joinToString(prefix = "[$length]${classId.elementTypeId.simpleName}{", postfix = "}") {
            (it as GoUtStructModel).toStringWithoutStructName()
        }
//        is GoArrayTypeId -> (0 until length).map {
//            value[it] ?: this.classId.getDefaultValueModelForElement()
//        }.joinToString(prefix = "[$length]${classId.elementTypeId.simpleName}{", postfix = "}") {
//            (it as GoUtArrayModel).to
//        }
        else -> (0 until length).map { value[it] ?: this.classId.elementTypeId.goDefaultValueModel() }
            .joinToString(prefix = "[$length]${classId.elementClassId!!.simpleName}{", postfix = "}")
    }

    override fun canNotBeEqual(): Boolean = value.values.any { it.canNotBeEqual() }
}

class GoUtFloatNaNModel(
    typeId: GoTypeId
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
    typeId: GoTypeId
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
    typeId: GoTypeId,
) : GoUtPrimitiveModel(
    "complex($realValue, $imagValue)",
    typeId,
    requiredImports = realValue.requiredImports + imagValue.requiredImports,
    explicitCastMode = ExplicitCastMode.NEVER
) {
    override fun canNotBeEqual(): Boolean = realValue.canNotBeEqual() || imagValue.canNotBeEqual()
}

class GoUtNilModel(
    val typeId: GoTypeId
) : GoUtModel(typeId, emptySet()) {
    override fun toString() = "nil"
    override fun canNotBeEqual(): Boolean = false
}