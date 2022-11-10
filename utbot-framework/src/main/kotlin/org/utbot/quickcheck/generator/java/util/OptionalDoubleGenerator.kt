package org.utbot.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.generator.java.lang.DoubleGenerator
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.OptionalDouble

/**
 * Produces values of type [OptionalDouble].
 */
class OptionalDoubleGenerator : Generator(OptionalDouble::class.java) {
    private val doubles = DoubleGenerator()

    /**
     * Tells this generator to produce values, when
     * [present][OptionalDouble.isPresent], within a specified minimum
     * (inclusive) and/or maximum (exclusive) with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minDouble] and [InRange.maxDouble],
     * if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange?) {
        doubles.configure(range!!)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val trial = random.nextDouble()
        val generated =
            if (trial < 0.25) OptionalDouble.empty() else OptionalDouble.of(doubles.generateValue(random, status))
        return utModelConstructor.construct(generated, classIdForType(OptionalDouble::class.java))
    }
}