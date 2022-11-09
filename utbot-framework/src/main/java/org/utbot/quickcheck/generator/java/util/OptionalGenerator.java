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
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;

import org.utbot.quickcheck.generator.ComponentizedGenerator;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.toList;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getObjectClassId;
import static org.utbot.framework.plugin.api.util.IdUtilKt.methodId;

/**
 * Produces values of type {@link Optional}.
 */
public class OptionalGenerator extends ComponentizedGenerator<Optional> {
    public OptionalGenerator() {
        super(Optional.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        double trial = random.nextDouble();
        if (trial < 0.25) {
            return UtModelGenerator.getUtModelConstructor().construct(Optional.empty(), classIdForType(Optional.class));
        }

        final UtModel value = componentGenerators().get(0).generate(random, status);

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        final ClassId classId = classIdForType(Optional.class);
        final ExecutableId constructorId = methodId(classId, "of", classId, getObjectClassId());

        final int generatedModelId = modelConstructor.computeUnusedIdAndUpdate();
        return new UtAssembleModel(
                generatedModelId,
                classId,
                constructorId.getName() + "#" + generatedModelId,
                new UtExecutableCallModel(null, constructorId, List.of(value)),
                null,
                (a) -> List.of()
        );
    }

    @Override public List<Optional> doShrink(
        SourceOfRandomness random,
        Optional larger) {

        if (!larger.isPresent())
            return new ArrayList<>();

        List<Optional> shrinks = new ArrayList<>();
        shrinks.add(Optional.empty());
        shrinks.addAll(
            componentGenerators().get(0)
                .shrink(random, larger.get())
                .stream()
                .map(Optional::of)
                .collect(toList()));
        return shrinks;
    }

    @Override public int numberOfNeededComponents() {
        return 1;
    }

    @Override public BigDecimal magnitude(Object value) {
        Optional<?> narrowed = narrow(value);

        return narrowed.map(componentGenerators().get(0)::magnitude)
            .orElse(ZERO);
    }
}
