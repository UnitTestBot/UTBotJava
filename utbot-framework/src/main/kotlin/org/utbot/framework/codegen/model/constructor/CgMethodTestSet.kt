package org.utbot.framework.codegen.model.constructor

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.UtFuzzedExecution
import soot.jimple.JimpleBody

data class CgMethodTestSet constructor(
    val executableId: ExecutableId,
    val jimpleBody: JimpleBody? = null,
    val errors: Map<String, Int> = emptyMap(),
    val clustersInfo: List<Pair<UtClusterInfo?, IntRange>>,
) {
    var executions: List<UtExecution> = emptyList()
        private set

    constructor(from: UtMethodTestSet) : this(
        from.method,
        from.jimpleBody,
        from.errors,
        from.clustersInfo
    ) {
        executions = from.executions
    }
    /**
     * For JavaScript purposes.
     */
    constructor(
        executableId: ExecutableId,
        execs: List<UtExecution> = emptyList(),
        errors: Map<String, Int> = emptyMap()
    ) : this(
        executableId,
        null,
        errors,
        listOf(null to execs.indices)
    ) {
        executions = execs
    }

    constructor(
        executableId: ExecutableId,
        executions: List<UtExecution> = emptyList(),
    ) : this(
        executableId,
        null,
        emptyMap(),

        listOf(null to executions.indices)
    ) {
        this.executions = executions
    }

    fun prepareTestSetsForParameterizedTestGeneration(): List<CgMethodTestSet> {
        val testSetList = mutableListOf<CgMethodTestSet>()

        // Mocks are not supported in parametrized tests, so we exclude them
        val testSetWithoutMocking = this.excludeExecutionsWithMocking()
        for (splitByExecutionTestSet in testSetWithoutMocking.splitExecutionsByResult()) {
            for (splitByChangedStaticsTestSet in splitByExecutionTestSet.splitExecutionsByChangedStatics()) {
                testSetList += splitByChangedStaticsTestSet
            }
        }

        return testSetList
    }

    /**
     * Finds a [ClassId] of all result models in executions.
     *
     * Tries to find a unique result type in testSets or
     * gets executable return type.
     */
    fun resultType(): ClassId {
        return when (executableId.returnType) {
            voidClassId -> executableId.returnType
            else -> {
                val successfulExecutions = executions.filter { it.result is UtExecutionSuccess }
                if (successfulExecutions.isNotEmpty()) {
                    successfulExecutions
                        .map { (it.result as UtExecutionSuccess).model.classId }
                        .distinct()
                        .singleOrNull()
                        ?: executableId.returnType
                } else {
                    executableId.returnType
                }
            }
        }
    }

    /**
     * Splits [CgMethodTestSet] into separate test sets having
     * unique result model [ClassId] in each subset.
     */
    private fun splitExecutionsByResult() : List<CgMethodTestSet> {
        val successfulExecutions = executions.filter { it.result is UtExecutionSuccess }
        val failureExecutions = executions.filter { it.result is UtExecutionFailure }

        val executionsByResult: MutableMap<ClassId, List<UtExecution>> =
            successfulExecutions
                .groupBy { (it.result as UtExecutionSuccess).model.classId }.toMutableMap()

        // if we have failure executions, we add them to the first successful executions group
        val groupClassId = executionsByResult.keys.firstOrNull()
        if (groupClassId != null) {
            executionsByResult[groupClassId] = executionsByResult[groupClassId]!! + failureExecutions
        } else {
            executionsByResult[objectClassId] = failureExecutions
        }

        return executionsByResult.map{ (_, executions) -> substituteExecutions(executions) }
    }

    /**
     * Splits [CgMethodTestSet] test sets by affected static fields statics.
     *
     * A separate test set is created for each combination of modified statics.
     */
    private fun splitExecutionsByChangedStatics(): List<CgMethodTestSet> {
        val executionsByStaticsUsage = executions.groupBy { it.stateBefore.statics.keys }

        val executionsByStaticsUsageAndTheirTypes = executionsByStaticsUsage
            .flatMap { (_, executions) ->
                executions.groupBy { it.stateBefore.statics.values.map { model -> model.classId } }.values
            }

        return executionsByStaticsUsageAndTheirTypes.map { executions -> substituteExecutions(executions) }
    }

    /**
     * Excludes [UtFuzzedExecution] and [UtSymbolicExecution] with mocking from [CgMethodTestSet].
     *
     * It is used in parameterized test generation.
     * We exclude them because we cannot track force mocking occurrences in fuzzing process
     * and cannot deal with mocking in parameterized mode properly.
     */
    private fun excludeExecutionsWithMocking(): CgMethodTestSet {
        val symbolicExecutionsWithoutMocking = executions
            .filterIsInstance<UtSymbolicExecution>()
            .filter { !it.containsMocking }

        return substituteExecutions(symbolicExecutionsWithoutMocking)
    }

    private fun substituteExecutions(newExecutions: List<UtExecution>): CgMethodTestSet =
        copy().apply { executions = newExecutions }
}