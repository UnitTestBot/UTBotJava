

package org.utbot.quickcheck.generator.java.util;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.generator.java.lang.LongGenerator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.*;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

/**
 * Produces values of type {@link OptionalLong}.
 */
public class    OptionalLongGenerator extends Generator<OptionalLong> {
    private final LongGenerator longs = new LongGenerator();

    public OptionalLongGenerator() {
        super(OptionalLong.class);
    }

    /**
     * Tells this generator to produce values, when
     * {@link OptionalLong#isPresent() present}, within a specified minimum
     * and/or maximum, inclusive, with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minLong()} and {@link InRange#maxLong()}, if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        longs.configure(range);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        double trial = random.nextDouble();
        final OptionalLong generated = trial < 0.25 ?
                OptionalLong.empty()
                : OptionalLong.of(longs.generateValue(random, status));

        return UtModelGenerator.getUtModelConstructor().construct(generated, classIdForType(OptionalLong.class));
    }

}
