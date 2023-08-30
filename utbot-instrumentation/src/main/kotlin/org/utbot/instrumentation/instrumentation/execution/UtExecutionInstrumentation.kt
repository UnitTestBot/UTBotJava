package org.utbot.instrumentation.instrumentation.execution

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.mapper.UtModelMapper
import org.utbot.framework.plugin.api.mapper.mapModels
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.context.InstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.context.SimpleInstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.phases.PhasesController

/**
 * Consists of the data needed to execute the method concretely. Also includes method arguments stored in models.
 *
 * @property [stateBefore] is necessary for construction of parameters of a concrete call.
 * @property [instrumentation] is necessary for mocking static methods and new instances.
 * @property [timeout] is timeout for specific concrete execution (in milliseconds).
 * By default is initialized from [UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis]
 */
data class UtConcreteExecutionData(
    val stateBefore: EnvironmentModels,
    val instrumentation: List<UtInstrumentation>,
    val timeout: Long
)

fun UtConcreteExecutionData.mapModels(mapper: UtModelMapper) = copy(
    stateBefore = stateBefore.mapModels(mapper),
    instrumentation = instrumentation.map { it.mapModels(mapper) }
)

/**
 * [UtConcreteExecutionResult] that has not yet been populated with extra data, e.g.:
 *  - updated [UtConcreteExecutionResult.stateBefore]
 *  - [UtConcreteExecutionResult.detectedMockingCandidates]
 */
data class PreliminaryUtConcreteExecutionResult(
    val stateAfter: EnvironmentModels,
    val result: UtExecutionResult,
    val coverage: Coverage,
    val newInstrumentation: List<UtInstrumentation>? = null,
) {
    fun toCompleteUtConcreteExecutionResult(
        stateBefore: EnvironmentModels,
        detectedMockingCandidates: Set<MethodId>
    ) = UtConcreteExecutionResult(
        stateBefore = stateBefore,
        stateAfter = stateAfter,
        result = result,
        coverage = coverage,
        newInstrumentation = newInstrumentation,
        detectedMockingCandidates = detectedMockingCandidates,
    )
}

data class UtConcreteExecutionResult(
    val stateBefore: EnvironmentModels,
    val stateAfter: EnvironmentModels,
    val result: UtExecutionResult,
    val coverage: Coverage,
    val newInstrumentation: List<UtInstrumentation>? = null,
    val detectedMockingCandidates: Set<MethodId>,
) {
    override fun toString(): String = buildString {
        appendLine("UtConcreteExecutionResult(")
        appendLine("stateBefore=$stateBefore")
        appendLine("stateAfter=$stateAfter")
        appendLine("result=$result")
        appendLine("coverage=$coverage)")
    }
}

data class ResultOfInstrumentation(val instructionsIds: List<Long>?)

interface UtExecutionInstrumentation : Instrumentation<UtConcreteExecutionResult> {
    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): UtConcreteExecutionResult = invoke(
        clazz, methodSignature, arguments, parameters, phasesWrapper = { invokeBasePhases -> invokeBasePhases() }
    )

    fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?,
        phasesWrapper: PhasesController.(invokeBasePhases: () -> PreliminaryUtConcreteExecutionResult) -> PreliminaryUtConcreteExecutionResult
    ): UtConcreteExecutionResult

    fun getResultOfInstrumentation(className: String, methodSignature: String): ResultOfInstrumentation

    interface Factory<out TInstrumentation : UtExecutionInstrumentation> : Instrumentation.Factory<UtConcreteExecutionResult, TInstrumentation> {
        override fun create(): TInstrumentation = create(SimpleInstrumentationContext())

        fun create(instrumentationContext: InstrumentationContext): TInstrumentation
    }
}
