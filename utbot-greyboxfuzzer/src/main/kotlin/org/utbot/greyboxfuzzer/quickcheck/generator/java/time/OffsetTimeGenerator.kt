package org.utbot.greyboxfuzzer.quickcheck.generator.java.time

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Produces values of type [OffsetTime].
 */
class OffsetTimeGenerator : Generator(OffsetTime::class.java) {
    private var min = OffsetTime.MIN
    private var max = OffsetTime.MAX

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution, down to the
     * nanosecond.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * times with values of either [OffsetTime.MIN] or
     * [OffsetTime.MAX] as appropriate.
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
            OffsetTime.parse(range.min, formatter)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max =
            OffsetTime.parse(range.max, formatter)
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val time = LocalTime.ofNanoOfDay(
            random.nextLong(
                min.withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalTime()
                    .toNanoOfDay(),
                max.withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalTime()
                    .toNanoOfDay()
            )
        )
        return generatorContext.utModelConstructor.construct(
            OffsetTime.of(time, ZoneOffset.UTC),
            OffsetTime::class.id
        )
    }
}