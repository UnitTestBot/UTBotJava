@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package org.utbot.go.api

import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.api.util.goFloat64TypeId
import org.utbot.go.api.util.neverRequiresExplicitCast

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

    open fun toValueGoCode(): String = "$value"
    fun toCastedValueGoCode(): String = "$typeId(${toValueGoCode()})"
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
)

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
)

class GoUtNilModel(
    val typeId: GoTypeId
) : GoUtModel(typeId, emptySet()) {
    override fun toString() = "nil"
}