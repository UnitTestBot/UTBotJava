package org.utbot.python.evaluation.coverage

import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.Instruction
import org.utbot.python.PythonMethod
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.pythonTypeRepresentation

enum class PythonCoverageMode {
    Lines {
        override fun toString() = "lines"
    },

    Instructions {
        override fun toString() = "instructions"
    }
}

fun String.toPythonCoverageMode(): PythonCoverageMode? {
    return when (this.lowercase()) {
        "lines" -> PythonCoverageMode.Lines
        "instructions" -> PythonCoverageMode.Instructions
        else -> null
    }
}

data class PyInstruction(
    val lineNumber: Int,
    val offset: Long,
    val depth: Int,
) {
    override fun toString(): String = listOf(lineNumber, offset, depth).joinToString(":")

    val id: Long = (offset to depth.toLong()).toCoverageId()

    constructor(lineNumber: Int) : this(lineNumber, lineNumber.toLong(), 0)
    constructor(lineNumber: Int, id: Long) : this(lineNumber, id.toPair().first, id.toPair().second.toInt())
}

fun String.toPyInstruction(): PyInstruction? {
    val data = this.split(":")
    when (data.size) {
        3 -> {
            val line = data[0].toInt()
            val offset = data[1].toLong()
            val depth = data[2].toInt()
            return PyInstruction(line, offset, depth)
        }
        2 -> {
            val line = data[0].toInt()
            val offset = data[1].toLong()
            return PyInstruction(line, offset, 0)
        }
        1 -> {
            val line = data[0].toInt()
            return PyInstruction(line)
        }
        else -> return null
    }
}

fun makeInstructions(coveredInstructions: Collection<PyInstruction>, method: PythonMethod): List<Instruction> {
    return coveredInstructions.map {
        Instruction(
            method.containingPythonClass?.pythonTypeRepresentation() ?: pythonAnyClassId.name,
            method.methodSignature(),
            it.lineNumber,
            it.id
        )
    }
}

data class PyCoverage(
    val coveredInstructions: List<PyInstruction>,
    val missedInstructions: List<PyInstruction>
)

fun calculateCoverage(coverage: PyCoverage, method: PythonMethod): Coverage {
    return calculateCoverage(coverage.coveredInstructions, coverage.missedInstructions, method)
}

fun calculateCoverage(statements: List<PyInstruction>, missedStatements: List<PyInstruction>, method: PythonMethod): Coverage {
    val covered = statements.filter { it !in missedStatements }
    return Coverage(
        coveredInstructions=covered.map {
            Instruction(
                method.containingPythonClass?.pythonTypeRepresentation() ?: pythonAnyClassId.name,
                method.methodSignature(),
                it.lineNumber,
                it.id
            )
        },
        instructionsCount = (covered.size + missedStatements.size).toLong(),
        missedInstructions = missedStatements.map {
            Instruction(
                method.containingPythonClass?.pythonTypeRepresentation() ?: pythonAnyClassId.name,
                method.methodSignature(),
                it.lineNumber,
                it.id
            )
        }
    )
}

enum class CoverageOutputFormat {
    Lines,
    Instructions,
    TopFrameInstructions;
}