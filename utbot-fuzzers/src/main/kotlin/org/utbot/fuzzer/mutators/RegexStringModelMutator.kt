package org.utbot.fuzzer.mutators

import com.github.curiousoddman.rgxgen.RgxGen
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelMutator
import org.utbot.fuzzer.providers.RegexFuzzedValue
import org.utbot.fuzzer.providers.RegexModelProvider
import kotlin.random.Random
import kotlin.random.asJavaRandom

/**
 * Provides different regex value for a concrete regex pattern
 */
object RegexStringModelMutator : ModelMutator {

    override fun mutate(
        description: FuzzedMethodDescription,
        index: Int,
        value: FuzzedValue,
        random: Random
    ): FuzzedValue? {
        if (value is RegexFuzzedValue) {
            val string = RgxGen(value.regex).apply {
                setProperties(RegexModelProvider.rgxGenProperties)
            }.generate(random.asJavaRandom())
            return RegexFuzzedValue(UtPrimitiveModel(string).mutatedFrom(value) {
                summary = "%var% = mutated regex ${value.regex}"
            }, value.regex)
        }
        return null
    }

}