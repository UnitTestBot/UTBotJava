package org.utbot.framework.context

import org.utbot.framework.codegen.generator.AbstractCodeGenerator
import org.utbot.framework.codegen.generator.CodeGeneratorParams

interface ApplicationContext {
    val mockerContext: MockerContext
    val typeReplacer: TypeReplacer
    val nonNullSpeculator: NonNullSpeculator
    val staticInitializerConcreteProcessor: StaticInitializerConcreteProcessor

    fun createConcreteExecutionContext(
        fullClasspath: String,
        classpathWithoutDependencies: String
    ): ConcreteExecutionContext

    fun createCodeGenerator(params: CodeGeneratorParams): AbstractCodeGenerator
}