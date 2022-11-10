
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

import static org.utbot.framework.plugin.api.util.IdUtilKt.getDoubleClassId;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values for property parameters of type {@code double} or
 * {@link Double}.
 */
public class PrimitiveDoubleGenerator extends DecimalGenerator<Double> {
    private double min = (Double) defaultValueOf(InRange.class, "minDouble");
    private double max = (Double) defaultValueOf(InRange.class, "maxDouble");

    public PrimitiveDoubleGenerator() {
        super(Collections.singletonList(double.class));
    }

    /**
     * Tells this generator to produce values within a specified minimum
     * (inclusive) and/or maximum (exclusive) with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minDouble()} and {@link InRange#maxDouble()},
     * if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        min =
                range.min().isEmpty()
                        ? range.minDouble()
                        : Double.parseDouble(range.min());
        max =
                range.max().isEmpty()
                        ? range.maxDouble()
                        : Double.parseDouble(range.max());
    }

    @Override public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(generateValue(random, status), getDoubleClassId());
    }

    public double generateValue(SourceOfRandomness random,
                                GenerationStatus status) {
        return random.nextDouble(min, max);
    }

    @Override protected Predicate<Double> inRange() {
        return Comparables.inRange(min, max);
    }

}
