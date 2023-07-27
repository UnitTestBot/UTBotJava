package org.utbot.instrumentation.instrumentation.execution.context

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.instrumentation.instrumentation.execution.constructors.UtCustomModelConstructor
import org.utbot.instrumentation.instrumentation.execution.constructors.javaStdLibCustomModelConstructors

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

    override fun findUtCustomModelConstructor(classId: ClassId): UtCustomModelConstructor? =
        javaStdLibCustomModelConstructors[classId.jClass]?.invoke()
}