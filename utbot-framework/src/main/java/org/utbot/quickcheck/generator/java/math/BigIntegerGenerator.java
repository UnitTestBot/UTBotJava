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

package org.utbot.quickcheck.generator.java.math;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.generator.IntegralGenerator;
import org.utbot.quickcheck.internal.Comparables;
import org.utbot.quickcheck.internal.Ranges;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Ranges.checkRange;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static java.util.function.Function.identity;

/**
 * <p>Produces values of type {@link BigInteger}.</p>
 *
 * <p>With no additional configuration, the generated values are chosen from
 * a range with a magnitude decided by
 * {@link GenerationStatus#size()}.</p>
 */
public class BigIntegerGenerator extends IntegralGenerator<BigInteger> {
    private BigInteger min;
    private BigInteger max;

    public BigIntegerGenerator() {
        super(BigInteger.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} and/or
     * {@linkplain InRange#max() maximum} inclusive, with uniform
     * distribution.</p>
     *
     * <p>If an endpoint of the range is not specified, its value takes on
     * a magnitude influenced by
     * {@link GenerationStatus#size()}.</p>

     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        if (!defaultValueOf(InRange.class, "min").equals(range.min()))
            min = new BigInteger(range.min());
        if (!defaultValueOf(InRange.class, "max").equals(range.max()))
            max = new BigInteger(range.max());
        if (min != null && max != null)
            checkRange(Ranges.Type.INTEGRAL, min, max);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        int numberOfBits = status.size() + 1;

        if (min == null && max == null)
            return UtModelGenerator.getUtModelConstructor().construct(random.nextBigInteger(numberOfBits), classIdForType(BigInteger.class));

        BigInteger minToUse = min;
        BigInteger maxToUse = max;
        if (minToUse == null)
            minToUse = maxToUse.subtract(TEN.pow(numberOfBits));
        else if (maxToUse == null)
            maxToUse = minToUse.add(TEN.pow(numberOfBits));

        return UtModelGenerator.getUtModelConstructor().construct(Ranges.choose(random, minToUse, maxToUse), classIdForType(BigInteger.class));
    }

    @Override protected Function<BigInteger, BigInteger> narrow() {
        return identity();
    }

    @Override protected Predicate<BigInteger> inRange() {
        return Comparables.inRange(min, max);
    }

    @Override protected BigInteger leastMagnitude() {
        return Comparables.leastMagnitude(min, max, ZERO);
    }

    @Override protected boolean negative(BigInteger target) {
        return target.signum() < 0;
    }

    @Override protected BigInteger negate(BigInteger target) {
        return target.negate();
    }

    @Override public BigDecimal magnitude(Object value) {
        return new BigDecimal(narrow(value));
    }
}
