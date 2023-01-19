package org.utbot.instrumentation.instrumentation.execution.data

import org.utbot.framework.plugin.api.UtIgnoreFailure
import org.utbot.instrumentation.instrumentation.execution.phases.ValueConstructionPhaseError

/**
 * Specification controls result handling and validation.
 */
interface UtConcreteExecutionSpecification {

    /**
     * If an exception is expected, it will be handling and translate to [UtIgnoreFailure]
     */
    fun exceptionIsExpected(exception: Exception): Boolean
}

/**
 * Fuzzing specification allows to ignore errors during values construction.
 */
class FuzzingSpecification : UtConcreteExecutionSpecification {
    override fun exceptionIsExpected(exception: Exception): Boolean {
        return exception is ValueConstructionPhaseError
    }
}
