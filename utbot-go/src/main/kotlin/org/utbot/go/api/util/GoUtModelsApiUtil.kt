package org.utbot.go.api.util

import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.api.*

fun getExplicitCastModeForFloatModel(
    typeId: GoTypeId,
    explicitCastRequired: Boolean,
    defaultFloat32Mode: ExplicitCastMode
): ExplicitCastMode {
    if (explicitCastRequired) {
        return ExplicitCastMode.REQUIRED
    }
    return when (typeId) {
        goFloat32TypeId -> defaultFloat32Mode
        goFloat64TypeId -> ExplicitCastMode.NEVER
        else -> error("illegal type")
    }
}

fun GoUtModel.isNaNOrInf(): Boolean = this is GoUtFloatNaNModel || this is GoUtFloatInfModel

fun GoUtModel.doesNotContainNaNOrInf(): Boolean {
    if (this.isNaNOrInf()) return false
    val asComplexModel = (this as? GoUtComplexModel) ?: return true
    return !(asComplexModel.realValue.isNaNOrInf() || asComplexModel.imagValue.isNaNOrInf())
}

fun GoUtModel.containsNaNOrInf(): Boolean = !this.doesNotContainNaNOrInf()