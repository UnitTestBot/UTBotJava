

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

import static org.utbot.framework.plugin.api.util.IdUtilKt.getIntClassId;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@code int} or {@link Integer}.
 */
public class PrimitiveIntGenerator extends IntegralGenerator<Integer> {
    private int min = (Integer) defaultValueOf(InRange.class, "minInt");
    private int max = (Integer) defaultValueOf(InRange.class, "maxInt");

    public PrimitiveIntGenerator() {
        super(Collections.singletonList(int.class));
    }

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minInt()} and {@link InRange#maxInt()}, if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        min =
                range.min().isEmpty()
                        ? range.minInt()
                        : Integer.parseInt(range.min());
        max =
                range.max().isEmpty()
                        ? range.maxInt()
                        : Integer.parseInt(range.max());
    }

    @Override public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {
        return UtModelGenerator.getUtModelConstructor().construct(generateValue(random, status), getIntClassId());
    }

    public int generateValue(SourceOfRandomness random,
                             GenerationStatus status) {
        return random.nextInt(min, max);
    }

    @Override protected Predicate<Integer> inRange() {
        return Comparables.inRange(min, max);
    }

}
