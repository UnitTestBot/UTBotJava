package org.utbot.quickcheck.generator.java.time

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Produces values of type [OffsetDateTime].
 */
class OffsetDateTimeGenerator : Generator(
    OffsetDateTime::class.java
) {
    private var min = OffsetDateTime.MIN
    private var max = OffsetDateTime.MAX

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution, down to the
     * nanosecond.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * dates with values of either [OffsetDateTime.MIN] or
     * [OffsetDateTime.MAX] as appropriate.
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
            OffsetDateTime.parse(range.min, formatter)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max =
            OffsetDateTime.parse(range.max, formatter)
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {

        // Project the OffsetDateTime to an Instant for easy long-based generation.
        return utModelConstructor.construct(
            OffsetDateTime.ofInstant(
                random.nextInstant(min.toInstant(), max.toInstant()),
                UTC_ZONE_ID
            ),
            OffsetDateTime::class.id
        )
    }

    companion object {
        private val UTC_ZONE_ID = ZoneId.of("UTC")
    }
}