package org.utbot.framework.context.custom

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.engine.executeConcretely
import org.utbot.framework.UtSettings
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation

class RerunningConcreteExecutionContext(
    private val delegateContext: ConcreteExecutionContext,
    private val maxRerunsPerMethod: Int,
    private val rerunTimeoutInMillis: Long = 10L * UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis,
) : ConcreteExecutionContext by delegateContext {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun transformExecutionsAfterMinimization(
        executions: List<UtExecution>,
        methodUnderTest: ExecutableId,
        rerunExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
    ): List<UtExecution> {
        @Suppress("NAME_SHADOWING")
        val executions = delegateContext.transformExecutionsAfterMinimization(
            executions,
            methodUnderTest,
            rerunExecutor
        )
            // it's better to rerun executions with non-empty coverage,
            // because executions with empty coverage are often duplicated
            .sortedBy { it.coverage?.coveredInstructions.isNullOrEmpty() }
        return executions
            .take(maxRerunsPerMethod)
            .map { execution ->
                runBlocking {
                    val result = try {
                        rerunExecutor.executeConcretely(
                            methodUnderTest = methodUnderTest,
                            stateBefore = execution.stateBefore,
                            instrumentation = emptyList(),
                            timeoutInMillis = rerunTimeoutInMillis,
                            isRerun = true,
                        )
                    } catch (e: Throwable) {
                        // we can't update execution result if we don't have a result
                        logger.warn(e) { "Rerun failed, keeping original result for execution [$execution]" }
                        return@runBlocking execution
                    }
                    execution.copy(
                        stateBefore = result.stateBefore,
                        stateAfter = result.stateAfter,
                        result = result.result,
                        coverage = result.coverage,
                    )
                }
            } + executions.drop(maxRerunsPerMethod)
    }
}