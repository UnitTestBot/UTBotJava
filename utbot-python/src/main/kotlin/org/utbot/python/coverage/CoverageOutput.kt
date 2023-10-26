package org.utbot.python.coverage

sealed interface CoverageFormat {
    fun toJson(): String
}
data class LineCoverage(val start: Int, val end: Int) : CoverageFormat {
    override fun toJson(): String = "{\"start\": ${start}, \"end\": ${end}}"
}

data class InstructionCoverage(
    val line: Int,
    val offset: Long,
    val fromMainFrame: Boolean
) : CoverageFormat {
    override fun toJson(): String = "{\"line\": ${line}, \"offset\": ${offset}, \"fromMainFrame\": ${fromMainFrame}}"
}

data class CoverageInfo<T: CoverageFormat>(
    val covered: List<T>,
    val notCovered: List<T>,
)

fun getLinesList(instructions: Collection<PyInstruction>): List<LineCoverage> =
    instructions
        .map { it.lineNumber }
        .sorted()
        .fold(emptyList()) { acc, lineNumber ->
            if (acc.isEmpty())
                return@fold listOf(LineCoverage(lineNumber, lineNumber))
            val elem = acc.last()
            if (elem.end + 1 == lineNumber || elem.end == lineNumber )
                acc.dropLast(1) + listOf(LineCoverage(elem.start, lineNumber))
            else
                acc + listOf(LineCoverage(lineNumber, lineNumber))
        }

fun filterMissedLines(covered: Collection<LineCoverage>, missed: Collection<PyInstruction>): List<PyInstruction> =
    missed.filterNot { missedInstruction -> covered.any { it.start <= missedInstruction.lineNumber && missedInstruction.lineNumber <= it.end } }

fun getInstructionsList(instructions: Collection<PyInstruction>): List<CoverageFormat> =
    instructions.map { InstructionCoverage(it.lineNumber, it.offset, it.fromMainFrame) }.toSet().toList()
