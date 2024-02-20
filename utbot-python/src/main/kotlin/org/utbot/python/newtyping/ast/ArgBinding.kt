package org.utbot.python.newtyping.ast

import org.utpython.types.PythonSubtypeChecker
import org.utpython.types.PythonTypeHintsStorage
import org.utpython.types.general.FunctionType

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