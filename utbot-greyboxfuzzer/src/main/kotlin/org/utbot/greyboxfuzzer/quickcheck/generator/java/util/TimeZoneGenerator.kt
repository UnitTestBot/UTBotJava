package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.TimeZone

/**
 * Produces values of type [TimeZone].
 */
class TimeZoneGenerator : Generator(TimeZone::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            TimeZone.getTimeZone(random.choose(AVAILABLE_IDS)),
            classIdForType(TimeZone::class.java)
        )
    }

    companion object {
        private val AVAILABLE_IDS = TimeZone.getAvailableIDs()
    }
}