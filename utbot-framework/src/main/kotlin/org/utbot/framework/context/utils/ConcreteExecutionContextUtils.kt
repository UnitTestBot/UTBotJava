package org.utbot.framework.context.utils

import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation

fun ConcreteExecutionContext.transformJavaFuzzingContext(
    transformer: (JavaFuzzingContext) -> JavaFuzzingContext
) = object : ConcreteExecutionContext by this {
    override fun tryCreateFuzzingContext(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
        classUnderTest: ClassId,
        idGenerator: IdentityPreservingIdGenerator<Int>
    ): JavaFuzzingContext = transformer(
        this@transformJavaFuzzingContext.tryCreateFuzzingContext(concreteExecutor, classUnderTest, idGenerator)
    )
}

fun ConcreteExecutionContext.transformValueProvider(
    transformer: (JavaValueProvider) -> JavaValueProvider
) = transformJavaFuzzingContext { it.transformValueProvider(transformer) }
