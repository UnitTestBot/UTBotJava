package org.utbot.instrumentation.instrumentation.execution.phases

import org.utbot.common.StopWatch
import org.utbot.common.ThreadBasedExecutor
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.mock.InstrumentationContext
import java.security.AccessControlException

class PhasesController(
    instrumentationContext: InstrumentationContext,
    traceHandler: TraceHandler,
    delegateInstrumentation: Instrumentation<Result<*>>,
    private val timeout: Long
) {
    private var currentlyElapsed = 0L
    val valueConstructionPhase = ValueConstructionPhase(instrumentationContext)

    val preparationPhase = PreparationPhase(traceHandler)

    val invocationPhase = InvocationPhase(delegateInstrumentation)

    val statisticsCollectionPhase = StatisticsCollectionPhase(traceHandler)

    val modelConstructionPhase = ModelConstructionPhase(traceHandler)

    val postprocessingPhase = PostprocessingPhase()

    inline fun computeConcreteExecutionResult(block: PhasesController.() -> UtConcreteExecutionResult): UtConcreteExecutionResult {
        try {
            return this.block()
        } catch (e: ExecutionPhaseStop) {
            return e.result
        } catch (e: ExecutionPhaseError) {
            if (e.cause.cause is AccessControlException) {
                return UtConcreteExecutionResult(
                    MissingState,
                    UtSandboxFailure(e.cause.cause!!),
                    Coverage()
                )
            }

            throw e
        }
    }

    fun <T, R : ExecutionPhase> executePhaseInTimeout(phase: R, block: R.() -> T): T = phase.start {
        val stopWatch = StopWatch()
        val context = UtContext(utContext.classLoader, stopWatch)
        val timeoutForCurrentPhase = timeout - currentlyElapsed
        val result = ThreadBasedExecutor.threadLocal.invokeWithTimeout(timeout - currentlyElapsed, stopWatch) {
            withUtContext(context) {
                phase.block()
            }
        } ?: throw TimeoutException("Timeout $timeoutForCurrentPhase ms for phase ${phase.javaClass.simpleName} elapsed, controller timeout - $timeout")

        val blockElapsed = stopWatch.get()
        currentlyElapsed += blockElapsed

        return@start result.getOrThrow() as T
    }
}