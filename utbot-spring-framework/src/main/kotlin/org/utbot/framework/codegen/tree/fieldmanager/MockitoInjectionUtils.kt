package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.isSubtypeOf

object MockitoInjectionUtils {
    /*
     * If count of fields of the same type is 1, then we mock/spy variable by @Mock/@Spy annotation,
     * otherwise we will create this variable by simple variable constructor.
     */
    fun ClassId.canBeInjectedByTypeInto(classToInjectInto: ClassId): Boolean =
        classToInjectInto.allDeclaredFieldIds.filter { isSubtypeOf(it.type) }.toList().size == 1
}