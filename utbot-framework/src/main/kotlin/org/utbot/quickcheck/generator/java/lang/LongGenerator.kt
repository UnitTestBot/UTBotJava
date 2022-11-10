package org.utbot.quickcheck.generator.java.lang

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.longWrapperClassId
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.generator.IntegralGenerator
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `long` or [Long].
 */
class LongGenerator : IntegralGenerator(listOf(Long::class.java)) {
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
        return utModelConstructor.construct(generateValue(random, status), longWrapperClassId)
    }

    fun generateValue(
        random: SourceOfRandomness,
        status: GenerationStatus?
    ): Long {
        return random.nextLong(min, max)
    }
}