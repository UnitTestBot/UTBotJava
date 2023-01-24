package org.utbot.greyboxfuzzer.quickcheck.generator.java.time

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Produces values of type [Clock].
 */
class ClockGenerator : Generator(Clock::class.java) {
    private var min = Instant.MIN
    private var max = Instant.MAX

    /**
     *
     * Tells this generator to produce values within a specified
     * [minimum][InRange.min] and/or [ maximum][InRange.max], inclusive, with uniform distribution, down to the
     * nanosecond.
     *
     *
     * Instances of this class are configured using [Instant]
     * strings.
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
        val instant = random.nextInstant(min, max)
        val zoneId = UTC_ZONE_ID
        val instantModel = generatorContext.utModelConstructor.construct(instant, Instant::class.id)
        val zoneIdModel = generatorContext.utModelConstructor.construct(zoneId, ZoneId::class.id)
        val modelId =  generatorContext.utModelConstructor.computeUnusedIdAndUpdate()
        val constructorId = Clock::fixed.executableId
        return UtAssembleModel(
            id = modelId,
            classId = Clock::class.id,
            modelName = constructorId.name + "#" + modelId,
            instantiationCall = UtExecutableCallModel(null, constructorId, listOf(instantModel, zoneIdModel)),
        )
    }

    companion object {
        private val UTC_ZONE_ID = ZoneId.of("UTC")
    }
}