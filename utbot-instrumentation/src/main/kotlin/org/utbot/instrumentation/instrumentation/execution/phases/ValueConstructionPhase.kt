package org.utbot.instrumentation.instrumentation.execution.phases

import org.utbot.framework.plugin.api.*
import java.util.IdentityHashMap
import org.utbot.instrumentation.instrumentation.execution.constructors.MockValueConstructor
import org.utbot.instrumentation.instrumentation.execution.mock.InstrumentationContext
import org.utbot.framework.plugin.api.util.isInaccessibleViaReflection
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult

typealias ConstructedParameters = List<UtConcreteValue<*>>
typealias ConstructedStatics = Map<FieldId, UtConcreteValue<*>>
typealias ConstructedCache = IdentityHashMap<Any, UtModel>

/**
 * This phase of values instantiation from given models.
 */
class ValueConstructionPhase(
    instrumentationContext: InstrumentationContext
) : ExecutionPhase {

    override fun wrapError(e: Throwable): ExecutionPhaseException {
        val message = this.javaClass.simpleName
        return when(e) {
            is TimeoutException ->  ExecutionPhaseStop(message, UtConcreteExecutionResult(MissingState, UtTimeoutException(e), Coverage()))
            else -> ExecutionPhaseError(message, e)
        }
    }

    private val constructor = MockValueConstructor(instrumentationContext)

    fun getCache(): ConstructedCache {
        return constructor.objectToModelCache
    }

    fun constructParameters(state: EnvironmentModels, thisInstanceCreator: (UtModel) -> UtConcreteValue<*>?): ConstructedParameters {
        val createdThisInstance = state.thisInstance?.let { thisInstanceCreator(it) }
        val parametersModels = listOfNotNull(state.thisInstance.takeIf { createdThisInstance == null }) + state.parameters
        return listOfNotNull(createdThisInstance) + constructor.constructMethodParameters(parametersModels)
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
