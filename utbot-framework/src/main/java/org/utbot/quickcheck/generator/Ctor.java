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

package org.utbot.quickcheck.generator;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.external.api.UtModelFactoryKt;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.Generators;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.instantiate;
import static org.utbot.quickcheck.internal.Reflection.singleAccessibleConstructor;

/**
 * <p>Produces instances of a class by generating values for the parameters of
 * one of the constructors on the class, and invoking the constructor.</p>
 *
 * <p>If a constructor parameter is marked with an annotation that influences
 * the generation of a given kind of value, that annotation will be applied to
 * the generation of values for that parameter.</p>
 *
 * <p>This generator is intended to be used with
 * {@link org.utbot.quickcheck.From}, and not to be available via the
 * {@link java.util.ServiceLoader} mechanism.</p>
 *
 * @param <T> the type of objects generated
 */
public class Ctor<T> extends Generator<T> {
    private final Constructor<T> ctor;
    private final Parameter[] parameters;
    private final List<Generator<?>> parameterGenerators = new ArrayList<>();

    /**
     * Reflects the given type for a single accessible constructor to be used
     * to generate values of that type.
     *
     * @param type the type of objects to be generated
     */
    public Ctor(Class<T> type) {
        this(singleAccessibleConstructor(type));
    }

    /**
     * Uses the given constructor to generate values of the declaring type.
     *
     * @param ctor the constructor to reflect on to generate constructor
     * parameter values
     */
    public Ctor(Constructor<T> ctor) {
        super(ctor.getDeclaringClass());

        this.ctor = ctor;
        this.parameters = ctor.getParameters();
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {
        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();

        final ClassId classId = classIdForType(ctor.getDeclaringClass());
        final List<ClassId> argumentTypes = Arrays.stream(ctor.getParameterTypes())
                .map(UtModelFactoryKt::classIdForType)
                .collect(Collectors.toList());

        final ExecutableId constructorId = new ConstructorId(classId, argumentTypes);

        final int generatedModelId = modelConstructor.computeUnusedIdAndUpdate();

        final List<UtModel> args = arguments(random, status);
        final UtAssembleModel generatedModel = new UtAssembleModel(
                generatedModelId,
                classId,
                constructorId.getName() + "#" + generatedModelId,
                new UtExecutableCallModel(null, constructorId, args),
                null,
                (a) -> List.of()
        );

        return generatedModel;
    }

    @Override public void provide(Generators provided) {
        super.provide(provided);

        parameterGenerators.clear();
        for (Parameter each : parameters) {
            parameterGenerators.add(gen().parameter(each));
        }
    }

    @Override public void configure(AnnotatedType annotatedType) {
        super.configure(annotatedType);

        for (int i = 0; i < parameters.length; ++i) {
            parameterGenerators.get(i)
                .configure(parameters[i].getAnnotatedType());
        }
    }

    @Override public Ctor<T> copy() {
        return new Ctor<>(ctor);
    }

    private List<UtModel> arguments(
        SourceOfRandomness random,
        GenerationStatus status) {

        UtModel[] args = new UtModel[parameters.length];

        for (int i = 0; i < args.length; ++i) {
            args[i] = parameterGenerators.get(i).generate(random, status);
        }

        return List.of(args);
    }
}
