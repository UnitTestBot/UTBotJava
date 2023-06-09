package org.utbot.fuzzing.seeds

import com.github.curiousoddman.rgxgen.RgxGen
import com.github.curiousoddman.rgxgen.config.RgxGenOption
import com.github.curiousoddman.rgxgen.config.RgxGenProperties
import org.utbot.fuzzing.Mutation
import kotlin.random.Random
import kotlin.random.asJavaRandom

class RegexValue(val pattern: String, val random: Random) : StringValue(
    valueProvider = {
        RgxGen(pattern).apply {
            setProperties(rgxGenProperties)
        }.generate(random.asJavaRandom())
    }
) {

    override fun mutations(): List<Mutation<out StringValue>> {
        return super.mutations() + Mutation<RegexValue> { source, random, _ ->
            RegexValue(source.pattern, random)
        }
    }
}

private val rgxGenProperties = RgxGenProperties().apply {
    setProperty(RgxGenOption.INFINITE_PATTERN_REPETITION.key, "100")
}