package org.utbot.instrumentation.instrumentation.execution.phases

import org.utbot.framework.plugin.api.*
import java.util.IdentityHashMap
import org.utbot.instrumentation.instrumentation.execution.constructors.InstrumentationContextAwareValueConstructor
import org.utbot.instrumentation.instrumentation.execution.context.InstrumentationContext
import org.utbot.framework.plugin.api.util.isInaccessibleViaReflection
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult

typealias ConstructedParameters = List<UtConcreteValue<*>>
typealias ConstructedStatics = Map<FieldId, UtConcreteValue<*>>
typealias ConstructedCache = IdentityHashMap<Any, UtModel>

data class ConstructedData(
    val params: ConstructedParameters,
    val statics: ConstructedStatics,
    val cache: ConstructedCache,
)

/**
 * This phase of values instantiation from given models.
 */
class ValueConstructionPhase(
    instrumentationContext: InstrumentationContext
) : ExecutionPhase {

    override fun wrapError(e: Throwable): ExecutionPhaseException = ExecutionPhaseStop(
        phase = this.javaClass.simpleName,
        result = UtConcreteExecutionResult(
            stateAfter = MissingState,
            result = when(e) {
                is TimeoutException -> UtTimeoutException(e)
                else -> UtConcreteExecutionProcessedFailure(e)
            },
            coverage = Coverage()
        )
    )

    private val constructor = InstrumentationContextAwareValueConstructor(instrumentationContext)

    fun getCache(): ConstructedCache {
        return constructor.objectToModelCache
    }

    fun constructParameters(state: EnvironmentModels): ConstructedParameters {
        val parametersModels = listOfNotNull(state.thisInstance) + state.parameters
        return constructor.constructMethodParameters(parametersModels)
    }

    fun constructStatics(state: EnvironmentModels): ConstructedStatics =
        constructor.constructStatics(
            state.statics.filterKeys { !it.isInaccessibleViaReflection }
        )

    fun mock(instrumentations: List<UtInstrumentation>) {
        mockStaticMethods(instrumentations)
        mockNewInstances(instrumentations)
    }

    private fun mockStaticMethods(instrumentations: List<UtInstrumentation>) {
        val staticMethodsInstrumentation = instrumentations.filterIsInstance<UtStaticMethodInstrumentation>()
        constructor.mockStaticMethods(staticMethodsInstrumentation)
    }

    private fun mockNewInstances(instrumentations: List<UtInstrumentation>) {
        val newInstanceInstrumentation = instrumentations.filterIsInstance<UtNewInstanceInstrumentation>()
        constructor.mockNewInstances(newInstanceInstrumentation)
    }

    fun resetMockMethods() {
        constructor.resetMockedMethods()
    }
}
