package org.utbot.framework.context.utils

import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.context.ConcreteExecutionContext.FuzzingContextParams
import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation

fun ConcreteExecutionContext.transformInstrumentationFactory(
    transformer: (UtExecutionInstrumentation.Factory<*>) -> UtExecutionInstrumentation.Factory<*>
) = object : ConcreteExecutionContext by this {
    override val instrumentationFactory: UtExecutionInstrumentation.Factory<*> =
        transformer(this@transformInstrumentationFactory.instrumentationFactory)
}

fun ConcreteExecutionContext.transformJavaFuzzingContext(
    transformer: FuzzingContextParams.(JavaFuzzingContext) -> JavaFuzzingContext
) = object : ConcreteExecutionContext by this {
    override fun tryCreateFuzzingContext(params: FuzzingContextParams): JavaFuzzingContext = params.transformer(
        this@transformJavaFuzzingContext.tryCreateFuzzingContext(params)
    )
}

fun ConcreteExecutionContext.transformValueProvider(
    transformer: FuzzingContextParams.(JavaValueProvider) -> JavaValueProvider
) = transformJavaFuzzingContext { javaFuzzingContext ->
    javaFuzzingContext.transformValueProvider { valueProvider -> transformer(valueProvider) }
}
