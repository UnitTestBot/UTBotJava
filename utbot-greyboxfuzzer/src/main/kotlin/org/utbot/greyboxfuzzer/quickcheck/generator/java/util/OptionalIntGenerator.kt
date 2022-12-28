package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.generator.java.lang.IntegerGenerator
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
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
            if (trial < 0.25) OptionalInt.empty() else OptionalInt.of(integers.generateValue(random))
        return generatorContext.utModelConstructor.construct(generated, classIdForType(OptionalInt::class.java))
    }
}