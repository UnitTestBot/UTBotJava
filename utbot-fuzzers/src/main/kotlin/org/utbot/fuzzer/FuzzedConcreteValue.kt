package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ClassId

/**
 * Object to pass concrete values to fuzzer
 */
data class FuzzedConcreteValue(
    val classId: ClassId,
    val value: Any,
    val fuzzedContext: FuzzedContext = FuzzedContext.Unknown,
)