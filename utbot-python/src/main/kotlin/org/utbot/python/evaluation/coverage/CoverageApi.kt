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
    };

    companion object {
        fun parse(name: String): PythonCoverageMode {
            return PythonCoverageMode.values().first {
                it.name.lowercase() == name.lowercase()
            }
        }
    }
}

data class PyInstruction(
    val lineNumber: Int,
    val offset: Long,
    val fromMainFrame: Boolean,
) {
    override fun toString(): String = listOf(lineNumber, offset, fromMainFrame).joinToString(":")

    val id: Long = (lineNumber.toLong() to offset).toCoverageId()

    constructor(lineNumber: Int) : this(lineNumber, lineNumber.toLong(), true)
    constructor(lineNumber: Int, id: Long) : this(lineNumber, id.toPair().second, true)
}

fun String.toPyInstruction(): PyInstruction? {
    val data = this.split(":")
    when (data.size) {
        3 -> {
            val line = data[0].toInt()
            val offset = data[1].toLong()
            val fromMainFrame = data[2].toInt() != 0
            return PyInstruction(line, offset, fromMainFrame)
        }
        2 -> {
            val line = data[0].toInt()
            val offset = data[1].toLong()
            return PyInstruction(line, offset, true)
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
    val covered = statements.filter { it !in missedStatements && it.fromMainFrame }
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
    Instructions;

    companion object {
        fun parse(name: String): CoverageOutputFormat {
            return CoverageOutputFormat.values().first {
                it.name.lowercase() == name.lowercase()
            }
        }
    }
}
