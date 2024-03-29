package org.utbot.framework.context.simple

import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.context.ConcreteExecutionContext.FuzzingContextParams
import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtExecution
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
        methodUnderTest: ExecutableId,
    ): List<UtExecution> = executions

    override fun transformExecutionsAfterMinimization(
        executions: List<UtExecution>,
        methodUnderTest: ExecutableId,
        rerunExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
    ): List<UtExecution> = executions

    override fun tryCreateFuzzingContext(params: FuzzingContextParams): JavaFuzzingContext =
        SimpleJavaFuzzingContext(params.classUnderTest, params.idGenerator)
}