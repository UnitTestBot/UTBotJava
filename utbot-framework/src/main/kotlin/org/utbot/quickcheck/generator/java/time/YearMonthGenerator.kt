package org.utbot.quickcheck.generator.java.time

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Produces values of type [YearMonth].
 */
class YearMonthGenerator : Generator(YearMonth::class.java) {
    private var min = YearMonth.of(Year.MIN_VALUE, 1)
    private var max = YearMonth.of(Year.MAX_VALUE, 12)

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * dates with values of either `YearMonth(Year#MIN_VALUE, 1)` or
     * `YearMonth(Year#MAX_VALUE, 12)` as appropriate.
     *
     *
     * [InRange.format] describes
     * [how the generator is to][DateTimeFormatter.ofPattern].
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        val formatter = DateTimeFormatter.ofPattern(range.format)
        if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) min =
            YearMonth.parse(range.min, formatter)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max =
            YearMonth.parse(range.max, formatter)
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val generated = random.nextLong(
            min.year * 12L + min.monthValue - 1,
            max.year * 12L + max.monthValue - 1
        )
        return utModelConstructor.construct(
            YearMonth.of(
                (generated / 12).toInt(),
                abs(generated % 12).toInt() + 1
            ),
            YearMonth::class.id
        )
    }
}