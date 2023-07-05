package org.utbot.instrumentation.instrumentation.execution.context

import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtModel

/**
 * Simple instrumentation context, that is used for pure JVM projects without
 * any frameworks with special support from UTBot (like Spring)
 */
class SimpleInstrumentationContext : InstrumentationContext {
    override val methodSignatureToId = mutableMapOf<String, Int>()

    /**
     * There are no context dependent values for pure JVM projects
     */
    override fun constructContextDependentValue(model: UtModel): UtConcreteValue<*>? = null
}