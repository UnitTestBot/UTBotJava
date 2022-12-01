package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.BitSet

/**
 * Produces values of type [BitSet].
 */
class BitSetGenerator : Generator(BitSet::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val size = status.size()
        val bits = BitSet(size)
        for (i in 0 until size) {
            bits[i] = random.nextBoolean()
        }
        return generatorContext.utModelConstructor.construct(bits, BitSet::class.id)
    }
}