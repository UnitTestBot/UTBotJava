package org.utbot.framework.context.spring

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.types.TypeResolver
import org.utbot.framework.context.StaticInitializerConcreteProcessor
import org.utbot.framework.context.simple.SimpleStaticInitializerConcreteProcessor
import soot.jimple.StaticFieldRef

object SpringStaticInitializerConcreteProcessor : StaticInitializerConcreteProcessor {
    override fun shouldProcessStaticFieldConcretely(fieldRef: StaticFieldRef, typeResolver: TypeResolver): Boolean =
        workaround(WorkaroundReason.PROCESS_CONCRETELY_STATIC_INITIALIZERS_IN_ENUMS_FOR_SPRING) {
            val declaringClass = fieldRef.field.declaringClass

            if (declaringClass.isEnum) {
                // Since Spring projects have a lot of complicated enums, we cannot waste resources for theirs analysis,
                // so always process theirs clinit sections concretely
                return true
            }

            return SimpleStaticInitializerConcreteProcessor.shouldProcessStaticFieldConcretely(fieldRef, typeResolver)
        }
}
