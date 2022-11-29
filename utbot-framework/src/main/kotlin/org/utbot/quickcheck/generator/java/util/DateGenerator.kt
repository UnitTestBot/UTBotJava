package org.utbot.quickcheck.generator.java.util

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Produces values of type [Date].
 */
class DateGenerator : Generator(Date::class.java) {
    private var min: Date = Date(Long.MIN_VALUE)
    private var max = Date(Long.MAX_VALUE)

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution, down to the
     * millisecond.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * dates with milliseconds-since-the-epoch values of either
     * [Integer.MIN_VALUE] or [Long.MAX_VALUE] as appropriate.
     *
     *
     * [InRange.format] describes
     * [how the generator is to][SimpleDateFormat.parse].
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        val formatter = SimpleDateFormat(range.format)
        formatter.isLenient = false
        try {
            if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) min = formatter.parse(range.min)
            if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max = formatter.parse(range.max)
        } catch (e: ParseException) {
            throw IllegalArgumentException(e)
        }
        require(min.time <= max.time) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            Date(random.nextLong(min.time, max.time)),
            classIdForType(Date::class.java)
        )
    }
}