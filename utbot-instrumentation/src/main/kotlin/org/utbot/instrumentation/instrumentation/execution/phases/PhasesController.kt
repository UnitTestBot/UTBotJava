package org.utbot.instrumentation.instrumentation.execution.phases

import com.jetbrains.rd.util.debug
import com.jetbrains.rd.util.getLogger
import java.io.Closeable
import java.security.AccessControlException
import org.utbot.instrumentation.instrumentation.execution.data.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.mock.InstrumentationContext
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtIgnoreFailure
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.execution.data.UtConcreteExecutionSpecification

class PhasesController(
    instrumentationContext: InstrumentationContext,
    traceHandler: TraceHandler,
    delegateInstrumentation: Instrumentation<Result<*>>,
    private val specification: UtConcreteExecutionSpecification?,
) : Closeable {

    private val logger by lazy { getLogger("InstrumentedProcess") }

    val valueConstructionContext = ValueConstructionContext(instrumentationContext)

    val preparationContext = PreparationContext(traceHandler)

    val invocationContext = InvocationContext(delegateInstrumentation)

    val statisticsCollectionContext = StatisticsCollectionContext(traceHandler)

    val modelConstructionContext = ModelConstructionContext(traceHandler)

    val postprocessingContext = PostprocessingContext()

    fun computeConcreteExecutionResult(block: PhasesController.() -> UtConcreteExecutionResult): UtConcreteExecutionResult {
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
                if (specification != null && specification.exceptionIsExpected(e)) {
                    logger.debug { "ignore exception by specification:" }
                    logger.debug { e }
                    return@use UtConcreteExecutionResult(
                        MissingState,
                        UtIgnoreFailure(e),
                        Coverage()
                    )
                }
                throw e
            }
        }
    }

    override fun close() {
        valueConstructionContext.close()
    }

}