package org.utbot.framework.context

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.JavaValueProvider
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
        classUnderTestId: ClassId
    ): List<UtExecution>

    fun tryCreateValueProvider(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
        classUnderTest: ClassId,
        idGenerator: IdentityPreservingIdGenerator<Int>,
    ): JavaValueProvider

    fun createStateBefore(
        thisInstance: UtModel?,
        parameters: List<UtModel>,
        statics: Map<FieldId, UtModel>,
        executableToCall: ExecutableId,
        idGenerator: IdGenerator<Int>
    ): EnvironmentModels
}