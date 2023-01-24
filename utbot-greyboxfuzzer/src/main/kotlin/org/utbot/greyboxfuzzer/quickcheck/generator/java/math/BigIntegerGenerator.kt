package org.utbot.greyboxfuzzer.quickcheck.generator.java.math

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.generator.IntegralGenerator
import org.utbot.greyboxfuzzer.quickcheck.internal.Ranges
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.math.BigInteger

/**
 *
 * Produces values of type [BigInteger].
 *
 *
 * With no additional configuration, the generated values are chosen from
 * a range with a magnitude decided by
 * [GenerationStatus.size].
 */
class BigIntegerGenerator : IntegralGenerator(BigInteger::class.java) {
    private var min: BigInteger? = null
    private var max: BigInteger? = null

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or
     * [maximum][InRange.max] inclusive, with uniform
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
        if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) min = BigInteger(range.min)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max = BigInteger(range.max)
        if (min != null && max != null) Ranges.checkRange(Ranges.Type.INTEGRAL, min, max)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val numberOfBits = status.size() + 1
        if (min == null && max == null)
            return generatorContext.utModelConstructor.construct(
                random.nextBigInteger(numberOfBits),
                BigInteger::class.id
            )

        val minToUse = min ?: max!!.subtract(BigInteger.TEN.pow(numberOfBits))
        val maxToUse = max ?: minToUse.add(BigInteger.TEN.pow(numberOfBits))
        return generatorContext.utModelConstructor.construct(
            Ranges.choose(random, minToUse, maxToUse),
            BigInteger::class.id
        )
    }

}