package org.utbot.framework.context

import org.utbot.engine.types.TypeResolver
import soot.jimple.StaticFieldRef

interface StaticInitializerConcreteProcessor {
    /**
     * Decides should we read this static field concretely or not.
     */
    fun shouldProcessStaticFieldConcretely(fieldRef: StaticFieldRef, typeResolver: TypeResolver): Boolean
}
