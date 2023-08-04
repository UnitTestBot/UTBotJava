package org.utbot.framework.context.simple

import org.utbot.framework.context.ConcreteExecutionContext
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

    override fun createStateBefore(
        thisInstance: UtModel?,
        parameters: List<UtModel>,
        statics: Map<FieldId, UtModel>,
        executableToCall: ExecutableId,
        idGenerator: IdGenerator<Int>
    ): EnvironmentModels = EnvironmentModels(
        thisInstance = thisInstance,
        parameters = parameters,
        statics = statics,
        executableToCall = executableToCall
    )
}