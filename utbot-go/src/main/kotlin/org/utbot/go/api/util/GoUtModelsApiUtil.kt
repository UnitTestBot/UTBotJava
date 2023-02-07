package org.utbot.go.api.util

import org.utbot.go.api.GoUtComplexModel
import org.utbot.go.api.GoUtFloatInfModel
import org.utbot.go.api.GoUtFloatNaNModel
import org.utbot.go.framework.api.go.GoUtModel

fun GoUtModel.isNaNOrInf(): Boolean = this is GoUtFloatNaNModel || this is GoUtFloatInfModel

fun GoUtModel.doesNotContainNaNOrInf(): Boolean {
    if (this.isNaNOrInf()) return false
    val asComplexModel = (this as? GoUtComplexModel) ?: return true
    return !(asComplexModel.realValue.isNaNOrInf() || asComplexModel.imagValue.isNaNOrInf())
}

fun GoUtModel.containsNaNOrInf(): Boolean = !this.doesNotContainNaNOrInf()