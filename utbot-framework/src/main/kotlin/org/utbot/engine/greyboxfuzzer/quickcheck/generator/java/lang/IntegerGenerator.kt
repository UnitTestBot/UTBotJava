package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.lang

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.intWrapperClassId
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.IntegralGenerator
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `int` or [Integer].
 */
class IntegerGenerator : IntegralGenerator(listOf(Int::class.javaObjectType)) {
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
        return generatorContext.utModelConstructor.construct(generateValue(random), intWrapperClassId)
    }

    fun generateValue(
        random: SourceOfRandomness
    ): Int {
        return random.nextInt(min, max)
    }
}