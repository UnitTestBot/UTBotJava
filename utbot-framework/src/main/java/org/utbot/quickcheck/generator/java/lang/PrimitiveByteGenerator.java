
package org.utbot.quickcheck.generator.java.lang;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.generator.IntegralGenerator;
import org.utbot.quickcheck.internal.Comparables;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.Collections;
import java.util.function.Predicate;

import static org.utbot.framework.plugin.api.util.IdUtilKt.getByteClassId;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@code byte} or {@link Byte}.
 */
public class PrimitiveByteGenerator extends IntegralGenerator<Byte> {
    private byte min = (Byte) defaultValueOf(InRange.class, "minByte");
    private byte max = (Byte) defaultValueOf(InRange.class, "maxByte");

    public PrimitiveByteGenerator() {
        super(Collections.singletonList(byte.class));
    }

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minByte()} and {@link InRange#maxByte()}, if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        min =
                range.min().isEmpty()
                        ? range.minByte()
                        : Byte.parseByte(range.min());
        max =
                range.max().isEmpty()
                        ? range.maxByte()
                        : Byte.parseByte(range.max());
    }

    @Override public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(random.nextByte(min, max), getByteClassId());
    }

    @Override protected Predicate<Byte> inRange() {
        return Comparables.inRange(min, max);
    }

}
