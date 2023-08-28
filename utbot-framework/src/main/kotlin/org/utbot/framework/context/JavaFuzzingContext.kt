package org.utbot.framework.context

import org.utbot.engine.MockStrategy
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult

interface JavaFuzzingContext {
    val classUnderTest: ClassId
    val mockStrategy: MockStrategy
    val idGenerator: IdentityPreservingIdGenerator<Int>
    val valueProvider: JavaValueProvider

    fun createStateBefore(
        thisInstance: UtModel?,
        parameters: List<UtModel>,
        statics: Map<FieldId, UtModel>,
        executableToCall: ExecutableId,
    ): EnvironmentModels

    fun handleFuzzedConcreteExecutionResult(
        methodUnderTest: ExecutableId,
        concreteExecutionResult: UtConcreteExecutionResult,
    )
}