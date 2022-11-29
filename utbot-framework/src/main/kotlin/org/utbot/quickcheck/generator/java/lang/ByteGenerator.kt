package org.utbot.quickcheck.generator.java.lang

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.byteWrapperClassId
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.InRange
import org.utbot.quickcheck.generator.IntegralGenerator
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `byte` or [Byte].
 */
class ByteGenerator : IntegralGenerator(listOf(Byte::class.javaObjectType)) {
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
        return generatorContext.utModelConstructor.construct(random.nextByte(min, max), byteWrapperClassId)
    }
}