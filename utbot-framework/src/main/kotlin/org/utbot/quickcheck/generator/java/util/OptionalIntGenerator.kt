

package org.utbot.quickcheck.generator.java.util;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.generator.java.lang.IntegerGenerator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.OptionalInt;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

/**
 * Produces values of type {@link OptionalInt}.
 */
public class OptionalIntGenerator extends Generator<OptionalInt> {
    private final IntegerGenerator integers = new IntegerGenerator();

    public OptionalIntGenerator() {
        super(OptionalInt.class);
    }

    /**
     * Tells this generator to produce values, when
     * {@link OptionalInt#isPresent() present}, within a specified minimum
     * and/or maximum, inclusive, with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minInt()} and {@link InRange#maxInt()}, if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        integers.configure(range);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        double trial = random.nextDouble();
        final OptionalInt generated = trial < 0.25 ?
                OptionalInt.empty()
                : OptionalInt.of(integers.generateValue(random, status));

        return UtModelGenerator.getUtModelConstructor().construct(generated, classIdForType(OptionalInt.class));
    }

}
