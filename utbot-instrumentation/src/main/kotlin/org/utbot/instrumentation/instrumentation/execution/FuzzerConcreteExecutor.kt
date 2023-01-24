package org.utbot.framework.concrete

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionData
import org.utbot.instrumentation.instrumentation.execution.UtFuzzingConcreteExecutionResult

class FuzzerConcreteExecutor(
    private val pathsToUserClasses: String,
) {

    private val concreteExecutor =
        if (UtSettings.greyBoxFuzzingCompetitionMode) {
            ConcreteExecutor(
                UtFuzzingExecutionInstrumentation,
                pathsToUserClasses,
            ).apply { this.classLoader = utContext.classLoader }
        } else {
            ConcreteExecutor(
                UtFuzzingExecutionInstrumentationWithStateAfterCollection,
                pathsToUserClasses,
            ).apply { this.classLoader = utContext.classLoader }
        }

    suspend fun execute(
        methodUnderTest: ExecutableId,
        stateBefore: EnvironmentModels,
        instrumentation: List<UtInstrumentation>
    ): UtFuzzingConcreteExecutionResult {
        return if (UtSettings.greyBoxFuzzingCompetitionMode) {
            val executor = concreteExecutor as ConcreteExecutor<UtFuzzingConcreteExecutionResult, UtFuzzingExecutionInstrumentation>
            val executionResult = executor.executeConcretelyFuzz(methodUnderTest, stateBefore, instrumentation)
            UtFuzzingConcreteExecutionResult(
                null,
                executionResult.result,
                executionResult.coverage,
                executionResult.methodInstructions
            )
        } else {
            val executor = concreteExecutor as ConcreteExecutor<UtFuzzingConcreteExecutionResult, UtFuzzingExecutionInstrumentationWithStateAfterCollection>
            val executionResult = executor.executeConcretelyFuzzWithStateAfterCollection(
                methodUnderTest,
                stateBefore,
                instrumentation
            )
            UtFuzzingConcreteExecutionResult(
                executionResult.stateAfter,
                executionResult.result,
                executionResult.coverage,
                executionResult.methodInstructions
            )
        }
    }

    private suspend fun ConcreteExecutor<UtFuzzingConcreteExecutionResult, UtFuzzingExecutionInstrumentation>.executeConcretelyFuzz(
        methodUnderTest: ExecutableId,
        stateBefore: EnvironmentModels,
        instrumentation: List<UtInstrumentation>
    ): UtFuzzingConcreteExecutionResult = executeAsync(
        methodUnderTest.classId.name,
        methodUnderTest.signature,
        arrayOf(),
        parameters = UtConcreteExecutionData(stateBefore, instrumentation)
    )

    private suspend fun ConcreteExecutor<UtFuzzingConcreteExecutionResult, UtFuzzingExecutionInstrumentationWithStateAfterCollection>.executeConcretelyFuzzWithStateAfterCollection(
        methodUnderTest: ExecutableId,
        stateBefore: EnvironmentModels,
        instrumentation: List<UtInstrumentation>
    ): UtFuzzingConcreteExecutionResult = executeAsync(
        methodUnderTest.classId.name,
        methodUnderTest.signature,
        arrayOf(),
        parameters = UtConcreteExecutionData(stateBefore, instrumentation)
    )

}