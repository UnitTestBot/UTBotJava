package org.utbot.quickcheck.generator.java.lang;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.internal.Comparables;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.Collections;

import static org.utbot.framework.plugin.api.util.IdUtilKt.getCharClassId;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@code char} or {@link Character}.
 */
public class PrimitiveCharGenerator extends Generator<Character> {
    private char min = (Character) defaultValueOf(InRange.class, "minChar");
    private char max = (Character) defaultValueOf(InRange.class, "maxChar");

    public PrimitiveCharGenerator() {
        super(Collections.singletonList(char.class));
    }

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minChar()} and {@link InRange#maxChar()}, if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        min = range.min().isEmpty() ? range.minChar() : range.min().charAt(0);
        max = range.max().isEmpty() ? range.maxChar() : range.max().charAt(0);
    }

    @Override public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(random.nextChar(min, max), getCharClassId());
    }

    private boolean inRange(Character value) {
        return Comparables.inRange(min, max).test(value);
    }
}
