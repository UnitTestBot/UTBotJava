package org.utbot.framework.context.simple

import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.plugin.api.UtError

class SimpleConcreteExecutionContext(
    // TODO these properties will be used later (to fulfill TODO in ConcreteExecutionContext)
    val fullClassPath: String,
    val classpathWithoutDependencies: String
) : ConcreteExecutionContext {
    override fun preventsFurtherTestGeneration(): Boolean = false

    override fun getErrors(): List<UtError> = emptyList()
}