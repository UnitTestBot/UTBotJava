package org.utbot.greyboxfuzzer.quickcheck.generator.java.math

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.DecimalGenerator
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.generator.Precision
import org.utbot.greyboxfuzzer.quickcheck.internal.Ranges
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.math.BigDecimal
import java.math.BigInteger

/**
 *
 * Produces values of type [BigDecimal].
 *
 *
 * With no additional configuration, the generated values are chosen from
 * a range with a magnitude decided by
 * [GenerationStatus.size].
 */
class BigDecimalGenerator : DecimalGenerator(BigDecimal::class.java) {
    private var min: BigDecimal? = null
    private var max: BigDecimal? = null
    private var precision: Precision? = null

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] (inclusive) and/or
     * [maximum][InRange.max] (exclusive), with uniform
     * distribution.
     *
     *
     * If an endpoint of the range is not specified, its value takes on
     * a magnitude influenced by
     * [GenerationStatus.size].
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) min = BigDecimal(range.min)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max = BigDecimal(range.max)
        if (min != null && max != null) Ranges.checkRange(Ranges.Type.FLOAT, min, max)
    }

    /**
     *
     * Tells this generator to produce values that have a specified
     * [scale][Precision.scale].
     *
     *
     * With no precision constraint and no [ min/max constraint][.configure], the scale of the generated values is
     * unspecified.
     *
     *
     * Otherwise, the scale of the generated values is set as
     * `max(0, precision.scale, range.min.scale, range.max.scale)`.
     *
     * @param configuration annotation that gives the desired scale of the
     * generated values
     */
    fun configure(configuration: Precision?) {
        precision = configuration
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        var minToUse = min
        var maxToUse = max
        val power = status.size() + 1
        if (minToUse == null && maxToUse == null) {
            maxToUse = BigDecimal.TEN.pow(power)
            minToUse = maxToUse.negate()
        }
        when {
            minToUse == null -> minToUse = maxToUse!!.subtract(BigDecimal.TEN.pow(power))
            maxToUse == null -> maxToUse = minToUse.add(BigDecimal.TEN.pow(power))
        }
        val scale = decideScale()
        val minShifted = minToUse!!.movePointRight(scale)
        val maxShifted = maxToUse!!.movePointRight(scale)
        val range = maxShifted.toBigInteger().subtract(minShifted.toBigInteger())
        var generated: BigInteger
        do {
            generated = random.nextBigInteger(range.bitLength())
        } while (generated >= range)

        return generatorContext.utModelConstructor.construct(
            minShifted.add(BigDecimal(generated)).movePointLeft(scale),
            BigDecimal::class.id
        )
    }

    private fun decideScale(): Int {
        var scale = Int.MIN_VALUE
        if (min != null) scale = scale.coerceAtLeast(min!!.scale())
        if (max != null) scale = scale.coerceAtLeast(max!!.scale())
        if (precision != null) scale = scale.coerceAtLeast(precision!!.scale)
        return scale.coerceAtLeast(0)
    }

}