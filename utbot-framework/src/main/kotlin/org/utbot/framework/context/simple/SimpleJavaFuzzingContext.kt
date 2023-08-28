package org.utbot.framework.context.simple

import org.utbot.engine.MockStrategy
import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.defaultValueProviders
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult

class SimpleJavaFuzzingContext(
    override val classUnderTest: ClassId,
    override val mockStrategy: MockStrategy,
    override val idGenerator: IdentityPreservingIdGenerator<Int>,
) : JavaFuzzingContext {
    override val valueProvider: JavaValueProvider =
        ValueProvider.of(defaultValueProviders(idGenerator))

    override fun createStateBefore(
        thisInstance: UtModel?,
        parameters: List<UtModel>,
        statics: Map<FieldId, UtModel>,
        executableToCall: ExecutableId,
    ): EnvironmentModels = EnvironmentModels(
        thisInstance = thisInstance,
        parameters = parameters,
        statics = statics,
        executableToCall = executableToCall
    )

    override fun handleFuzzedConcreteExecutionResult(
        methodUnderTest: ExecutableId,
        concreteExecutionResult: UtConcreteExecutionResult
    ) = Unit
}
