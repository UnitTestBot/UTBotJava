package org.utbot.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.generator.java.lang.LongGenerator
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.OptionalLong

/**
 * Produces values of type [OptionalLong].
 */
class OptionalLongGenerator : Generator(OptionalLong::class.java) {
    private val longs = LongGenerator()

    /**
     * Tells this generator to produce values, when
     * [present][OptionalLong.isPresent], within a specified minimum
     * and/or maximum, inclusive, with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minLong] and [InRange.maxLong], if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange?) {
        longs.configure(range!!)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val trial = random.nextDouble()
        val generated = if (trial < 0.25) OptionalLong.empty() else OptionalLong.of(longs.generateValue(random, status))
        return utModelConstructor.construct(generated, classIdForType(OptionalLong::class.java))
    }
}