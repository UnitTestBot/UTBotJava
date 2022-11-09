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
import org.utbot.quickcheck.generator.*;
import org.utbot.quickcheck.internal.Lists;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getObjectClassId;
import static org.utbot.framework.plugin.api.util.IdUtilKt.methodId;
import static org.utbot.quickcheck.internal.Lists.removeFrom;
import static org.utbot.quickcheck.internal.Lists.shrinksOfOneItem;
import static org.utbot.quickcheck.internal.Ranges.Type.INTEGRAL;
import static org.utbot.quickcheck.internal.Ranges.checkRange;
import static org.utbot.quickcheck.internal.Reflection.findConstructor;
import static org.utbot.quickcheck.internal.Reflection.instantiate;
import static org.utbot.quickcheck.internal.Sequences.halving;

/**
 * <p>Base class for generators of {@link Map}s.</p>
 *
 * <p>The generated map has a number of entries limited by
 * {@link GenerationStatus#size()}, or else by the attributes of a {@link Size}
 * marking. The individual keys and values will have types corresponding to the
 * property parameter's type arguments.</p>
 *
 * @param <T> the type of map generated
 */
public abstract class MapGenerator<T extends Map>
    extends ComponentizedGenerator<T> {

    private Size sizeRange;
    private boolean distinct;

    protected MapGenerator(Class<T> type) {
        super(type);
    }

    /**
     * <p>Tells this generator to add key-value pairs to the generated map a
     * number of times within a specified minimum and/or maximum, inclusive,
     * chosen with uniform distribution.</p>
     *
     * <p>Note that maps disallow duplicate keys, so the number of pairs added
     * may not be equal to the map's {@link Map#size()}.</p>
     *
     * @param size annotation that gives the size constraints
     */
    public void configure(Size size) {
        this.sizeRange = size;
        checkRange(INTEGRAL, size.min(), size.max());
    }

    /**
     * Tells this generator to add entries whose keys are distinct from
     * each other.
     *
     * @param distinct Keys of generated entries will be distinct if this
     * param is not null
     */
    public void configure(Distinct distinct) {
        this.distinct = distinct != null;
    }

    @SuppressWarnings("unchecked")
    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        int size = size(random, status);

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        final ClassId classId = classIdForType(types().get(0));
        final Generator<?> keyGenerator = componentGenerators().get(0);
        final Generator<?> valueGenerator = componentGenerators().get(1);

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
                    final ExecutableId putMethodId = methodId(classId, "put", getObjectClassId(), getObjectClassId(), getObjectClassId());

                    int i = 0;
                    while (i < size) {
                        final UtModel key = keyGenerator.generate(random, status);
                        final UtModel value = valueGenerator.generate(random, status);
                        if (!okToAdd(key, value)) continue;
                        i++;
                        modificationChain.add(new UtExecutableCallModel(a, putMethodId, List.of(key, value)));
                    }
                    return modificationChain;
                }
        );



        return generatedModel;
    }

    @Override public List<T> doShrink(SourceOfRandomness random, T larger) {
        @SuppressWarnings("unchecked")
        List<Entry<?, ?>> entries = new ArrayList<>(larger.entrySet());

        List<T> shrinks = new ArrayList<>(removals(entries));

        @SuppressWarnings("unchecked")
        Shrink<Entry<?, ?>> entryShrink = entryShrinker(
            (Shrink<Object>) componentGenerators().get(0),
            (Shrink<Object>) componentGenerators().get(1));

        Stream<List<Entry<?, ?>>> oneEntryShrinks =
            shrinksOfOneItem(random, entries, entryShrink)
                .stream();
        if (distinct)
            oneEntryShrinks = oneEntryShrinks.filter(MapGenerator::isKeyDistinct);

        shrinks.addAll(
            oneEntryShrinks
                .map(this::convert)
                .filter(this::inSizeRange)
                .collect(toList()));

        return shrinks;
    }

    @Override public int numberOfNeededComponents() {
        return 2;
    }

    @Override public BigDecimal magnitude(Object value) {
        Map<?, ?> narrowed = narrow(value);

        if (narrowed.isEmpty())
            return BigDecimal.ZERO;

        BigDecimal keysMagnitude =
            narrowed.keySet().stream()
                .map(e -> componentGenerators().get(0).magnitude(e))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valuesMagnitude =
            narrowed.values().stream()
                .map(e -> componentGenerators().get(1).magnitude(e))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return BigDecimal.valueOf(narrowed.size())
            .multiply(keysMagnitude)
            .add(valuesMagnitude);
    }

    protected final T empty() {
        return instantiate(findConstructor(types().get(0)));
    }

    protected boolean okToAdd(Object key, Object value) {
        return true;
    }

    private boolean inSizeRange(T target) {
        return sizeRange == null
            || (target.size() >= sizeRange.min() && target.size() <= sizeRange.max());
    }

    private int size(SourceOfRandomness random, GenerationStatus status) {
        return sizeRange != null
            ? random.nextInt(sizeRange.min(), sizeRange.max())
            : status.size();
    }

    private List<T> removals(List<Entry<?, ?>> items) {
        return stream(halving(items.size()).spliterator(), false)
            .map(i -> removeFrom(items, i))
            .flatMap(Collection::stream)
            .map(this::convert)
            .filter(this::inSizeRange)
            .collect(toList());
    }

    @SuppressWarnings("unchecked")
    private T convert(List<?> entries) {
        T converted = empty();

        for (Object each : entries) {
            Entry<?, ?> entry = (Entry<?, ?>) each;
            converted.put(entry.getKey(), entry.getValue());
        }

        return converted;
    }

    private Shrink<Entry<?, ?>> entryShrinker(
        Shrink<Object> keyShrinker,
        Shrink<Object> valueShrinker) {

        return (r, e) -> {
            @SuppressWarnings("unchecked")
            Entry<Object, Object> entry = (Entry<Object, Object>) e;

            List<Object> keyShrinks = keyShrinker.shrink(r, entry.getKey());
            List<Object> valueShrinks = valueShrinker.shrink(r, entry.getValue());
            List<Entry<?, ?>> shrinks = new ArrayList<>();
            shrinks.addAll(
                keyShrinks.stream()
                    .map(k -> new SimpleEntry<>(k, entry.getValue()))
                    .collect(toList()));
            shrinks.addAll(
                valueShrinks.stream()
                    .map(v -> new SimpleEntry<>(entry.getKey(), v))
                    .collect(toList()));

            return shrinks;
        };
    }

    private static boolean isKeyDistinct(List<Entry<?, ?>> entries) {
        return Lists.isDistinct(
            entries.stream()
                .map(Entry::getKey)
                .collect(toList()));
    }
}
