package org.utbot.framework.context

import org.utbot.framework.plugin.api.CodeGenerationContext

interface ApplicationContext : CodeGenerationContext {
    val mockerContext: MockerContext
    val typeReplacer: TypeReplacer
    val nonNullSpeculator: NonNullSpeculator

    fun createConcreteExecutionContext(
        fullClasspath: String,
        classpathWithoutDependencies: String
    ): ConcreteExecutionContext
}