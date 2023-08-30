package org.utbot.framework.context.utils

import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.fuzzing.JavaValueProvider

fun ApplicationContext.transformConcreteExecutionContext(
    transformer: (ConcreteExecutionContext) -> ConcreteExecutionContext
) = object : ApplicationContext by this {
    override fun createConcreteExecutionContext(
        fullClasspath: String,
        classpathWithoutDependencies: String
    ): ConcreteExecutionContext = transformer(
        this@transformConcreteExecutionContext.createConcreteExecutionContext(
            fullClasspath, classpathWithoutDependencies
        )
    )
}

fun ApplicationContext.transformValueProvider(
    transformer: (JavaValueProvider) -> JavaValueProvider
) = transformConcreteExecutionContext { it.transformValueProvider(transformer) }
