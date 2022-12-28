package org.utbot.greyboxfuzzer.quickcheck.generator.java.time

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.time.ZoneId

/**
 * Produces values of type [ZoneId].
 */
class ZoneIdGenerator : Generator(ZoneId::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            ZoneId.of(random.choose(ZoneId.getAvailableZoneIds())),
            ZoneId::class.id
        )
    }
}