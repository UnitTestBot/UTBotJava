package org.utbot.framework.minimization

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.*
import org.utbot.framework.util.calculateSize
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor


/**
 * Minimizes [executions] in each test suite independently. Test suite is computed with [executionToTestSuite] function.
 *
 * We have 4 different test suites:
 * * Regression suite
 * * Error suite (invocations in which implicitly thrown unchecked exceptions reached to the top)
 * * Crash suite (invocations in which the instrumented process crashed or unexpected exception in our code occurred)
 * * Artificial error suite (invocations in which some custom exception like overflow detection occurred)
 * * Timeout suite
 *
 * We want to minimize tests independently in each of these suites.
 *
 * @return flatten minimized executions in each test suite.
 */
fun <T : Any> minimizeTestCase(
    executions: List<UtExecution>,
    executionToTestSuite: (ex: UtExecution) -> T
): List<UtExecution> {
    val groupedExecutionsByTestSuite = groupExecutionsByTestSuite(executions, executionToTestSuite)
    val groupedExecutionsByBranchInstructions = groupedExecutionsByTestSuite.flatMap { execution ->
        groupByBranchInstructions(
            execution,
            UtSettings.numberOfBranchInstructionsForClustering
        )
    }
    return groupedExecutionsByBranchInstructions.map { minimizeExecutions(it) }.flatten()
}

fun minimizeExecutions(executions: List<UtExecution>): List<UtExecution> {
    val unknownCoverageExecutions =
        executions
            .filter { it.coverage?.coveredInstructions.isNullOrEmpty() }
            .groupBy {
                it.result.javaClass to (
                        (it.result as? UtExecutionSuccess)?.model ?: (it.result as? UtExecutionFailure)?.exception
                )?.javaClass
            }
            .values
            .flatMap { executionsGroup ->
                val executionToSize = executionsGroup.associateWith { it.stateBefore.calculateSize() }
                executionsGroup
                    .sortedBy { executionToSize[it] }
                    .take(UtSettings.maxUnknownCoverageExecutionsPerMethodPerResultType)
            }
            .toSet()
    // ^^^ here we add executions with empty or null coverage

    val filteredExecutions = filterOutDuplicateCoverages(executions - unknownCoverageExecutions)
    val (mapping, executionToPriorityMapping) = buildMapping(filteredExecutions)

    val usedFilteredExecutionIndexes = GreedyEssential.minimize(mapping, executionToPriorityMapping).toSet()
    val usedFilteredExecutions = filteredExecutions.filterIndexed { idx, _ -> idx in usedFilteredExecutionIndexes }

    val usedMinimizedExecutions = usedFilteredExecutions + unknownCoverageExecutions

    return if (UtSettings.minimizeCrashExecutions) {
        usedMinimizedExecutions.filteredCrashExecutions()
    } else {
        usedMinimizedExecutions
    }
}

private fun filterOutDuplicateCoverages(executions: List<UtExecution>): List<UtExecution> {
    val (executionIdxToCoveredEdgesMap, _) = buildMapping(executions)
    return executions
        .withIndex()
        // we need to group by coveredEdges and not just Coverage to not miss exceptional edges that buildMapping() function adds
        .groupBy(
            keySelector = { indexedExecution -> executionIdxToCoveredEdgesMap[indexedExecution.index] },
            valueTransform = { indexedExecution -> indexedExecution.value }
        ).values
        .map { executionsWithEqualCoverage -> executionsWithEqualCoverage.chooseOneExecution() }
}

/**
 * Groups the [executions] by their `paths` on `first` [branchInstructionsNumber] `branch` instructions.
 *
 * An instruction is called as a `branch` instruction iff there are two or more possible next instructions after this
 * instructions. For example, if we have two test cases with these instructions list:
 * * `{1, 2, 3, 2, 4, 5, 6}`
 * * `{1, 2, 4, 5, 7}`
 *
 * Then `2` and `5` instuctions are branch instructions, because we can go either `2` -> `3` or `2` -> `5`. Similarly,
 * we can go either `5` -> `6` or `5` -> `7`.
 *
 * `First` means that we will find first [branchInstructionsNumber] `branch` instructions in the order of the appearing
 * in the instruction list of a certain execution.
 *
 * A `path` on a `branch` instruction is a concrete next instruction, which we have chosen.
 */

private fun groupByBranchInstructions(
    executions: List<UtExecution>,
    branchInstructionsNumber: Int
): Collection<List<UtExecution>> {
    val instructionToPossibleNextInstructions = mutableMapOf<Long, MutableSet<Long?>>()

    for (execution in executions) {
        execution.coverage?.let { coverage ->
            val coveredInstructionIds = coverage.coveredInstructions.map { it.id }
            for (i in coveredInstructionIds.indices) {
                instructionToPossibleNextInstructions
                    .getOrPut(coveredInstructionIds[i]) { mutableSetOf() }
                    .add(coveredInstructionIds.getOrNull(i + 1))
            }
        }
    }

    val branchInstructions = instructionToPossibleNextInstructions
        .filterValues { it.size > 1 } // here we take only real branch instruction.
        .keys

    /**
     * here we group executions by their behaviour on the branch instructions
     * e.g., we have these executions and [branchInstructionsNumber] == 2:
     * 1. {2, 3, 2, 4, 2, 5}
     * 2. {2, 3, 2, 6}
     * 3. {2, 3, 4, 3}
     * branch instructions are {2 -> (3, 4, 5, 6), 3 -> (2, 4), 4 -> (2, 3)}
     *
     * we will build these lists representing their behaviour:
     * 1. {2 -> 3, 3 -> 2} (because of {__2__, __3__, 2, 4, 2, 5})
     * 2. {2 -> 3, 3 -> 2} (because of {__2__, __3__, 2, 6})
     * 3. {2 -> 3, 3 -> 4} (because of {__2__, __3__, 4, 3})
     */
    val groupedExecutions = executions.groupBy { execution ->
        execution.coverage?.let { coverage ->
            val branchInstructionToBranch = mutableListOf<Pair<Long, Long>>() // we group executions by this variable
            val coveredInstructionIds = coverage.coveredInstructions.map { it.id }
            // collect the behaviour on the branch instructions
            for (i in 0 until coveredInstructionIds.size - 1) {
                if (coveredInstructionIds[i] in branchInstructions) {
                    branchInstructionToBranch.add(coveredInstructionIds[i] to coveredInstructionIds[i + 1])
                }
                if (branchInstructionToBranch.size == branchInstructionsNumber) {
                    break
                }
            }
            branchInstructionToBranch
        }
    }

    return groupedExecutions.values
}

private fun <T : Any> groupExecutionsByTestSuite(
    executions: List<UtExecution>,
    executionToTestSuite: (UtExecution) -> T
): Collection<List<UtExecution>> =
    executions.groupBy { executionToTestSuite(it) }.values

/**
 * Builds a mapping from execution id to edges id and from execution id to its priority.
 */
private fun buildMapping(executions: List<UtExecution>): Pair<Map<Int, List<Int>>, Map<Int, Int>> {
    // (inst1, instr2) -> edge id --- edge represents as a pair of instructions, which are connected by this edge
    val allCoveredEdges = mutableMapOf<Pair<Long, Long?>, Int>()
    val thrownExceptions = mutableMapOf<String, Long>()
    val mapping = mutableMapOf<Int, List<Int>>()
    val executionToPriorityMapping = mutableMapOf<Int, Int>()


    executions.forEachIndexed { idx, execution ->
        execution.coverage?.let { coverage ->
            val instructionsWithoutExtra = coverage.coveredInstructions.map { it.id }
            addExtraIfLastInstructionIsException( // here we add one more instruction to represent an exception.
                instructionsWithoutExtra,
                execution.result,
                thrownExceptions
            ).let { instructions ->
                val edges = instructions.indices.map { i ->
                    allCoveredEdges.getOrPut(instructions[i] to instructions.getOrNull(i + 1)) { allCoveredEdges.size }
                }

                mapping[idx] = edges
                executionToPriorityMapping[idx] = execution.getExecutionPriority()
            }
        }
    }

    return Pair(mapping, executionToPriorityMapping)
}

/**
 * For crash executions we want to keep executions with minimal statements. We assume that method under test contains
 * no more than one crash, and we want to achieve it with minimal statements. The possible way is choosing an execution
 * with minimal model, so here we are keeping non crash executions and trying to find minimal crash execution.
 */
private fun List<UtExecution>.filteredCrashExecutions(): List<UtExecution> {
    val crashExecutions = filter { it.result is UtConcreteExecutionFailure }.ifEmpty {
        return this
    }

    val notCrashExecutions = filterNot { it.result is UtConcreteExecutionFailure }

    return notCrashExecutions + crashExecutions.chooseOneExecution()
}

/**
 * Chooses one execution with the highest [execution priority][getExecutionPriority]. If multiple executions
 * have the same priority, then the one with the [smallest][calculateSize] [UtExecution.stateBefore] is chosen.
 *
 * Only [UtExecution.stateBefore] is considered, because [UtExecution.result] and [UtExecution.stateAfter]
 * don't represent true picture as they are limited by [construction depth][UtModelConstructor.maxDepth] and their
 * sizes can't be calculated for crushed executions.
 */
private fun List<UtExecution>.chooseOneExecution(): UtExecution = minWithOrNull(
    compareBy({ it.getExecutionPriority() }, { it.stateBefore.calculateSize() })
) ?: error("Cannot find minimal execution within empty executions")

/**
 * Extends the [instructionsWithoutExtra] with one extra instruction if the [result] is
 * [UtExecutionFailure].
 *
 * Also adds this exception to the [thrownExceptions] if it is not already there.
 *
 * @return the extended list of instructions or initial list if [result] is not [UtExecutionFailure].
 */
private fun addExtraIfLastInstructionIsException(
    instructionsWithoutExtra: List<Long>,
    result: UtExecutionResult,
    thrownExceptions: MutableMap<String, Long>
): List<Long> =
    if (result is UtExecutionFailure) {
        val exceptionInfo = result.exception.exceptionToInfo()
        thrownExceptions.putIfAbsent(exceptionInfo, (-thrownExceptions.size - 1).toLong())
        val exceptionId = thrownExceptions.getValue(exceptionInfo)
        instructionsWithoutExtra.toMutableList().apply { add(exceptionId) }
    } else {
        instructionsWithoutExtra
    }

/**
 * Takes an exception name, a class name, a method signature and a line number from exception.
 */
private fun Throwable.exceptionToInfo(): String =
    this::class.java.name + (this.stackTrace.firstOrNull()?.run { className + methodName + lineNumber } ?: "null")

/**
 * Returns an execution priority. [UtSymbolicExecution] has the highest priority
 * over other executions like [UtFuzzedExecution], [UtFailedExecution], etc.
 *
 * NOTE! Smaller number represents higher priority.
 *
 * See [https://github.com/UnitTestBot/UTBotJava/issues/1504] for more details.
 */
private fun UtExecution.getExecutionPriority(): Int = when (this) {
    is UtSymbolicExecution -> 0
    else -> 1
}
