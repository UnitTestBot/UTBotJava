package org.utbot.instrumentation.instrumentation.execution.ndd

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isStatic
import kotlin.random.Random

class NonDeterministicDetector {
    private val nonDeterministicStaticMethods: HashSet<MethodId> = HashSet()

    private val nonDeterministicClasses: HashSet<ClassId> = buildList {
        add(java.util.Random::class.java)
        add(Random::class.java)
    }.map { it.id }.toHashSet()

    val inserter = NonDeterministicBytecodeInserter()

    fun isNonDeterministic(caller: ClassId, method: MethodId): Boolean {
        return if (method.isStatic) {
            nonDeterministicStaticMethods.contains(method)
        } else {
            isNonDeterministic(caller)
        }
    }

    fun isNonDeterministic(clazz: ClassId): Boolean {
        var cl: ClassId? = clazz
        while (cl != null && !nonDeterministicClasses.contains(cl)) {
           cl = cl.superclass?.id
        }
        return nonDeterministicClasses.contains(cl)
    }

}