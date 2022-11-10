package org.utbot.quickcheck.generator.java.time

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.internal.Ranges
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness
import java.math.BigInteger
import java.time.Period
import java.time.Year

/**
 * Produces values of type [Period].
 */
class PeriodGenerator : Generator(Period::class.java) {
    private var min = Period.of(Year.MIN_VALUE, -12, -31)
    private var max = Period.of(Year.MAX_VALUE, 12, 31)

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * Periods with values of either `Period(Year#MIN_VALUE, -12, -31)`
     * or `Period(Year#MAX_VALUE, 12, 31)` as appropriate.
     *
     *
     * [InRange.format] is ignored.  Periods are always parsed
     * using formats based on the ISO-8601 period formats `PnYnMnD` and
     * `PnW`.
     *
     * @see Period.parse
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) min = Period.parse(range.min)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max = Period.parse(range.max)
        require(toBigInteger(min) <= toBigInteger(max)) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return utModelConstructor.construct(
            fromBigInteger(
                Ranges.choose(random, toBigInteger(min), toBigInteger(max))
            ),
            Period::class.id
        )
    }

    private fun toBigInteger(period: Period): BigInteger {
        return BigInteger.valueOf(period.years.toLong())
            .multiply(TWELVE)
            .add(BigInteger.valueOf(period.months.toLong()))
            .multiply(THIRTY_ONE)
            .add(BigInteger.valueOf(period.days.toLong()))
    }

    private fun fromBigInteger(period: BigInteger): Period {
        val monthsAndDays = period.divideAndRemainder(THIRTY_ONE)
        val yearsAndMonths = monthsAndDays[0].divideAndRemainder(TWELVE)
        return Period.of(
            yearsAndMonths[0].intValueExact(),
            yearsAndMonths[1].intValueExact(),
            monthsAndDays[1].intValueExact()
        )
    }

    companion object {
        private val TWELVE = BigInteger.valueOf(12)
        private val THIRTY_ONE = BigInteger.valueOf(31)
    }
}