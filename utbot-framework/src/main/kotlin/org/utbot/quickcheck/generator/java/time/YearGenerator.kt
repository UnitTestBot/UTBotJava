package org.utbot.quickcheck.generator.java.time

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness
import java.time.Year
import java.time.format.DateTimeFormatter

/**
 * Produces values of type [Year].
 */
class YearGenerator : Generator(Year::class.java) {
    private var min = Year.of(Year.MIN_VALUE)
    private var max = Year.of(Year.MAX_VALUE)

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution.
     *
     *
     * If an endpoint of the range is not specified, the generator will use
     * Years with values of either `Year#MIN_VALUE` or
     * `Year#MAX_VALUE` as appropriate.
     *
     *
     * [InRange.format] describes
     * [how the generator is to][DateTimeFormatter.ofPattern].
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        val formatter = DateTimeFormatter.ofPattern(range.format)
        if (Reflection.defaultValueOf(InRange::class.java, "min") != range.min) min = Year.parse(range.min, formatter)
        if (Reflection.defaultValueOf(InRange::class.java, "max") != range.max) max = Year.parse(range.max, formatter)
        require(min <= max) { String.format("bad range, %s > %s", min, max) }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            Year.of(random.nextInt(min.value, max.value)),
            Year::class.id
        )
    }
}