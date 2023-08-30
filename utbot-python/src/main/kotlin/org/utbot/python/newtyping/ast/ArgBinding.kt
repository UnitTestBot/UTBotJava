package org.utbot.python.newtyping.ast

import org.utbot.python.newtyping.PythonSubtypeChecker
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.general.FunctionType

// TODO: consider different types of parameters
fun signaturesAreCompatible(
    functionSignature: FunctionType,
    callSignature: FunctionType,
    storage: PythonTypeHintsStorage
): Boolean {
    if (functionSignature.arguments.size != callSignature.arguments.size)
        return false
    return (functionSignature.arguments zip callSignature.arguments).all { (funcArg, callArg) ->
        PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(funcArg, callArg, storage)
    }
}