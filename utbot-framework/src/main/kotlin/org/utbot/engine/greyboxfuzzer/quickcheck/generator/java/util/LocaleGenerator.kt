package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
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