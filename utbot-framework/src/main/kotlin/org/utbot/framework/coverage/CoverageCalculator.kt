package org.utbot.framework.coverage

import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtValueExecution
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.coverage.CoverageInfo
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.util.StaticEnvironment
import kotlinx.coroutines.runBlocking

fun methodCoverage(executable: ExecutableId, executions: List<UtValueExecution<*>>, classpath: String): Coverage {
    val methodSignature = executable.signature
    val classId = executable.classId
    return ConcreteExecutor(CoverageInstrumentation, classpath).let { executor ->
        for (execution in executions) {
            val args = execution.stateBefore.params.map { it.value }.toMutableList()
            val caller = execution.stateBefore.caller
            if (caller != null) {
                args.add(0, caller.value)
            }
            val staticEnvironment = StaticEnvironment(
                execution.stateBefore.statics.map { it.key to it.value.value }
            )
            runBlocking {
                executor.executeAsync(
                    classId.name,
                    methodSignature,
                    args.toTypedArray(),
                    parameters = staticEnvironment
                )
            }
        }

        val coverage = executor.collectCoverage(classId.jClass)
        coverage.toMethodCoverage(methodSignature)
    }
}

fun CoverageInfo.toMethodCoverage(methodSignature: String): Coverage {
    val methodRange = methodToInstrRange[methodSignature]!!
    val visitedCount = visitedInstrs.count { it in methodRange }
    return Coverage(
        Counter(),
        Counter(methodRange.count(), visitedCount, methodRange.count() - visitedCount),
        Counter()
    )
}

data class Coverage(
    val branchCounter: Counter = Counter(),
    val instructionCounter: Counter = Counter(),
    val lineCounter: Counter = Counter()
) {
    override fun toString() = "(branches: $branchCounter, instructions: $instructionCounter, lines: $lineCounter)"
}

operator fun Counter.plus(other: Counter): Counter =
    Counter(
        total + other.total,
        covered + other.covered,
        missed + other.missed
    )

data class Counter(val total: Int = 0, val covered: Int = 0, val missed: Int = 0) {
    override fun toString() = "$covered/$total"
}

val Coverage.counters: List<Counter>
    get() = listOf(branchCounter, instructionCounter, lineCounter)

operator fun Coverage.compareTo(other: Coverage) = this.sumCovered().compareTo(other.sumCovered())

fun Coverage.toAtLeast(): Int =
    this.counters.minOf { if (it.total == 0) 100 else (it.covered * 100 / it.total) }

fun Coverage.sumCovered(): Int = this.counters.sumBy { it.covered }
