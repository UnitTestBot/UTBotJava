package org.utbot.greyboxfuzzer.quickcheck.generator.java.lang

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.generator.IntegralGenerator
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `long` or [Long].
 */
class PrimitiveLongGenerator : IntegralGenerator(listOf(Long::class.javaPrimitiveType!!)) {
    private var min = Reflection.defaultValueOf(InRange::class.java, "minLong") as Long
    private var max = Reflection.defaultValueOf(InRange::class.java, "maxLong") as Long

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minLong] and [InRange.maxLong], if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        min = if (range.min.isEmpty()) range.minLong else range.min.toLong()
        max = if (range.max.isEmpty()) range.maxLong else range.max.toLong()
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(generateValue(random, status), longClassId)
    }

    fun generateValue(
        random: SourceOfRandomness,
        status: GenerationStatus?
    ): Long {
        return random.nextLong(min, max)
    }
}