package org.utbot.instrumentation.instrumentation.execution.ndd

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import java.lang.reflect.Method
import java.security.SecureRandom
import kotlin.random.Random

class NonDeterministicDetector {

    private fun MutableList<Method>.addJavaRandomMethods() {
        addAll(java.util.Random::class.java.methods
            .filter { it.name.startsWith("next") && it.returnType?.isPrimitive == true }
        )
    }

    private fun MutableList<Method>.addKotlinRandomMethods() {
        val badMethods = hashSetOf("nextBits", "nextBytes", "nextUBytes")
        addAll(Random::class.java.methods
            .filter { it.name.startsWith("next") && it.returnType?.isPrimitive == true }
            .filterNot { badMethods.contains(it.name) }
        )
    }

    private val nonDeterministicMethods: HashSet<MethodId> = buildList {
        addJavaRandomMethods()
        addKotlinRandomMethods()
    }.map { it.executableId }.toHashSet()

    private val nonDeterministicClasses: HashSet<ClassId> = buildList {
        add(java.util.Random::class.java)
        add(Random::class.java)
    }.map { it.id }.toHashSet()

    val inserter = NonDeterministicBytecodeInserter()

    fun isNonDeterministic(method: MethodId): Boolean {
        var mt: MethodId? = method
        while (mt != null && !nonDeterministicMethods.contains(mt)) {
            mt = method.classId.superclass?.let { clazz ->
                clazz.id.allMethods.find { it.signature == method.signature }
            }
        }
        return nonDeterministicMethods.contains(mt)
    }

    fun isNonDeterministic(clazz: ClassId): Boolean {
        var cl: ClassId? = clazz
        while (cl != null && !nonDeterministicClasses.contains(cl)) {
           cl = cl.superclass?.id
        }
        return nonDeterministicClasses.contains(cl)
    }

}