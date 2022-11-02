package org.utbot.fuzzing.seeds

import com.github.curiousoddman.rgxgen.RgxGen
import com.github.curiousoddman.rgxgen.config.RgxGenOption
import com.github.curiousoddman.rgxgen.config.RgxGenProperties
import org.utbot.fuzzing.Mutation
import kotlin.random.Random
import kotlin.random.asJavaRandom

class RegexValue(val pattern: String, val random: Random) : KnownValue {

    val value: String by lazy { RgxGen(pattern).apply {
        setProperties(rgxGenProperties)
    }.generate(random.asJavaRandom()) }

    override fun mutations() = listOf(Mutation<KnownValue> { source, random, _ ->
        require(this === source)
        RegexValue(pattern, random)
    })
}

private val rgxGenProperties = RgxGenProperties().apply {
    setProperty(RgxGenOption.INFINITE_PATTERN_REPETITION.key, "5")
}