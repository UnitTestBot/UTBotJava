package org.utbot.fuzzer

/**
 * Fuzzed parameter of a method.
 *
 * @param index of the parameter in method signature
 * @param value fuzzed values
 */
class FuzzedParameter(
    val index: Int,
    val value: FuzzedValue
) {
    operator fun component1() = index
    operator fun component2() = value
}