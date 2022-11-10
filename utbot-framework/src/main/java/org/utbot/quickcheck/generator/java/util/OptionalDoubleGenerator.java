

package org.utbot.quickcheck.generator.java.util;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.generator.java.lang.DoubleGenerator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.OptionalDouble;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

/**
 * Produces values of type {@link OptionalDouble}.
 */
public class OptionalDoubleGenerator extends Generator<OptionalDouble> {
    private final DoubleGenerator doubles = new DoubleGenerator();

    public OptionalDoubleGenerator() {
        super(OptionalDouble.class);
    }

    /**
     * Tells this generator to produce values, when
     * {@link OptionalDouble#isPresent() present}, within a specified minimum
     * (inclusive) and/or maximum (exclusive) with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minDouble()} and {@link InRange#maxDouble()},
     * if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        doubles.configure(range);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        double trial = random.nextDouble();
        final OptionalDouble generated = trial < 0.25 ?
                OptionalDouble.empty()
                : OptionalDouble.of(doubles.generateValue(random, status));

        return UtModelGenerator.getUtModelConstructor().construct(generated, classIdForType(OptionalDouble.class));
    }

}
