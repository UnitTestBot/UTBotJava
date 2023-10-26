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
    override val lineNumber: Int,
    val offset: Long,
    val fromMainFrame: Boolean,
) : Instruction(
    "",
    "",
    lineNumber,
    (lineNumber.toLong() to offset).toCoverageId() * 2 + fromMainFrame.toLong()) {
    override fun toString(): String = listOf(lineNumber, offset, fromMainFrame).joinToString(":")

    constructor(lineNumber: Int) : this(lineNumber, lineNumber.toLong(), true)
    constructor(lineNumber: Int, id: Long) : this(lineNumber, id.floorDiv(2).toPair().second, id % 2 == 1L)
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
    return Coverage(
        coveredInstructions = coveredStatements,
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
