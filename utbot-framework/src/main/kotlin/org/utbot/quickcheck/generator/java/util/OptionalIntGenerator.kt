package org.utbot.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.generator.java.lang.IntegerGenerator
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.OptionalInt

/**
 * Produces values of type [OptionalInt].
 */
class OptionalIntGenerator : Generator(OptionalInt::class.java) {
    private val integers = IntegerGenerator()

    /**
     * Tells this generator to produce values, when
     * [present][OptionalInt.isPresent], within a specified minimum
     * and/or maximum, inclusive, with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minInt] and [InRange.maxInt], if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange?) {
        integers.configure(range!!)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val trial = random.nextDouble()
        val generated =
            if (trial < 0.25) OptionalInt.empty() else OptionalInt.of(integers.generateValue(random, status))
        return utModelConstructor.construct(generated, classIdForType(OptionalInt::class.java))
    }
}