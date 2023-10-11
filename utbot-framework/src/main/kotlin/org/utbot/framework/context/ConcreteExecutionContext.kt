package org.utbot.framework.context

import org.utbot.engine.MockStrategy
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation

interface ConcreteExecutionContext {
    val instrumentationFactory: UtExecutionInstrumentation.Factory<*>

    fun loadContext(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
    ): ConcreteContextLoadingResult

    fun transformExecutionsBeforeMinimization(
        executions: List<UtExecution>,
        methodUnderTest: ExecutableId,
    ): List<UtExecution>

    fun transformExecutionsAfterMinimization(
        executions: List<UtExecution>,
        methodUnderTest: ExecutableId,
        rerunExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
    ): List<UtExecution>

    fun tryCreateFuzzingContext(params: FuzzingContextParams): JavaFuzzingContext

    data class FuzzingContextParams(
        val concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
        val classUnderTest: ClassId,
        val idGenerator: IdentityPreservingIdGenerator<Int>,
        val fuzzingStartTimeMillis: Long,
        val fuzzingEndTimeMillis: Long,
        val mockStrategy: MockStrategy,
    )
}