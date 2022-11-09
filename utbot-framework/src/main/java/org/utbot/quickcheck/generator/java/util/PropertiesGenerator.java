package org.utbot.quickcheck.generator.java.util;

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


import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.java.lang.AbstractStringGenerator;
import org.utbot.quickcheck.generator.java.lang.Encoded;
import org.utbot.quickcheck.generator.java.lang.StringGenerator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.math.BigDecimal;
import java.util.*;

import static java.math.BigDecimal.ZERO;
import static java.util.Arrays.asList;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getObjectClassId;
import static org.utbot.framework.plugin.api.util.IdUtilKt.methodId;

/**
 * Produces values of type {@link Properties}.
 */
public class PropertiesGenerator extends Generator<Properties> {
    private AbstractStringGenerator stringGenerator = new StringGenerator();

    public PropertiesGenerator() {
        super(Properties.class);
    }

    public void configure(Encoded.InCharset charset) {
        Encoded encoded = new Encoded();
        encoded.configure(charset);
        stringGenerator = encoded;
    }

    @Override
    public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

        int size = status.size();

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        final ClassId classId = classIdForType(Properties.class);

        final ExecutableId constructorId = new ConstructorId(classId, List.of());
        final int generatedModelId = modelConstructor.computeUnusedIdAndUpdate();

        final UtAssembleModel generatedModel = new UtAssembleModel(
                generatedModelId,
                classId,
                constructorId.getName() + "#" + generatedModelId,
                new UtExecutableCallModel(null, constructorId, List.of()),
                null,
                (a) -> {
                    final List<UtStatementModel> modificationChain = new ArrayList<>();
                    final ExecutableId setPropertyMethodId = methodId(classId, "setProperty", getObjectClassId(), getObjectClassId(), getObjectClassId());

                    for (int i = 0; i < size; i++) {
                        final UtModel key = stringGenerator.generate(random, status);
                        final UtModel value = stringGenerator.generate(random, status);
                        modificationChain.add(new UtExecutableCallModel(a, setPropertyMethodId, List.of(key, value)));
                    }
                    return modificationChain;
                }
        );

        return generatedModel;
    }

    @Override public boolean canRegisterAsType(Class<?> type) {
        Set<Class<?>> exclusions =
            new HashSet<>(
                asList(
                    Object.class,
                    Hashtable.class,
                    Map.class,
                    Dictionary.class));
        return !exclusions.contains(type);
    }

    @Override public BigDecimal magnitude(Object value) {
        Properties narrowed = narrow(value);

        if (narrowed.isEmpty())
            return ZERO;

        BigDecimal keysMagnitude =
            narrowed.keySet().stream()
                .map(e -> stringGenerator.magnitude(e))
                .reduce(ZERO, BigDecimal::add);
        BigDecimal valuesMagnitude =
            narrowed.values().stream()
                .map(e -> stringGenerator.magnitude(e))
                .reduce(ZERO, BigDecimal::add);
        return BigDecimal.valueOf(narrowed.size())
            .multiply(keysMagnitude)
            .add(valuesMagnitude);
    }
}
