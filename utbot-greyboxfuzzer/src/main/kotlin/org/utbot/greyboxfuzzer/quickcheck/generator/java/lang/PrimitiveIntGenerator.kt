package org.utbot.greyboxfuzzer.quickcheck.generator.java.lang

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.generator.IntegralGenerator
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `int` or [Integer].
 */
class PrimitiveIntGenerator : IntegralGenerator(listOf(Int::class.javaPrimitiveType!!)) {
    private var min = Reflection.defaultValueOf(InRange::class.java, "minInt") as Int
    private var max = Reflection.defaultValueOf(InRange::class.java, "maxInt") as Int

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minInt] and [InRange.maxInt], if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        min = if (range.min.isEmpty()) range.minInt else range.min.toInt()
        max = if (range.max.isEmpty()) range.maxInt else range.max.toInt()
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(generateValue(random, status), intClassId)
    }

    fun generateValue(
        random: SourceOfRandomness,
        status: GenerationStatus?
    ): Int {
        return random.nextInt(min, max)
    }
}