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

package org.utbot.quickcheck.internal.generator;

import javassist.bytecode.ByteArray;
import org.antlr.runtime.misc.IntArray;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.ClassId;
import org.utbot.framework.plugin.api.UtArrayModel;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.framework.plugin.api.UtNullModel;
import org.utbot.framework.plugin.api.util.IdUtilKt;
import org.utbot.quickcheck.generator.*;
import org.utbot.quickcheck.internal.Lists;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Lists.removeFrom;
import static org.utbot.quickcheck.internal.Lists.shrinksOfOneItem;
import static org.utbot.quickcheck.internal.Ranges.Type.INTEGRAL;
import static org.utbot.quickcheck.internal.Ranges.checkRange;
import static org.utbot.quickcheck.internal.Reflection.annotatedComponentTypes;
import static org.utbot.quickcheck.internal.Sequences.halving;

public class ArrayGenerator extends Generator<Object> {
    private final Class<?> componentType;
    private final Generator<?> component;

    private Size lengthRange;
    private boolean distinct;

    ArrayGenerator(Class<?> componentType, Generator<?> component) {
        super(Object.class);

        this.componentType = componentType;
        this.component = component;
    }

    public Generator<?> getComponent() {
        return component;
    }

    /**
     * Tells this generator to produce values with a length within a specified
     * minimum and/or maximum, inclusive, chosen with uniform distribution.
     *
     * @param size annotation that gives the length constraints
     */
    public void configure(Size size) {
        this.lengthRange = size;
        checkRange(INTEGRAL, size.min(), size.max());
    }

    /**
     * Tells this generator to produce values which are distinct from each
     * other.
     *
     * @param distinct Generated values will be distinct if this param is not
     * null.
     */
    public void configure(Distinct distinct) {
        this.distinct = distinct != null;
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        int length = length(random, status);
        final ClassId componentTypeId = classIdForType(componentType);

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        final int modelId = modelConstructor.computeUnusedIdAndUpdate();
        final Map<Integer, UtModel> stores = new HashMap<>();
        final UtModel generatedModel = new UtArrayModel(
                modelId, getClassIdForArrayType(componentType), length, IdUtilKt.defaultValueModel(componentTypeId), stores
        );

        for (int i = 0; i < length; ++i) {
            final UtModel item = component.generate(random, status);
            stores.put(i, item);
        }

        return generatedModel;
    }
    private ClassId getClassIdForArrayType(Class<?> componentType) {
        if (int.class.equals(componentType)) {
            return new ClassId("[i", classIdForType(int.class));
        } else if (boolean.class.equals(componentType)) {
            return new ClassId("[z", classIdForType(boolean.class));
        } else if (byte.class.equals(componentType)) {
            return new ClassId("[b", classIdForType(byte.class));
        } else if (char.class.equals(componentType)) {
            return new ClassId("[c", classIdForType(char.class));
        } else if (double.class.equals(componentType)) {
            return new ClassId("[d", classIdForType(double.class));
        } else if (float.class.equals(componentType)) {
            return new ClassId("[f", classIdForType(float.class));
        } else if (long.class.equals(componentType)) {
            return new ClassId("[j", classIdForType(long.class));
        } else if (short.class.equals(componentType)) {
            return new ClassId("[s", classIdForType(short.class));
        } else {
            return new ClassId("[L", classIdForType(componentType));
        }
    }
    @Override public boolean canShrink(Object larger) {
        return larger.getClass().getComponentType() == componentType;
    }

    @Override public List<Object> doShrink(
        SourceOfRandomness random,
        Object larger) {

        int length = Array.getLength(larger);
        List<Object> asList = new ArrayList<>();
        for (int i = 0; i < length; ++i) {
            asList.add(Array.get(larger, i));
        }

        List<Object> shrinks = new ArrayList<>(removals(asList));

        @SuppressWarnings("unchecked")
        Stream<List<Object>> oneItemShrinks =
            shrinksOfOneItem(random, asList, (Shrink<Object>) component)
                .stream();
        if (distinct) {
            oneItemShrinks = oneItemShrinks.filter(Lists::isDistinct);
        }

        shrinks.addAll(
            oneItemShrinks
                .map(this::convert)
                .filter(this::inLengthRange)
                .collect(toList()));

        return shrinks;
    }

    @Override public void provide(Generators provided) {
        super.provide(provided);

        component.provide(provided);
    }

    @Override public BigDecimal magnitude(Object value) {
        int length = Array.getLength(value);
        if (length == 0)
            return ZERO;

        BigDecimal elementsMagnitude =
            IntStream.range(0, length)
                .mapToObj(i -> component.magnitude(Array.get(value, i)))
                .reduce(ZERO, BigDecimal::add);
        return BigDecimal.valueOf(length).multiply(elementsMagnitude);
    }

    @Override public void configure(AnnotatedType annotatedType) {
        super.configure(annotatedType);

        List<AnnotatedType> annotated = annotatedComponentTypes(annotatedType);
        if (!annotated.isEmpty()) {
            component.configure(annotated.get(0));
        }
    }

    private int length(SourceOfRandomness random, GenerationStatus status) {
        return lengthRange != null
            ? random.nextInt(lengthRange.min(), lengthRange.max())
            : status.size();
    }

    private boolean inLengthRange(Object items) {
        int length = Array.getLength(items);
        return lengthRange == null
            || (length >= lengthRange.min() && length <= lengthRange.max());
    }

    private List<Object> removals(List<?> items) {
        return stream(halving(items.size()).spliterator(), false)
            .map(i -> removeFrom(items, i))
            .flatMap(Collection::stream)
            .map(this::convert)
            .filter(this::inLengthRange)
            .collect(toList());
    }

    private Object convert(List<?> items) {
        Object array = Array.newInstance(componentType, items.size());
        for (int i = 0; i < items.size(); ++i) {
            Array.set(array, i, items.get(i));
        }
        return array;
    }
}
