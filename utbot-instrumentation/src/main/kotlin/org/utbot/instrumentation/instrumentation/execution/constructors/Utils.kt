package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId
import java.util.concurrent.atomic.AtomicInteger

internal fun valueToClassId(value: Any?) = value?.let { it::class.java.id } ?: objectClassId

val concreteModelId =  AtomicInteger()

fun nextModelName(base: String): String = "${base}_concrete_${concreteModelId.incrementAndGet()}"