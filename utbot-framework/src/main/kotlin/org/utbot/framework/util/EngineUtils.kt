package org.utbot.framework.util

import org.utbot.common.Reflection
import org.utbot.engine.ValueConstructor
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtMethodValueTestSet
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectClassId
import java.util.concurrent.atomic.AtomicInteger
import soot.SootMethod


@Suppress("DEPRECATION")
val Class<*>.anyInstance: Any
    get() {
//        val defaultCtor = declaredConstructors.singleOrNull { it.parameterCount == 0}
//        if (defaultCtor != null) {
//            try {
//                defaultCtor.isAccessible = true
//                return defaultCtor.newInstance()
//            } catch (e : Throwable) {
//                logger.warn(e) { "Can't create object with default ctor. Fallback to Unsafe." }
//            }
//        }
        return Reflection.unsafe.allocateInstance(this)

//        val constructors = runCatching {
//            arrayOf(getDeclaredConstructor())
//        }.getOrElse { declaredConstructors }
//
//        return constructors.asSequence().mapNotNull { constructor ->
//            runCatching {
//                val parameters = constructor.parameterTypes.map { defaultParameterValue(it) }
//                val isAccessible = constructor.isAccessible
//                try {
//                    constructor.isAccessible = true
//                    constructor.newInstance(*parameters.toTypedArray())
//                } finally {
//                    constructor.isAccessible = isAccessible
//                }
//            }.getOrNull()
//        }.firstOrNull() ?: error("Failed to create instance of $this")
    }

/**
 * Gets method or constructor id of SootMethod.
 */
val SootMethod.executableId: ExecutableId
    get() = when {
        isConstructor -> constructorId(
                classId = declaringClass.id,
                arguments = parameterTypes.map { it.classId }.toTypedArray()
        )
        else -> methodId(
                classId = declaringClass.id,
                name = name,
                returnType = returnType.classId,
                arguments = parameterTypes.map { it.classId }.toTypedArray()
        )
    }

val modelIdCounter = AtomicInteger(0)
val instanceCounter = AtomicInteger(0)

fun nextModelName(base: String): String = "$base${instanceCounter.incrementAndGet()}"

fun UtMethodTestSet.toValueTestCase(): UtMethodValueTestSet<*> {
    val valueExecutions = executions.map { ValueConstructor().construct(it) } // TODO: make something about UTExecution
    return UtMethodValueTestSet(method, valueExecutions, errors)
}

fun UtModel.isUnit(): Boolean =
        this is UtVoidModel

fun UtSymbolicExecution.hasThisInstance(): Boolean = when {
    stateBefore.thisInstance == null && stateAfter.thisInstance == null -> false
    stateBefore.thisInstance != null && stateAfter.thisInstance != null -> true
    stateAfter == MissingState -> false
    // An execution must either have this instance or not.
    // This instance cannot be absent before execution and appear after
    // as well as it cannot be present before execution and disappear after.
    else -> error("Execution configuration must not change between states")
}

internal fun valueToClassId(value: Any?) = value?.let { it::class.java.id } ?: objectClassId