package org.utbot.greyboxfuzzer.quickcheck.generator.java.time

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.time.Instant

/**
 * Produces values of type [Instant].
 */
class InstantGenerator : Generator(Instant::class.java) {
    private var min = Instant.MIN
    private var max = Instant.MAX

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution, down to the
     * nanosecond.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * instants with values of either [Instant.MIN] or
     * [Instant.MAX] as appropriate.
     *
     *
     * [InRange.format] is ignored. Instants are always
     * parsed using [java.time.format.DateTimeFormatter.ISO_INSTANT].
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) min = Instant.parse(range.min)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max = Instant.parse(range.max)
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(random.nextInstant(min, max), Instant::class.id)
    }
}