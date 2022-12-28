package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.Locale

/**
 * Produces values of type [Locale].
 */
class LocaleGenerator : Generator(Locale::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            random.choose(AVAILABLE_LOCALES),
            classIdForType(Locale::class.java)
        )
    }

    companion object {
        private val AVAILABLE_LOCALES = Locale.getAvailableLocales()
    }
}