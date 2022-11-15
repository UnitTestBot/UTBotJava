package org.utbot.quickcheck.generator.java.lang

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.shortWrapperClassId
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.generator.IntegralGenerator
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `short` or [Short].
 */
class ShortGenerator : IntegralGenerator(listOf(Short::class.javaObjectType)) {
    private var min = Reflection.defaultValueOf(InRange::class.java, "minShort") as Short
    private var max = Reflection.defaultValueOf(InRange::class.java, "maxShort") as Short

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minShort] and [InRange.maxShort], if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        min = if (range.min.isEmpty()) range.minShort else range.min.toShort()
        max = if (range.max.isEmpty()) range.maxShort else range.max.toShort()
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return utModelConstructor.construct(random.nextShort(min, max), shortWrapperClassId)
    }
}