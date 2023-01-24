package org.utbot.greyboxfuzzer.quickcheck.generator.java.lang

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.doubleWrapperClassId
import org.utbot.greyboxfuzzer.quickcheck.generator.DecimalGenerator
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness

/**
 * Produces values for property parameters of type `double` or
 * [Double].
 */
class DoubleGenerator : DecimalGenerator(listOf(Double::class.javaObjectType)) {
    private var min = Reflection.defaultValueOf(InRange::class.java, "minDouble") as Double
    private var max = Reflection.defaultValueOf(InRange::class.java, "maxDouble") as Double

    /**
     * Tells this generator to produce values within a specified minimum
     * (inclusive) and/or maximum (exclusive) with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minDouble] and [InRange.maxDouble],
     * if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        min = if (range.min.isEmpty()) range.minDouble else range.min.toDouble()
        max = if (range.max.isEmpty()) range.maxDouble else range.max.toDouble()
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(generateValue(random, status), doubleWrapperClassId)
    }

    fun generateValue(
        random: SourceOfRandomness,
        status: GenerationStatus?
    ): Double {
        return random.nextDouble(min, max)
    }
}