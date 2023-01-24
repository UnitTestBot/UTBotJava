package org.utbot.greyboxfuzzer.quickcheck.generator.java.lang

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.InRange
import org.utbot.greyboxfuzzer.quickcheck.generator.IntegralGenerator
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `byte` or [Byte].
 */
class PrimitiveByteGenerator : IntegralGenerator(listOf(Byte::class.javaPrimitiveType!!)) {
    private var min = Reflection.defaultValueOf(InRange::class.java, "minByte") as Byte
    private var max = Reflection.defaultValueOf(InRange::class.java, "maxByte") as Byte

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * [InRange.min] and [InRange.max] take precedence over
     * [InRange.minByte] and [InRange.maxByte], if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    fun configure(range: InRange) {
        min = if (range.min.isEmpty()) range.minByte else range.min.toByte()
        max = if (range.max.isEmpty()) range.maxByte else range.max.toByte()
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(random.nextByte(min, max), byteClassId)
    }
}