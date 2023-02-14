package org.utbot.go.simplecodegeneration

import org.utbot.go.api.GoUtFloatNaNModel
import org.utbot.go.api.GoUtPrimitiveModel

object GoUtModelToCodeConverter {
    fun goUtModelToCode(goUtModel: GoUtPrimitiveModel): String {
        return "1"
    }

    fun goUtModelToCode(goUtModel: GoUtFloatNaNModel): String {
        return "2"
    }
}
