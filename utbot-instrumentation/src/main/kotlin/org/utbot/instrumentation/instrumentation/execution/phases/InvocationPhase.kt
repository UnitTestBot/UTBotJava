package org.utbot.instrumentation.instrumentation.execution.phases

import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult


/**
 * This phase is about invoking user's code using [delegateInstrumentation].
 */
class InvocationPhase(
    private val delegateInstrumentation: Instrumentation<Result<*>>
) : ExecutionPhase {

    override fun wrapError(e: Throwable): ExecutionPhaseException {
        val message = this.javaClass.simpleName
        return when(e) {
            is TimeoutException ->  ExecutionPhaseStop(message, UtConcreteExecutionResult(MissingState, MissingState, UtTimeoutException(e), Coverage()))
            else -> ExecutionPhaseError(message, e)
        }
    }


    fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        params: List<Any?>,
    ): Result<*> = delegateInstrumentation.invoke(clazz, methodSignature, params)
}