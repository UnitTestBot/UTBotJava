package org.utbot.framework.context.simple

import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.context.MockerContext
import org.utbot.framework.context.NonNullSpeculator
import org.utbot.framework.context.TypeReplacer

/**
 * A context to use when no specific data is required.
 */
class SimpleApplicationContext(
    override val mockerContext: MockerContext,
    override val typeReplacer: TypeReplacer = SimpleTypeReplacer(),
    override val nonNullSpeculator: NonNullSpeculator = SimpleNonNullSpeculator()
) : ApplicationContext {
    override fun createConcreteExecutionContext(
        fullClasspath: String,
        classpathWithoutDependencies: String
    ): ConcreteExecutionContext = SimpleConcreteExecutionContext(fullClasspath, classpathWithoutDependencies)
}