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

package org.utbot.quickcheck.generator.java.util;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.framework.plugin.api.UtNullModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.generator.java.lang.LongGenerator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.math.BigDecimal;
import java.util.*;

import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.toList;
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

    @Override public List<OptionalLong> doShrink(
        SourceOfRandomness random,
        OptionalLong larger) {

        if (!larger.isPresent())
            return new ArrayList<>();

        List<OptionalLong> shrinks = new ArrayList<>();
        shrinks.add(OptionalLong.empty());
        shrinks.addAll(
            longs.shrink(random, larger.getAsLong())
                .stream()
                .map(OptionalLong::of)
                .collect(toList()));
        return shrinks;
    }

    @Override public BigDecimal magnitude(Object value) {
        OptionalLong narrowed = narrow(value);

        return narrowed.isPresent()
            ? BigDecimal.valueOf(narrowed.getAsLong())
            : ZERO;
    }
}
