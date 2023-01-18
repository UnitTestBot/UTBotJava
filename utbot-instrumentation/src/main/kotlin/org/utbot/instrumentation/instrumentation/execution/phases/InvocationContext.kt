package org.utbot.instrumentation.instrumentation.execution.phases

import org.utbot.common.StopWatch
import org.utbot.common.ThreadBasedExecutor
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.instrumentation.instrumentation.Instrumentation

class InvocationPhaseError(cause: Throwable) : PhaseError(
    message = "Error during user's code invocation phase",
    cause
)

/**
 * This phase is about invoking user's code using [delegateInstrumentation].
 */
class InvocationContext(
    private val delegateInstrumentation: Instrumentation<Result<*>>
) : PhaseContext<InvocationPhaseError> {

    override fun wrapError(error: Throwable): InvocationPhaseError =
        InvocationPhaseError(error)

    fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        params: List<Any?>,
        timeout: Long,
    ): Result<*> {
        val stopWatch = StopWatch()
        val context = UtContext(utContext.classLoader, stopWatch)
        val concreteResult = ThreadBasedExecutor.threadLocal.invokeWithTimeout(timeout, stopWatch) {
            withUtContext(context) {
                delegateInstrumentation.invoke(clazz, methodSignature, params)
            }
        }?.getOrThrow() as? Result<*> ?: Result.failure<Any?>(TimeoutException("Timeout $timeout elapsed"))
        return concreteResult
    }

}