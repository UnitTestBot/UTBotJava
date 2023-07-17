package org.utbot.framework.context.simple

import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.defaultValueProviders
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.execution.SimpleUtExecutionInstrumentation
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation
import java.io.File

class SimpleConcreteExecutionContext(fullClassPath: String) : ConcreteExecutionContext {
    override val instrumentationFactory: UtExecutionInstrumentation.Factory<*> =
        SimpleUtExecutionInstrumentation.Factory(fullClassPath.split(File.pathSeparator).toSet())

    override fun loadContext(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
    ): ConcreteContextLoadingResult = ConcreteContextLoadingResult.successWithoutExceptions()

    override fun transformExecutionsBeforeMinimization(
        executions: List<UtExecution>,
        classUnderTestId: ClassId
    ): List<UtExecution> = executions

    override fun tryCreateValueProvider(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
        classUnderTest: ClassId,
        idGenerator: IdentityPreservingIdGenerator<Int>
    ): JavaValueProvider = ValueProvider.of(defaultValueProviders(idGenerator))
}