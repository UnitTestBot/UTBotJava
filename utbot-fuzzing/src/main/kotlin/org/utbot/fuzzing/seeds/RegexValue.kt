package org.utbot.fuzzing.seeds

import org.cornutum.regexpgen.js.Provider
import org.cornutum.regexpgen.random.RandomBoundsGen
import org.utbot.fuzzing.Mutation
import kotlin.random.Random
import kotlin.random.asJavaRandom

class RegexValue(
    val pattern: String,
    val random: Random,
    val maxLength: Int = listOf(16, 256, 2048).random(random)
) : StringValue(
    valueProvider = {
        val matchingExact = Provider.forEcmaScript().matchingExact(pattern)
        matchingExact.generate(RandomBoundsGen(random.asJavaRandom()), 1, maxLength)
    }
) {

    override fun mutations(): List<Mutation<out StringValue>> {
        return super.mutations() + Mutation<RegexValue> { source, random, _ ->
            RegexValue(source.pattern, random)
        }
    }
}

fun String.isSupportedPattern(): Boolean {
    if (isEmpty()) return false
    return try {
        Provider.forEcmaScript().matchingExact(this)
        true
    } catch (_: Throwable) {
        false
    }
}