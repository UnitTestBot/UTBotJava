package org.utbot.fuzzer

import org.utbot.fuzzer.types.Type

/**
 * Object to pass concrete values to fuzzer
 */
data class FuzzedConcreteValue(
    val type: Type,
    val value: Any,
    val fuzzedContext: FuzzedContext = FuzzedContext.Unknown,
)