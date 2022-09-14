package org.utbot.fuzzer

import kotlin.random.Random
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel

/**
 * Chooses a random value using frequencies.
 *
 * If value has greater frequency value then it would be chosen with greater probability.
 *
 * @return the index of the chosen item.
 */
fun Random.chooseOne(frequencies: DoubleArray): Int {
    val total = frequencies.sum()
    val value = nextDouble(total)
    var nextBound = 0.0
    frequencies.forEachIndexed { index, bound ->
        check(bound >= 0) { "Frequency must not be negative" }
        nextBound += bound
        if (value < nextBound) return index
    }
    error("Cannot find next index")
}

/**
 * Tries a value.
 *
 * If a random value is less than [probability] returns true.
 */
fun Random.flipCoin(probability: Int): Boolean {
    check(probability in 0 .. 100) { "probability must in range [0, 100] but $probability is provided" }
    return nextInt(1, 101) <= probability
}

fun Long.invertBit(bitIndex: Int): Long {
    return this xor (1L shl bitIndex)
}


fun ModelProvider.assembleModel(id: Int, constructorId: ConstructorId, params: List<FuzzedValue>): FuzzedValue {
    val instantiationChain = mutableListOf<UtStatementModel>()
    return UtAssembleModel(
        id,
        constructorId.classId,
        "${constructorId.classId.name}${constructorId.parameters}#" + id.hex(),
        instantiationChain = instantiationChain,
        modificationsChain = mutableListOf()
    ).apply {
        instantiationChain += UtExecutableCallModel(null, constructorId, params.map { it.model }, this)
    }.fuzzed {
        summary = "%var% = ${constructorId.classId.simpleName}(${constructorId.parameters.joinToString { it.simpleName }})"
    }
}

fun Int.hex(): String =
    toString(16)
