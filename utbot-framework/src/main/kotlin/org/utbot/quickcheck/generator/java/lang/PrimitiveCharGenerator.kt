package org.utbot.quickcheck.generator.java.lang

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `char` or [Character].
 */
class PrimitiveCharGenerator : Generator(listOf(Char::class.javaPrimitiveType!!)) {
    private var min = Reflection.defaultValueOf(InRange::class.java, "minChar") as Char
    private var max = Reflection.defaultValueOf(InRange::class.java, "maxChar") as Char

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minChar] and [InRange.maxChar], if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        min = if (range.min.isEmpty()) range.minChar else range.min[0]
        max = if (range.max.isEmpty()) range.maxChar else range.max[0]
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(random.nextChar(min, max), charClassId)
    }

}