package org.utbot.instrumentation.instrumentation.execution

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.*
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

data class UtConcreteExecutionResult(
    val stateAfter: EnvironmentModels,
    val result: UtExecutionResult,
    val coverage: Coverage,
    val newInstrumentation: List<UtInstrumentation>? = null,
) {
    override fun toString(): String = buildString {
        appendLine("UtConcreteExecutionResult(")
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
        phasesWrapper: PhasesController.(invokeBasePhases: () -> UtConcreteExecutionResult) -> UtConcreteExecutionResult
    ): UtConcreteExecutionResult

    fun getResultOfInstrumentation(className: String, methodSignature: String): ResultOfInstrumentation

    interface Factory<out TInstrumentation : UtExecutionInstrumentation> : Instrumentation.Factory<UtConcreteExecutionResult, TInstrumentation> {
        override fun create(): TInstrumentation = create(SimpleInstrumentationContext())

        fun create(instrumentationContext: InstrumentationContext): TInstrumentation
    }
}
