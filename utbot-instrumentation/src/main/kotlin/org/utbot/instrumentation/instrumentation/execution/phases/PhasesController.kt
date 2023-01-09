package org.utbot.instrumentation.instrumentation.execution.phases

import java.io.Closeable
import java.security.AccessControlException
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.mock.InstrumentationContext
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.et.TraceHandler

class PhasesController(
    instrumentationContext: InstrumentationContext,
    traceHandler: TraceHandler,
    delegateInstrumentation: Instrumentation<Result<*>>,
) : Closeable {

    val valueConstructionContext = ValueConstructionContext(instrumentationContext)

    val preparationContext = PreparationContext(traceHandler)

    val invocationContext = InvocationContext(delegateInstrumentation)

    val statisticsCollectionContext = StatisticsCollectionContext(traceHandler)

    val modelConstructionContext = ModelConstructionContext(traceHandler)

    val postprocessingContext = PostprocessingContext()

    inline fun computeConcreteExecutionResult(block: PhasesController.() -> UtConcreteExecutionResult): UtConcreteExecutionResult {
        return use {
            try {
                block()
            } catch (e: PhaseError) {
                if (e.cause.cause is AccessControlException) {
                    return@use UtConcreteExecutionResult(
                        MissingState,
                        UtSandboxFailure(e.cause.cause!!),
                        Coverage()
                    )
                }
                // TODO: make failure results from different phase errors
                throw e
            }
        }
    }

    override fun close() {
        valueConstructionContext.close()
    }

}