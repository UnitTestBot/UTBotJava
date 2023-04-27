package org.utbot.python.evaluation.utils

import org.utbot.framework.plugin.api.Instruction
import org.utbot.python.PythonMethod
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.pythonTypeRepresentation

fun coveredLinesToInstructions(coveredLines: Collection<Int>, method: PythonMethod): List<Instruction> {
    return coveredLines.map {
        Instruction(
            method.containingPythonClass?.pythonTypeRepresentation() ?: pythonAnyClassId.name,
            method.methodSignature(),
            it,
            it.toLong()
        )
    }
}
