package org.utbot.quickcheck.generator.java.lang;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.DecimalGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.internal.Comparables;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.Collections;
import java.util.function.Predicate;

import static org.utbot.framework.plugin.api.util.IdUtilKt.getFloatWrapperClassId;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@code float} or {@link Float}.
 */
public class FloatGenerator extends DecimalGenerator<Float> {
    private float min = (Float) defaultValueOf(InRange.class, "minFloat");
    private float max = (Float) defaultValueOf(InRange.class, "maxFloat");

    public FloatGenerator() {
        super(Collections.singletonList(Float.class));
    }

    /**
     * Tells this generator to produce values within a specified minimum
     * (inclusive) and/or maximum (exclusive) with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minFloat()} and {@link InRange#maxFloat()}, if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        min =
            range.min().isEmpty()
                ? range.minFloat()
                : Float.parseFloat(range.min());
        max =
            range.max().isEmpty()
                ? range.maxFloat()
                : Float.parseFloat(range.max());
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(random.nextFloat(min, max), getFloatWrapperClassId());
    }

    @Override protected Predicate<Float> inRange() {
        return Comparables.inRange(min, max);
    }

}
