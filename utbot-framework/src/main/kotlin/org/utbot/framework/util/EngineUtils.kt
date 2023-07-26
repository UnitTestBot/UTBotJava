package org.utbot.framework.util

import mu.KotlinLogging
import org.utbot.common.logException
import org.utbot.framework.plugin.api.util.constructor.ValueConstructor
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtMethodValueTestSet
import org.utbot.framework.plugin.api.UtVoidModel
import java.util.concurrent.atomic.AtomicInteger


private val logger = KotlinLogging.logger {  }

private val instanceCounter = AtomicInteger(0)

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
    else -> logger.logException { error("Execution configuration must not change between states") }
}