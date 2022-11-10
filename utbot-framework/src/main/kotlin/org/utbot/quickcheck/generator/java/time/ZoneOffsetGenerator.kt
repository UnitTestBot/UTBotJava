package org.utbot.quickcheck.generator.java.time

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness
import java.time.ZoneOffset

/**
 * Produces values of type [ZoneOffset].
 */
class ZoneOffsetGenerator : Generator(ZoneOffset::class.java) {
    /* The way ZoneOffsets work, ZoneOffset.MAX (-18:00) is actually
       the lower end of the seconds range, whereas ZoneOffset.MIN (+18:00)
       is the upper end. */
    private var min = ZoneOffset.MAX
    private var max = ZoneOffset.MIN

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * ZoneOffsets with values of either `ZoneOffset#MIN` or
     * `ZoneOffset#MAX` as appropriate.
     *
     *
     * [InRange.format] is ignored. ZoneOffsets are always
     * parsed using their zone id.
     *
     * @see ZoneOffset.of
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) min = ZoneOffset.of(range.min)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max = ZoneOffset.of(range.max)
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val minSeconds = min.totalSeconds
        val maxSeconds = max.totalSeconds

        return utModelConstructor.construct(
            ZoneOffset.ofTotalSeconds(
                if (minSeconds <= maxSeconds) random.nextInt(minSeconds, maxSeconds) else random.nextInt(
                    maxSeconds,
                    minSeconds
                )
            ),
            ZoneOffset::class.id
        )
    }
}