package org.utbot.framework.concrete

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.greyboxfuzzer.util.UtFuzzingConcreteExecutionResult
import org.utbot.instrumentation.ConcreteExecutor

class FuzzerConcreteExecutor(
    private val pathsToUserClasses: String,
    private val pathsToDependencyClasses: String,
) {

    suspend fun execute(
        methodUnderTest: ExecutableId,
        stateBefore: EnvironmentModels,
        instrumentation: List<UtInstrumentation>
    ): UtFuzzingConcreteExecutionResult {
        return if (UtSettings.greyBoxFuzzingCompetitionMode) {
            val fuzzingExecutor =
                ConcreteExecutor(
                    UtFuzzingExecutionInstrumentation,
                    pathsToUserClasses,
                    pathsToDependencyClasses
                ).apply { this.classLoader = utContext.classLoader }
            val executionResult = fuzzingExecutor.executeConcretelyFuzz(methodUnderTest, stateBefore, instrumentation)
            UtFuzzingConcreteExecutionResult(
                null,
                executionResult.result,
                executionResult.coverage,
                executionResult.methodInstructions
            )
        } else {
            val fuzzingExecutor =
                ConcreteExecutor(
                    UtFuzzingExecutionInstrumentationWithStateAfterCollection,
                    pathsToUserClasses,
                    pathsToDependencyClasses
                ).apply { this.classLoader = utContext.classLoader }
            val executionResult = fuzzingExecutor.executeConcretelyFuzzWithStateAfterCollection(
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