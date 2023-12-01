package org.utbot.python.coverage

import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.Instruction

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
    val pyLineNumber: Int,
    val offset: Long,
    val fromMainFrame: Boolean,
) : Instruction(
    "",
    "",
    pyLineNumber,
    (pyLineNumber.toLong() to offset).toCoverageId() * 2 + fromMainFrame.toLong()) {
    override fun toString(): String = listOf(lineNumber, offset, fromMainFrame).joinToString(":")

    constructor(lineNumber: Int) : this(lineNumber, lineNumber.toLong(), true)
    constructor(lineNumber: Int, id: Long) : this(lineNumber, id.floorDiv(2).toPair().second, id % 2 == 1L)
}

data class PyInstructionEdge(
    val instruction1: PyInstruction,
    val instruction2: PyInstruction,
) : Instruction(
    "",
    "",
    instruction2.pyLineNumber,
    (instruction1.id to instruction2.id).toCoverageId()
) {
    override fun toString(): String = "$instruction1 -> $instruction2"
}

fun Boolean.toLong() = if (this) 1L else 0L

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

fun buildCoverage(coveredStatements: List<PyInstruction>, missedStatements: List<PyInstruction>): Coverage {
    return buildEdgeCoverage(coveredStatements, missedStatements)
//    return Coverage(
//        coveredInstructions = coveredStatements,
//        instructionsCount = (coveredStatements.size + missedStatements.size).toLong(),
//        missedInstructions = missedStatements
//    )
}

fun buildEdgeCoverage(coveredStatements: List<PyInstruction>, missedStatements: List<PyInstruction>): Coverage {
    return Coverage(
        coveredInstructions = coveredStatements.windowed(2, 1).map { PyInstructionEdge(it[0], it[1]) },
        instructionsCount = (coveredStatements.size + missedStatements.size).toLong(),
        missedInstructions = missedStatements
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
