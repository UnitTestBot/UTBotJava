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
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.framework.plugin.api.UtNullModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.Generators;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.*;
import static java.util.stream.Collectors.toList;

/**
 * <p>Produces instances of a class by reflecting the class's fields and
 * generating random values for them.</p>
 *
 * <p>All fields of the class and its class hierarchy are auto-generated.</p>
 *
 * <p>In order for this generator to work, the type it is given must have an
 * accessible zero-arg constructor.</p>
 *
 * <p>If a field is marked with an annotation that influences the generation of
 * a given kind of value, that annotation will be applied to the generation of
 * values for that field.</p>
 *
 * <p>This generator is intended to be used with
 * {@link org.utbot.quickcheck.From}, and not to be available via the
 * {@link java.util.ServiceLoader} mechanism.</p>
 *
 * @param <T> the type of objects generated
 */
public class Fields<T> extends org.utbot.quickcheck.generator.Generator<T> {
    private final List<Field> fields;
    private final List<org.utbot.quickcheck.generator.Generator<?>> fieldGenerators = new ArrayList<>();

    /**
     * @param type the type of objects to be generated
     */
    public Fields(Class<T> type) {
        super(type);

        this.fields =
            allDeclaredFieldsOf(type).stream()
                .filter(f -> !Modifier.isFinal(f.getModifiers()))
                .collect(toList());

        instantiate(type);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        Class<T> type = types().get(0);
        Object generated = instantiate(type);

        for (int i = 0; i < fields.size(); i++) {
            setField(
                fields.get(i),
                generated,
                fieldGenerators.get(i).generate(random, status),
                true);
        }

        //return UtModelGenerator.getUtModelConstructor().construct(type.cast(generated),);
        return new UtNullModel(classIdForType(type));
    }

    @Override public void provide(Generators provided) {
        super.provide(provided);

        fieldGenerators.clear();
        for (Field each : fields) {
            fieldGenerators.add(gen().field(each));
        }
    }

    @Override public void configure(AnnotatedType annotatedType) {
        super.configure(annotatedType);

        for (int i = 0; i < fields.size(); ++i) {
            fieldGenerators.get(i).configure(
                fields.get(i).getAnnotatedType());
        }
    }

    @Override public Generator<T> copy() {
        return new Fields<>(types().get(0));
    }
}
