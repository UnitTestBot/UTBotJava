package org.utbot.quickcheck.generator.java.time

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Produces values of type [LocalTime].
 */
class LocalTimeGenerator : Generator(LocalTime::class.java) {
    private var min = LocalTime.MIN
    private var max = LocalTime.MAX

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution, down to the
     * nanosecond.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * times with values of either [LocalTime.MIN] or
     * [LocalTime.MAX] as appropriate.
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
            LocalTime.parse(range.min, formatter)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max =
            LocalTime.parse(range.max, formatter)
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            LocalTime.ofNanoOfDay(
                random.nextLong(min.toNanoOfDay(), max.toNanoOfDay())
            ),
            LocalTime::class.id
        )
    }
}