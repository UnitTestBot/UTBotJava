package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.lang

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.DecimalGenerator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `float` or [Float].
 */
class PrimitiveFloatGenerator : DecimalGenerator(listOf(Float::class.javaPrimitiveType!!)) {
    private var min = Reflection.defaultValueOf(InRange::class.java, "minFloat") as Float
    private var max = Reflection.defaultValueOf(InRange::class.java, "maxFloat") as Float

    /**
     * Tells this generator to produce values within a specified minimum
     * (inclusive) and/or maximum (exclusive) with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minFloat] and [InRange.maxFloat], if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        min = if (range.min.isEmpty()) range.minFloat else range.min.toFloat()
        max = if (range.max.isEmpty()) range.maxFloat else range.max.toFloat()
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(random.nextFloat(min, max), floatClassId)
    }
}