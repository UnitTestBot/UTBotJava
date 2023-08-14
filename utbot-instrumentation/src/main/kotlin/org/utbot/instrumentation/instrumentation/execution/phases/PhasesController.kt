package org.utbot.instrumentation.instrumentation.execution.phases

import com.jetbrains.rd.util.getLogger
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
import org.utbot.instrumentation.instrumentation.execution.PreliminaryUtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionData
import org.utbot.instrumentation.instrumentation.execution.context.InstrumentationContext
import java.security.AccessControlException

class PhasesController(
    private val instrumentationContext: InstrumentationContext,
    traceHandler: TraceHandler,
    delegateInstrumentation: Instrumentation<Result<*>>,
    private val timeout: Long
) {
    private var currentlyElapsed = 0L
    val valueConstructionPhase = ValueConstructionPhase(instrumentationContext)

    val preparationPhase = PreparationPhase(traceHandler)

    val invocationPhase = InvocationPhase(delegateInstrumentation)

    val statisticsCollectionPhase = StatisticsCollectionPhase(traceHandler)

    val modelConstructionPhase = ModelConstructionPhase(
        traceHandler = traceHandler,
        utModelWithCompositeOriginConstructorFinder = instrumentationContext::findUtModelWithCompositeOriginConstructor
    )

    val postprocessingPhase = PostprocessingPhase()

    inline fun computeConcreteExecutionResult(block: PhasesController.() -> PreliminaryUtConcreteExecutionResult): PreliminaryUtConcreteExecutionResult {
        try {
            return this.block()
        } catch (e: ExecutionPhaseStop) {
            return e.result
        } catch (e: ExecutionPhaseError) {
            if (e.cause.cause is AccessControlException) {
                return PreliminaryUtConcreteExecutionResult(
                    stateAfter = MissingState,
                    result = UtSandboxFailure(e.cause.cause!!),
                    coverage = Coverage()
                )
            }

            throw e
        }
    }

    companion object {
        private val logger = getLogger<PhasesController>()
    }

    fun <T, R : ExecutionPhase> executePhaseInTimeout(phase: R, block: R.() -> T): T = phase.start {
        val stopWatch = StopWatch()
        val context = UtContext(utContext.classLoader, stopWatch)
        val timeoutForCurrentPhase = timeout - currentlyElapsed
        val executor = ThreadBasedExecutor.threadLocal
        val result = executor.invokeWithTimeout(timeout - currentlyElapsed, stopWatch) {
            withUtContext(context) {
                try {
                    phase.block()
                } finally {
                    if (executor.isCurrentThreadTimedOut())
                        instrumentationContext.onPhaseTimeout(phase)
                }
            }
        } ?: throw TimeoutException("Timeout $timeoutForCurrentPhase ms for phase ${phase.javaClass.simpleName} elapsed, controller timeout - $timeout")

        val blockElapsed = stopWatch.get()
        currentlyElapsed += blockElapsed

        return@start result.getOrThrow() as T
    }

    fun <T, R : ExecutionPhase> executePhaseWithoutTimeout(phase: R, block: R.() -> T): T = phase.start {
        return@start ThreadBasedExecutor.threadLocal.invokeWithoutTimeout {
            phase.block()
        }.getOrThrow() as T
    }

    fun applyPreprocessing(parameters: UtConcreteExecutionData): ConstructedData {

        val constructedData = executePhaseInTimeout(valueConstructionPhase) {
            val params = constructParameters(parameters.stateBefore)
            val statics = constructStatics(parameters.stateBefore)

            // here static methods and instances are mocked
            mock(parameters.instrumentation)

            ConstructedData(params, statics, getCache())
        }

        // invariants:
        // 1. phase must always complete if started as static reset relies on it
        // 2. phase must be fast as there are no incremental changes
        postprocessingPhase.setStaticFields(preparationPhase.start {
            val result = setStaticFields(constructedData.statics)
            resetTrace()
            resetND()
            result
        })

        return constructedData
    }

    fun applyPostprocessing() {
        postprocessingPhase.start {
            resetStaticFields()
            valueConstructionPhase.resetMockMethods()
        }
    }
}