package org.utbot.greyboxfuzzer.quickcheck.generator.java.time

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter

/**
 * Produces values of type [MonthDay].
 */
class MonthDayGenerator : Generator(MonthDay::class.java) {
    private var min = MonthDay.of(1, 1)
    private var max = MonthDay.of(12, 31)

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * dates with values of either `MonthDay(1, 1)` or
     * `MonthDay(12, 31)` as appropriate.
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
            MonthDay.parse(range.min, formatter)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max =
            MonthDay.parse(range.max, formatter)
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {

        /* Project the MonthDay to a LocalDate for easy long-based generation.
           Any leap year will do here. */
        val minEpochDay = min.atYear(2000).toEpochDay()
        val maxEpochDay = max.atYear(2000).toEpochDay()
        val date = LocalDate.ofEpochDay(random.nextLong(minEpochDay, maxEpochDay))
        return generatorContext.utModelConstructor.construct(
            MonthDay.of(date.monthValue, date.dayOfMonth),
            MonthDay::class.id
        )
    }
}