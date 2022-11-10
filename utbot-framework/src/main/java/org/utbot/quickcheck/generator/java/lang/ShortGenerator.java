

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

import static org.utbot.framework.plugin.api.util.IdUtilKt.getShortWrapperClassId;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@code short} or {@link Short}.
 */
public class ShortGenerator extends IntegralGenerator<Short> {
    private short min = (Short) defaultValueOf(InRange.class, "minShort");
    private short max = (Short) defaultValueOf(InRange.class, "maxShort");

    public ShortGenerator() {
        super(Collections.singletonList(Short.class));
    }

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minShort()} and {@link InRange#maxShort()}, if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        min =
            range.min().isEmpty()
                ? range.minShort()
                : Short.parseShort(range.min());
        max =
            range.max().isEmpty()
                ? range.maxShort()
                : Short.parseShort(range.max());
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(random.nextShort(min, max), getShortWrapperClassId());
    }

    @Override protected Predicate<Short> inRange() {
        return Comparables.inRange(min, max);
    }

}
