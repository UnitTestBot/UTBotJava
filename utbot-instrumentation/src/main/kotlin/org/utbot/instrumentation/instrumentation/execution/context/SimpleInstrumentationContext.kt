package org.utbot.instrumentation.instrumentation.execution.context

import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtModel

class SimpleInstrumentationContext : InstrumentationContext {
    override val methodSignatureToId = mutableMapOf<String, Int>()

    override fun constructContextDependentValue(model: UtModel): UtConcreteValue<*>? = null
}