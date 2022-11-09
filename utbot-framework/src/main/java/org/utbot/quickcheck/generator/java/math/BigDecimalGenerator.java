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
import org.utbot.quickcheck.generator.DecimalGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.generator.Precision;
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
import static java.lang.Math.max;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static java.util.function.Function.identity;

/**
 * <p>Produces values of type {@link BigDecimal}.</p>
 *
 * <p>With no additional configuration, the generated values are chosen from
 * a range with a magnitude decided by
 * {@link GenerationStatus#size()}.</p>
 */
public class BigDecimalGenerator extends DecimalGenerator<BigDecimal> {
    private BigDecimal min;
    private BigDecimal max;
    private Precision precision;

    public BigDecimalGenerator() {
        super(BigDecimal.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} (inclusive) and/or
     * {@linkplain InRange#max() maximum} (exclusive), with uniform
     * distribution.</p>
     *
     * <p>If an endpoint of the range is not specified, its value takes on
     * a magnitude influenced by
     * {@link GenerationStatus#size()}.</p>
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        if (!defaultValueOf(InRange.class, "min").equals(range.min()))
            min = new BigDecimal(range.min());
        if (!defaultValueOf(InRange.class, "max").equals(range.max()))
            max = new BigDecimal(range.max());

        if (min != null && max != null)
            checkRange(Ranges.Type.FLOAT, min, max);
    }

    /**
     * <p>Tells this generator to produce values that have a specified
     * {@linkplain Precision#scale() scale}.</p>
     *
     * <p>With no precision constraint and no {@linkplain #configure(InRange)
     * min/max constraint}, the scale of the generated values is
     * unspecified.</p>
     *
     * <p>Otherwise, the scale of the generated values is set as
     * {@code max(0, precision.scale, range.min.scale, range.max.scale)}.</p>
     *
     * @param configuration annotation that gives the desired scale of the
     *                      generated values
     */
    public void configure(Precision configuration) {
        precision = configuration;
    }

    @Override
    public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

        BigDecimal minToUse = min;
        BigDecimal maxToUse = max;
        int power = status.size() + 1;

        if (minToUse == null && maxToUse == null) {
            maxToUse = TEN.pow(power);
            minToUse = maxToUse.negate();
        }

        if (minToUse == null)
            minToUse = maxToUse.subtract(TEN.pow(power));
        else if (maxToUse == null)
            maxToUse = minToUse.add(TEN.pow(power));

        int scale = decideScale();

        BigDecimal minShifted = minToUse.movePointRight(scale);
        BigDecimal maxShifted = maxToUse.movePointRight(scale);
        BigInteger range =
                maxShifted.toBigInteger().subtract(minShifted.toBigInteger());

        BigInteger generated;
        do {
            generated = random.nextBigInteger(range.bitLength());
        } while (generated.compareTo(range) >= 0);


        return UtModelGenerator.getUtModelConstructor().construct(minShifted.add(new BigDecimal(generated))
                .movePointLeft(scale), classIdForType(BigDecimal.class));
    }

    private int decideScale() {
        int scale = Integer.MIN_VALUE;

        if (min != null)
            scale = max(scale, min.scale());
        if (max != null)
            scale = max(scale, max.scale());
        if (precision != null)
            scale = max(scale, precision.scale());

        return max(scale, 0);
    }

    @Override
    protected Function<BigDecimal, BigDecimal> widen() {
        return identity();
    }

    @Override
    protected Function<BigDecimal, BigDecimal> narrow() {
        return identity();
    }

    @Override
    protected Predicate<BigDecimal> inRange() {
        return Comparables.inRange(min, max);
    }

    @Override
    protected BigDecimal leastMagnitude() {
        return Comparables.leastMagnitude(min, max, ZERO);
    }

    @Override
    protected boolean negative(BigDecimal target) {
        return target.signum() < 0;
    }

    @Override
    protected BigDecimal negate(BigDecimal target) {
        return target.negate();
    }

    @Override
    public BigDecimal magnitude(Object value) {
        return narrow(value);
    }
}
