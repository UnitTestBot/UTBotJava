package org.utbot.quickcheck.generator.java.time

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.random.SourceOfRandomness
import java.time.ZoneId

/**
 * Produces values of type [ZoneId].
 */
class ZoneIdGenerator : Generator(ZoneId::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return utModelConstructor.construct(
            ZoneId.of(random.choose(ZoneId.getAvailableZoneIds())),
            ZoneId::class.id
        )
    }
}