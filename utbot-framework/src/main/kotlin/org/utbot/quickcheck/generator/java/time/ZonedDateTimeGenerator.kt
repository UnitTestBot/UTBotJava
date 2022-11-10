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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Produces values of type [ZonedDateTime].
 */
class ZonedDateTimeGenerator : Generator(ZonedDateTime::class.java) {
    private var min = ZonedDateTime.of(Year.MIN_VALUE, 1, 1, 0, 0, 0, 0, UTC_ZONE_ID)
    private var max = ZonedDateTime.of(
        Year.MAX_VALUE, 12, 31, 23, 59, 59, 999999999, UTC_ZONE_ID
    )

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution, down to the
     * nanosecond.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * dates with values of either [java.time.Instant.MIN] or
     * [java.time.Instant.MAX] and UTC zone as appropriate.
     *
     *
     * [InRange.format] describes
     * [how the generator is to][DateTimeFormatter.ofPattern].
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        val formatter = DateTimeFormatter.ofPattern(range.format)
        if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) {
            min = ZonedDateTime.parse(range.min, formatter)
                .withZoneSameInstant(UTC_ZONE_ID)
        }
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) {
            max = ZonedDateTime.parse(range.max, formatter)
                .withZoneSameInstant(UTC_ZONE_ID)
        }
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {

        // Project the ZonedDateTime to an Instant for easy long-based
        // generation.
        return utModelConstructor.construct(
            ZonedDateTime.ofInstant(
                random.nextInstant(min.toInstant(), max.toInstant()),
                UTC_ZONE_ID
            ),
            ZonedDateTime::class.id
        )
    }

    companion object {
        private val UTC_ZONE_ID = ZoneId.of("UTC")
    }
}