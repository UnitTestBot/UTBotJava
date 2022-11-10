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

import static org.utbot.framework.plugin.api.util.IdUtilKt.getLongWrapperClassId;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@code long} or {@link Long}.
 */
public class LongGenerator extends IntegralGenerator<Long> {
    private long min = (Long) defaultValueOf(InRange.class, "minLong");
    private long max = (Long) defaultValueOf(InRange.class, "maxLong");

    public LongGenerator() {
        super(Collections.singletonList(Long.class));
    }

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minLong()} and {@link InRange#maxLong()}, if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        min =
            range.min().isEmpty()
                ? range.minLong()
                : Long.parseLong(range.min());
        max =
            range.max().isEmpty()
                ? range.maxLong()
                : Long.parseLong(range.max());
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(generateValue(random, status), getLongWrapperClassId());
    }

    public long generateValue(SourceOfRandomness random,
                             GenerationStatus status) {
        return random.nextLong(min, max);
    }

    @Override protected Predicate<Long> inRange() {
        return Comparables.inRange(min, max);
    }

}
