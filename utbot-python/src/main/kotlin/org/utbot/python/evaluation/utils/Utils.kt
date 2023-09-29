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

data class PyInstruction(
    val lineNumber: Int,
    val offset: Long
) {
    constructor(lineNumber: Int) : this(lineNumber, lineNumber.toLong())
}

fun String.toPyInstruction(): PyInstruction? {
    val data = this.split(":")
    if (data.size == 2) {
        val line = data[0].toInt()
        val offset = data[1].toLong()
        return PyInstruction(line, offset)
    } else if (data.size == 1) {
        val line = data[0].toInt()
        return PyInstruction(line)
    }
    return null
}

fun makeInstructions(coveredInstructions: Collection<PyInstruction>, method: PythonMethod): List<Instruction> {
    return coveredInstructions.map {
        val line = it.lineNumber
        val offset = it.offset
        Instruction(
            method.containingPythonClass?.pythonTypeRepresentation() ?: pythonAnyClassId.name,
            method.methodSignature(),
            line,
            offset
        )
    }
}
