package org.utbot.framework.util

import org.utbot.common.Reflection
import org.utbot.engine.ValueConstructor
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import soot.SootMethod
import java.util.concurrent.atomic.AtomicInteger


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
        isConstructor -> declaringClass.id.findConstructor(
            arguments = parameterTypes.map { it.classId }.toTypedArray()
        ).asExecutable()
        else -> declaringClass.id.findMethod(
            name = name,
            returnType = returnType.classId,
            arguments = parameterTypes.map { it.classId }
        ).asExecutable()
    }

val modelIdCounter = AtomicInteger(0)
val instanceCounter = AtomicInteger(0)

fun nextModelName(base: String): String = "$base${instanceCounter.incrementAndGet()}"

fun UtMethodTestSet.toValueTestCase(): UtMethodValueTestSet<*> {
    val valueExecutions = executions.map { ValueConstructor().construct(it) }
    return UtMethodValueTestSet(method, valueExecutions, errors)
}

fun UtModel.isUnit(): Boolean =
        this is UtVoidModel

fun UtExecution.hasThisInstance(): Boolean = when {
    stateBefore.thisInstance == null && stateAfter.thisInstance == null -> false
    stateBefore.thisInstance != null && stateAfter.thisInstance != null -> true
    stateAfter == MissingState -> false
    // An execution must either have this instance or not.
    // This instance cannot be absent before execution and appear after
    // as well as it cannot be present before execution and disappear after.
    else -> error("Execution configuration must not change between states")
}

internal fun valueToClassId(value: Any?) = value?.let { it::class.java.id } ?: objectClassId