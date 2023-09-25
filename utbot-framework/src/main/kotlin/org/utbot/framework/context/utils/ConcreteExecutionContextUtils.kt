package org.utbot.framework.context.utils

import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.context.ConcreteExecutionContext.FuzzingContextParams
import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.fuzzing.JavaValueProvider

fun ConcreteExecutionContext.transformJavaFuzzingContext(
    transformer: FuzzingContextParams.(JavaFuzzingContext) -> JavaFuzzingContext
) = object : ConcreteExecutionContext by this {
    override fun tryCreateFuzzingContext(params: FuzzingContextParams): JavaFuzzingContext = params.transformer(
        this@transformJavaFuzzingContext.tryCreateFuzzingContext(params)
    )
}

fun ConcreteExecutionContext.transformValueProvider(
    transformer: (JavaValueProvider) -> JavaValueProvider
) = transformJavaFuzzingContext { it.transformValueProvider(transformer) }
