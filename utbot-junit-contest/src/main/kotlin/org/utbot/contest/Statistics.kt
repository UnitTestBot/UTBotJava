package org.utbot.contest

import java.io.File
import org.utbot.common.MutableMultiset
import org.utbot.common.mutableMultisetOf
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.UtError

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

fun <T> Iterable<T>.printMultiline(printer: (T) -> Any?) = "\n" + joinToString("\n") { "${printer(it)}" } + "\n"

class GlobalStats {

    companion object {
        const val PRECISION: Int = 2
    }

    var duration: Long? = null

    val statsForClasses = mutableListOf<StatsForClass>()

    val classesForGeneration: Int
        get() = statsForClasses.size

    val testCasesGenerated: Int
        get() = statsForClasses.sumOf { it.testcasesGenerated }

    val classesWithoutProblems: Int
        get() = statsForClasses.count { !it.canceledByTimeout && it.methodsWithAtLeastOneException == 0 }

    val classesCanceledByTimeout: Int
        get() = statsForClasses.count { it.canceledByTimeout }

    val totalMethodsForGeneration: Int
        get() = statsForClasses.sumOf { it.methodsCount }

    val methodsWithAtLeastOneTestCaseGenerated: Int
        get() = statsForClasses.sumOf { it.statsForMethods.count { it.testsGeneratedCount > 0 } }

    val methodsWithExceptions: Int
        get() = statsForClasses.sumOf { clazz -> clazz.statsForMethods.count { it.failReasons.isNotEmpty() } }

    val suspiciousMethods: Int
        get() = statsForClasses.sumOf { it.statsForMethods.count { it.isSuspicious } }

    val testClassesFailedToCompile: Int
        get() = statsForClasses.count { it.failedToCompile }

    val coveredInstructions: Int
        get() = statsForClasses.sumOf { it.coverage.getCoverageInfo(it.className).covered }

    val coveredInstructionsByFuzzing: Int
        get() = statsForClasses.sumOf { it.fuzzedCoverage.getCoverageInfo(it.className).covered }

    val coveredInstructionsByConcolic: Int
        get() = statsForClasses.sumOf { it.concolicCoverage.getCoverageInfo(it.className).covered }

    val totalInstructions: Int
        get() = statsForClasses.sumOf { it.coverage.totalInstructions.toInt() }

    val avgCoverage: Double
        get() = statsForClasses
            .filter { it.coverage.totalInstructions != 0L }
            .map { it.coverage.getCoverageInfo(it.className).run { 100.0 * covered / total } }
            .average().run {
                if (isNaN()) 0.0
                else this
            }

    override fun toString(): String = "\n<Global statistics> :" +
            "\n\t#classes for generation = $classesForGeneration" +
            "\n\t#tc generated = $testCasesGenerated" +
            "\n\t#classes without problems = $classesWithoutProblems" +
            "\n\t#classes canceled by timeout = $classesCanceledByTimeout" +
            "\n----------------------------------------" +
            "\n\t#total methods for generation = $totalMethodsForGeneration" +
            "\n\t#methods with at least one testcase generated = $methodsWithAtLeastOneTestCaseGenerated" +
            "\n\t#methods with exceptions = $methodsWithExceptions" +
            "\n\t#suspicious methods WITH NO testcases AND NO exceptions = $suspiciousMethods" +
            "\n----------------------------------------" +
            "\n\t#Test classes failed to compile = $testClassesFailedToCompile out of $classesForGeneration:" +
            statsForClasses.filter { it.failedToCompile }.printMultiline { "\t >" + it.testClassFile?.name } +
            "\n----------------------------------------" +
            "\n\tMost common fail reasons in symbolic execution: \n\t\t" + // for each exception with count number of methods it was encountered (not TC!)
            statsForClasses.flatMap { it.statsForMethods }
                .flatMap { methodStats -> methodStats.failReasons.map { it to methodStats.methodName } } //all fail reasons in list
                .groupBy { it.first }.mapValues { it.value.map { it.second } }.entries
                .sortedByDescending { (_, names) -> names.size }
                .take(10)
                .printMultiline { (reason, names) -> " ${names.joinToString()}\n-->> In ${names.size} method(s) :: $reason" } +
            "\n----------------------------------------" +
            totalInstructions.let { denum ->
                "\n\tTotal coverage: \n\t\t" +
                coveredInstructions.let { num ->
                    "$num/$denum (${(100.0 * num / denum).format(PRECISION)} %)"
                } +
                "\n\tTotal fuzzed coverage: \n\t\t" +
                coveredInstructionsByFuzzing.let { num ->
                    "$num/$denum (${(100.0 * num / denum).format(PRECISION)} %)"
                } +
                "\n\tTotal concolic coverage: \n\t\t" +
                coveredInstructionsByConcolic.let { num ->
                    "$num/$denum (${(100.0 * num / denum).format(PRECISION)} %)"
                }
            } +
            "\n\tAvg coverage: \n\t\t" +
            avgCoverage.format(PRECISION) + " %"
}

class StatsForClass(val className: String) {
    var methodsCount: Int = -1
    val statsForMethods = mutableListOf<StatsForMethod>()

    var failedToCompile = false
    var canceledByTimeout = false
    var testClassFile: File? = null

    val methodsWithAtLeastOneException: Int get() = statsForMethods.count { it.failReasons.isNotEmpty() }
    val testcasesGenerated: Int get() = statsForMethods.sumOf { it.testsGeneratedCount }

    var coverage = CoverageInstructionsSet()
    var fuzzedCoverage = CoverageInstructionsSet()
    var concolicCoverage = CoverageInstructionsSet()

    private fun CoverageInstructionsSet.prettyInfo(): String =
        getCoverageInfo(className).run { "$covered/$total" }

    override fun toString(): String = "\n<StatsForClass> :" +
            "\n\tcanceled by timeout = $canceledByTimeout" +
            "\n\t#methods = $methodsCount, " +
            "\n\t#methods started symbolic exploration = ${statsForMethods.size}" +
            "\n\t#methods with at least one TC = ${statsForMethods.count { it.testsGeneratedCount > 0 }}" +
            "\n\t#methods with exceptions = $methodsWithAtLeastOneException" +
            "\n\t#generated TC = $testcasesGenerated" +
            "\n\t#total coverage = ${coverage.prettyInfo()}" +
            "\n\t#fuzzed coverage = ${fuzzedCoverage.prettyInfo()}" +
            "\n\t#concolic coverage = ${concolicCoverage.prettyInfo()}"
}


class StatsForMethod(val methodName: String) {
    var testsGeneratedCount = 0

    val failReasons: MutableMultiset<FailReason> = mutableMultisetOf()

    //generated no TC, nor exception
    val isSuspicious: Boolean get() = failReasons.isEmpty() && testsGeneratedCount == 0


    override fun toString(): String = "\n<StatsForMethod> :" + (if (isSuspicious) " SUSPICIOUS" else "") +
            "\n\t#generatedTC=$testsGeneratedCount\n\t" +
            (if (failReasons.isEmpty()) "WITH NO EXCEPTIONS"
            else "FAILED ${failReasons.sumOfMultiplicities} time(s) with ${failReasons.size} different exception(s)\"")

}

//equality by stacktrace
class FailReason(private val throwable: Throwable) {

    //optimization
    private val stackTrace: Array<StackTraceElement> = throwable.stackTrace

    constructor(error: UtError) : this(error.error)


    override fun toString(): String {
        return throwable.stackTraceToString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FailReason

        return stackTrace.contentEquals(other.stackTrace)
    }

    override fun hashCode(): Int {
        return stackTrace.contentHashCode()
    }

}

data class CoverageInstructionsSet(
    val coveredInstructions: MutableSet<Instruction> = mutableSetOf(),
    var totalInstructions: Long = 0
)

data class CoverageStatistic(val covered: Int, val total: Int)

/**
 * Compute coverage of class named [className] with its anonymous, nested and inner classes.
 */
private fun CoverageInstructionsSet?.getCoverageInfo(className: String): CoverageStatistic = this?.run {
    CoverageStatistic(
        coveredInstructions.filter {
            instr -> instr.className.startsWith(className)
        }.toSet().size,
        totalInstructions.toInt()
    )
} ?: CoverageStatistic(covered = 0, total = 0)
