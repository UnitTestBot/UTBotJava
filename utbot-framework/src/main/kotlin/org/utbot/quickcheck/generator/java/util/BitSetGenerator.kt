package org.utbot.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.random.SourceOfRandomness
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
        return utModelConstructor.construct(bits, BitSet::class.id)
    }
}