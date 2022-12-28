package org.utbot.instrumentation.instrumentation.execution.phases

import java.io.Closeable
import java.util.IdentityHashMap
import org.utbot.instrumentation.instrumentation.execution.constructors.MockValueConstructor
import org.utbot.instrumentation.instrumentation.execution.mock.InstrumentationContext
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.util.isInaccessibleViaReflection

class ValueConstructionPhaseError(cause: Throwable) : PhaseError(
    message = "Error during phase of constructing values from models",
    cause
)

/**
 * This phase of values instantiation from given models.
 */
class ValueConstructionContext(
    instrumentationContext: InstrumentationContext,
    generateNullOnError: Boolean = false
) : PhaseContext<ValueConstructionPhaseError>, Closeable {

    override fun wrapError(error: Throwable): ValueConstructionPhaseError =
        ValueConstructionPhaseError(error)

    private val constructor = MockValueConstructor(instrumentationContext, generateNullOnError)

    fun getCache(): IdentityHashMap<Any, UtModel> {
        return constructor.objectToModelCache
    }

    fun constructParameters(state: EnvironmentModels): List<UtConcreteValue<*>> {
        val parametersModels = listOfNotNull(state.thisInstance) + state.parameters
        return constructor.constructMethodParameters(parametersModels)
    }

    fun constructStatics(state: EnvironmentModels): Map<FieldId, UtConcreteValue<*>> =
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

    override fun close() {
        constructor.close()
    }

}
