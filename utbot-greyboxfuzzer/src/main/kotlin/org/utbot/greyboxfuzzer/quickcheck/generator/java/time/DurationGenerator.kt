package org.utbot.greyboxfuzzer.quickcheck.generator.java.time

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.time.Duration

/**
 * Produces values of type [Duration].
 */
class DurationGenerator : Generator(Duration::class.java) {
    private var min = Duration.ofSeconds(Long.MIN_VALUE, 0)
    private var max = Duration.ofSeconds(Long.MAX_VALUE, 999999999)

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution, down to the
     * nanosecond.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * durations with second values of either [Long.MIN_VALUE] or
     * [Long.MAX_VALUE] (with nanoseconds set to 999,999,999) as
     * appropriate.
     *
     *
     * [InRange.format] is ignored. Durations are always
     * parsed using formats based on the ISO-8601 duration format
     * `PnDTnHnMn.nS` with days considered to be exactly 24 hours.
     *
     * @see Duration.parse
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) min = Duration.parse(range.min)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max = Duration.parse(range.max)
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generatorContext.utModelConstructor.construct(random.nextDuration(min, max), Duration::class.id)
    }
}