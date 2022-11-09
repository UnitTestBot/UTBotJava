/*
 The MIT License

 Copyright (c) 2010-2021 Paul R. Holser, Jr.

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.utbot.quickcheck.generator.java.lang;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.generator.IntegralGenerator;
import org.utbot.quickcheck.internal.Comparables;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.utbot.framework.plugin.api.util.IdUtilKt.getIntClassId;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;
import static java.util.Arrays.asList;

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

    @Override protected Function<BigInteger, Integer> narrow() {
        return BigInteger::intValue;
    }

    @Override protected Predicate<Integer> inRange() {
        return Comparables.inRange(min, max);
    }

    @Override protected Integer leastMagnitude() {
        return Comparables.leastMagnitude(min, max, 0);
    }

    @Override protected boolean negative(Integer target) {
        return target < 0;
    }

    @Override protected Integer negate(Integer target) {
        return -target;
    }

    @Override public BigDecimal magnitude(Object value) {
        return BigDecimal.valueOf(narrow(value));
    }
}
