package org.utbot.framework.minimization

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtConcreteExecutionFailure
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.UtVoidModel


/**
 * Minimizes [executions] in each test suite independently. Test suite is computed with [executionToTestSuite] function.
 *
 * We have 4 different test suites:
 * * Regression suite
 * * Error suite (invocations in which implicitly thrown unchecked exceptions reached to the top)
 * * Crash suite (invocations in which the child process crashed or unexpected exception in our code occurred)
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
        executions.indices.filter { executions[it].coverage?.coveredInstructions?.isEmpty() ?: true }.toSet()
    // ^^^ here we add executions with empty or null coverage, because it happens only if a concrete execution failed,
    //     so we don't know the actual coverage for such executions
    val mapping = buildMapping(executions.filterIndexed { idx, _ -> idx !in unknownCoverageExecutions })
    val usedExecutionIndexes = (GreedyEssential.minimize(mapping) + unknownCoverageExecutions).toSet()

    val usedMinimizedExecutions = executions.filterIndexed { idx, _ -> idx in usedExecutionIndexes }

    return if (UtSettings.minimizeCrashExecutions) {
        usedMinimizedExecutions.filteredCrashExecutions()
    } else {
        usedMinimizedExecutions
    }
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
    val instructionToPossibleNextInstructions = mutableMapOf<Long, MutableSet<Long>>()

    for (execution in executions) {
        execution.coverage?.let { coverage ->
            val coveredInstructionIds = coverage.coveredInstructions.map { it.id }
            for (i in 0 until coveredInstructionIds.size - 1) {
                instructionToPossibleNextInstructions
                    .getOrPut(coveredInstructionIds[i]) { mutableSetOf() }
                    .add(coveredInstructionIds[i + 1])
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
 * Builds a mapping from execution id to edges id.
 */
private fun buildMapping(executions: List<UtExecution>): Map<Int, List<Int>> {
    // (inst1, instr2) -> edge id --- edge represents as a pair of instructions, which are connected by this edge
    val allCoveredEdges = mutableMapOf<Pair<Long, Long>, Int>()
    val thrownExceptions = mutableMapOf<String, Long>()
    val mapping = mutableMapOf<Int, List<Int>>()


    executions.forEachIndexed { idx, execution ->
        execution.coverage?.let { coverage ->
            val instructionsWithoutExtra = coverage.coveredInstructions.map { it.id }
            addExtraIfLastInstructionIsException( // here we add one more instruction to represent an exception.
                instructionsWithoutExtra,
                execution.result,
                thrownExceptions
            ).let { instructions ->
                for (i in 0 until instructions.size - 1) {
                    allCoveredEdges.putIfAbsent(instructions[i] to instructions[i + 1], allCoveredEdges.size)
                }

                val edges = mutableListOf<Int>()
                for (i in 0 until instructions.size - 1) {
                    edges += allCoveredEdges[instructions[i] to instructions[i + 1]]!!
                }
                mapping[idx] = edges
            }
        }
    }

    return mapping
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

    return notCrashExecutions + crashExecutions.chooseMinimalCrashExecution()
}

/**
 * As for now crash execution can only be produced by Concrete Executor, it does not have [UtExecution.stateAfter] and
 * [UtExecution.result] is [UtExecutionFailure], so we check only [UtExecution.stateBefore].
 */
private fun List<UtExecution>.chooseMinimalCrashExecution(): UtExecution = minByOrNull {
   it.stateBefore.calculateSize()
} ?: error("Cannot find minimal crash execution within empty executions")

private fun EnvironmentModels.calculateSize(): Int {
    val thisInstanceSize = thisInstance?.calculateSize() ?: 0
    val parametersSize = parameters.sumOf { it.calculateSize() }
    val staticsSize = statics.values.sumOf { it.calculateSize() }

    return thisInstanceSize + parametersSize + staticsSize
}

/**
 * We assume that "size" for "common" models is 1, 0 for [UtVoidModel] (as they do not return anything) and
 * [UtPrimitiveModel] and [UtNullModel] (we use them as literals in codegen), summarising for all statements for [UtAssembleModel] and
 * summarising for all fields and mocks for [UtCompositeModel]. As [UtCompositeModel] could be recursive, we need to
 * store it in [used]. Moreover, if we already calculate size for [this], it means that we will use already created
 * variable by this model and do not need to create it again, so size should be equal to 0.
 */
private fun UtModel.calculateSize(used: MutableSet<UtModel> = mutableSetOf()): Int {
    if (this in used) return 0

    used += this

    return when (this) {
        is UtNullModel, is UtPrimitiveModel, UtVoidModel -> 0
        is UtClassRefModel, is UtEnumConstantModel, is UtArrayModel -> 1
        is UtAssembleModel -> {
            1 + instantiationCall.calculateSize(used) + modificationsChain.sumOf { it.calculateSize(used) }
        }
        is UtCompositeModel -> 1 + fields.values.sumOf { it.calculateSize(used) }
        is UtLambdaModel -> 1 + capturedValues.sumOf { it.calculateSize(used) }
    }
}

private fun UtStatementModel.calculateSize(used: MutableSet<UtModel> = mutableSetOf()): Int =
    when (this) {
        is UtDirectSetFieldModel -> 1 + fieldModel.calculateSize(used)
        is UtExecutableCallModel -> 1 + params.sumOf { it.calculateSize(used) }
    }

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