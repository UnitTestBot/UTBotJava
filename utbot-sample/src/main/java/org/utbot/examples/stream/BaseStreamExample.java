package org.utbot.examples.stream;

import org.jetbrains.annotations.NotNull;
import org.utbot.api.mock.UtMock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"IfStatementWithIdenticalBranches", "RedundantOperationOnEmptyContainer"})
public class BaseStreamExample {
    Stream<Integer> returningStreamExample(List<Integer> list) {
        UtMock.assume(list != null);

        if (list.isEmpty()) {
            return list.stream();
        } else {
            return list.stream();
        }
    }

    Stream<Integer> returningStreamAsParameterExample(Stream<Integer> s) {
        UtMock.assume(s != null);
        return s;
    }

    @SuppressWarnings("Convert2MethodRef")
    boolean filterExample(List<Integer> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int prevSize = list.size();

        int newSize = list.stream().filter(value -> value != null).toArray().length;

        return prevSize != newSize;
    }

    Integer[] mapExample(List<Integer> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final Function<Integer, Integer> mapper = value -> value * 2;
        if (list.contains(null)) {
            return list.stream().map(mapper).toArray(Integer[]::new);
        } else {
            return list.stream().map(mapper).toArray(Integer[]::new);
        }
    }

    int[] mapToIntExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        if (list.contains(null)) {
            return list.stream().mapToInt(Short::intValue).toArray();
        } else {
            return list.stream().mapToInt(Short::intValue).toArray();
        }
    }

    long[] mapToLongExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        if (list.contains(null)) {
            return list.stream().mapToLong(Short::longValue).toArray();
        } else {
            return list.stream().mapToLong(Short::longValue).toArray();
        }
    }

    double[] mapToDoubleExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        if (list.contains(null)) {
            return list.stream().mapToDouble(Short::doubleValue).toArray();
        } else {
            return list.stream().mapToDouble(Short::doubleValue).toArray();
        }
    }

    Object[] flatMapExample(List<Integer> list) {
        UtMock.assume(list != null && !list.isEmpty());

        return list.stream().flatMap(value -> Arrays.stream(new Object[]{value, value})).toArray(Object[]::new);
    }

    int[] flatMapToIntExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();

        return list
                .stream()
                .flatMapToInt(value ->
                        Arrays.stream(new int[]{
                                shortToIntFunction.applyAsInt(value),
                                shortToIntFunction.applyAsInt(value)}
                        )
                )
                .toArray();
    }

    long[] flatMapToLongExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();

        return list
                .stream()
                .flatMapToLong(value ->
                        Arrays.stream(new long[]{
                                shortToLongFunction.applyAsLong(value),
                                shortToLongFunction.applyAsLong(value)}
                        )
                )
                .toArray();
    }

    double[] flatMapToDoubleExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();

        return list
                .stream()
                .flatMapToDouble(value ->
                        Arrays.stream(new double[]{
                                shortToDoubleFunction.applyAsDouble(value),
                                shortToDoubleFunction.applyAsDouble(value)}
                        )
                )
                .toArray();
    }

    boolean distinctExample(List<Integer> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int prevSize = list.size();

        int newSize = list.stream().distinct().toArray().length;

        return prevSize != newSize;
    }

    Integer[] sortedExample(List<Integer> list) {
        UtMock.assume(list != null && list.size() >= 2);

        Integer first = list.get(0);

        int lastIndex = list.size() - 1;
        Integer last = list.get(lastIndex);

        UtMock.assume(last < first);

        return list.stream().sorted().toArray(Integer[]::new);
    }

    // TODO sorted with custom Comparator

    static int x = 0;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    int peekExample(List<Integer> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int beforeStaticValue = x;

        final Consumer<Integer> action = value -> x += value;
        if (list.contains(null)) {
            list.stream().peek(action);
        } else {
            list.stream().peek(action);
        }

        return beforeStaticValue;
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    Integer[] limitExample(List<Integer> list) {
        UtMock.assume(list != null && !list.isEmpty());

        if (list.size() <= 5) {
            return list.stream().limit(5).toArray(Integer[]::new);
        } else {
            return list.stream().limit(5).toArray(Integer[]::new);
        }
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    Integer[] skipExample(List<Integer> list) {
        UtMock.assume(list != null && !list.isEmpty());

        if (list.size() > 5) {
            return list.stream().skip(5).toArray(Integer[]::new);
        } else {
            return list.stream().skip(5).toArray(Integer[]::new);
        }
    }

    @SuppressWarnings("SimplifyStreamApiCallChains")
    int forEachExample(List<Integer> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int beforeStaticValue = x;

        final Consumer<Integer> action = value -> x += value;
        if (list.contains(null)) {
            list.stream().forEach(action);
        } else {
            list.stream().forEach(action);
        }

        return beforeStaticValue;
    }

    @SuppressWarnings("SimplifyStreamApiCallChains")
    Object[] toArrayExample(List<Integer> list) {
        UtMock.assume(list != null);

        int size = list.size();
        if (size <= 1) {
            return list.stream().toArray();
        } else {
            return list.stream().toArray(Integer[]::new);
        }
    }

    Integer reduceExample(List<Integer> list) {
        UtMock.assume(list != null);

        final int identity = 42;
        if (list.isEmpty()) {
            return list.stream().reduce(identity, this::nullableSum);
        } else {
            return list.stream().reduce(identity, this::nullableSum);
        }
    }

    Optional<Integer> optionalReduceExample(List<Integer> list) {
        UtMock.assume(list != null);

        int size = list.size();

        if (size == 0) {
            return list.stream().reduce(this::nullableSum);
        }

        if (size == 1 && list.get(0) == null) {
            return list.stream().reduce(this::nullableSum);
        }

        return list.stream().reduce(this::nullableSum);
    }

    Double complexReduceExample(List<Integer> list) {
        UtMock.assume(list != null);

        final double identity = 42.0;
        final BiFunction<Double, Integer, Double> accumulator = (Double a, Integer b) -> a + (b != null ? b.doubleValue() : 0.0);
        if (list.isEmpty()) {
            return list.stream().reduce(identity, accumulator, Double::sum);
        }

        // TODO this branch leads to almost infinite analysis
//        if (list.contains(null)) {
//            return list.stream().reduce(42.0, (Double a, Integer b) -> a + b.doubleValue(), Double::sum);
//        }

        return list.stream().reduce(
                identity,
                accumulator,
                Double::sum
        );
    }

    Integer collectExample(List<Integer> list) {
        UtMock.assume(list != null);

        if (list.contains(null)) {
            return list.stream().collect(IntWrapper::new, IntWrapper::plus, IntWrapper::plus).value;
        } else {
            return list.stream().collect(IntWrapper::new, IntWrapper::plus, IntWrapper::plus).value;
        }
    }

    @SuppressWarnings("SimplifyStreamApiCallChains")
    Set<Integer> collectorExample(List<Integer> list) {
        UtMock.assume(list != null);

        return list.stream().collect(Collectors.toSet());
    }

    @SuppressWarnings("RedundantOperationOnEmptyContainer")
    Optional<Integer> minExample(List<Integer> list) {
        UtMock.assume(list != null);

        int size = list.size();

        if (size == 0) {
            return list.stream().min(this::nullableCompareTo);
        }

        if (size == 1 && list.get(0) == null) {
            return list.stream().min(this::nullableCompareTo);
        }

        return list.stream().min(this::nullableCompareTo);
    }

    @SuppressWarnings("RedundantOperationOnEmptyContainer")
    Optional<Integer> maxExample(List<Integer> list) {
        UtMock.assume(list != null);

        int size = list.size();

        if (size == 0) {
            return list.stream().max(this::nullableCompareTo);
        }

        if (size == 1 && list.get(0) == null) {
            return list.stream().max(this::nullableCompareTo);
        }

        return list.stream().max(this::nullableCompareTo);
    }

    @SuppressWarnings({"ReplaceInefficientStreamCount", "ConstantConditions"})
    long countExample(List<Integer> list) {
        UtMock.assume(list != null);

        if (list.isEmpty()) {
            return list.stream().count();
        } else {
            return list.stream().count();
        }
    }

    @SuppressWarnings({"Convert2MethodRef", "ConstantConditions", "RedundantOperationOnEmptyContainer"})
    boolean anyMatchExample(List<Integer> list) {
        UtMock.assume(list != null);

        final Predicate<Integer> predicate = value -> value == null;
        if (list.isEmpty()) {
            return list.stream().anyMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Integer first = list.get(0);
        Integer second = list.get(1);

        if (first == null && second == null) {
            return list.stream().anyMatch(predicate);
        }

        if (first == null) {
            return list.stream().anyMatch(predicate);
        }

        if (second == null) {
            return list.stream().anyMatch(predicate);
        }

        return list.stream().anyMatch(predicate);
    }

    @SuppressWarnings({"Convert2MethodRef", "ConstantConditions", "RedundantOperationOnEmptyContainer"})
    boolean allMatchExample(List<Integer> list) {
        UtMock.assume(list != null);

        final Predicate<Integer> predicate = value -> value == null;
        if (list.isEmpty()) {
            return list.stream().allMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Integer first = list.get(0);
        Integer second = list.get(1);

        if (first == null && second == null) {
            return list.stream().allMatch(predicate);
        }

        if (first == null) {
            return list.stream().allMatch(predicate);
        }

        if (second == null) {
            return list.stream().allMatch(predicate);
        }

        return list.stream().allMatch(predicate);
    }

    @SuppressWarnings({"Convert2MethodRef", "ConstantConditions", "RedundantOperationOnEmptyContainer"})
    boolean noneMatchExample(List<Integer> list) {
        UtMock.assume(list != null);

        final Predicate<Integer> predicate = value -> value == null;
        if (list.isEmpty()) {
            return list.stream().noneMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Integer first = list.get(0);
        Integer second = list.get(1);

        if (first == null && second == null) {
            return list.stream().noneMatch(predicate);
        }

        if (first == null) {
            return list.stream().noneMatch(predicate);
        }

        if (second == null) {
            return list.stream().noneMatch(predicate);
        }

        return list.stream().noneMatch(predicate);
    }

    @SuppressWarnings("RedundantOperationOnEmptyContainer")
    Optional<Integer> findFirstExample(List<Integer> list) {
        UtMock.assume(list != null);

        if (list.isEmpty()) {
            return list.stream().findFirst();
        }

        if (list.get(0) == null) {
            return list.stream().findFirst();
        } else {
            return list.stream().findFirst();
        }
    }

    @SuppressWarnings("DuplicatedCode")
    Integer iteratorSumExample(List<Integer> list) {
        UtMock.assume(list != null);

        int sum = 0;
        Iterator<Integer> streamIterator = list.stream().iterator();

        if (list.isEmpty()) {
            while (streamIterator.hasNext()) {
                // unreachable
                Integer value = streamIterator.next();
                sum += value;
            }
        } else {
            while (streamIterator.hasNext()) {
                Integer value = streamIterator.next();
                sum += value;
            }
        }

        return sum;
    }

    Stream<Integer> streamOfExample(Integer[] values) {
        UtMock.assume(values != null);

        if (values.length == 0) {
            return Stream.empty();
        } else {
            return Stream.of(values);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    long closedStreamExample(List<Integer> values) {
        UtMock.assume(values != null);

        Stream<Integer> stream = values.stream();
        stream.count();

        return stream.count();
    }

    @SuppressWarnings({"ReplaceInefficientStreamCount", "ConstantConditions"})
    long customCollectionStreamExample(CustomCollection<Integer> customCollection) {
        UtMock.assume(customCollection != null && customCollection.data != null);

        if (customCollection.isEmpty()) {
            return customCollection.stream().count();

            // simplified example, does not generate branch too
            /*customCollection.removeIf(Objects::isNull);
            return customCollection.toArray().length;*/
        } else {
            return customCollection.stream().count();

            // simplified example, does not generate branch too
            /*customCollection.removeIf(Objects::isNull);
            return customCollection.toArray().length;*/
        }
    }

    @SuppressWarnings({"ConstantConditions", "ReplaceInefficientStreamCount"})
    long anyCollectionStreamExample(Collection<Integer> c) {
        UtMock.assume(c != null);

        if (c.isEmpty()) {
            return c.stream().count();
        } else {
            return c.stream().count();
        }
    }

    Integer[] generateExample() {
        return Stream.generate(() -> 42).limit(10).toArray(Integer[]::new);
    }

    Integer[] iterateExample() {
        return Stream.iterate(42, x -> x + 1).limit(10).toArray(Integer[]::new);
    }

    Integer[] concatExample() {
        final int identity = 42;
        Stream<Integer> first = Stream.generate(() -> identity).limit(10);
        Stream<Integer> second = Stream.iterate(identity, x -> x + 1).limit(10);

        return Stream.concat(first, second).toArray(Integer[]::new);
    }

    // avoid NPE
    private int nullableSum(Integer a, Integer b) {
        if (b == null) {
            return a;
        }

        return a + b;
    }

    // avoid NPE
    private int nullableCompareTo(Integer a, Integer b) {
        if (a == null && b == null) {
            return 0;
        }

        if (a == null) {
            return -1;
        }

        if (b == null) {
            return 1;
        }

        return a.compareTo(b);
    }

    private static class IntWrapper {
        int value = 0;

        void plus(int other) {
            value += other;
        }

        void plus(IntWrapper other) {
            value += other.value;
        }
    }

    public static class CustomCollection<E> implements Collection<E> {
        private E[] data;

        public CustomCollection(@NotNull E[] data) {
            this.data = data;
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean contains(Object o) {
            return Arrays.asList(data).contains(o);
        }

        @NotNull
        @Override
        public Iterator<E> iterator() {
            return Arrays.asList(data).iterator();
        }

        @SuppressWarnings({"ManualArrayCopy", "unchecked"})
        @NotNull
        @Override
        public Object @NotNull [] toArray() {
            final int size = size();
            E[] arr = (E[]) new Object[size];
            for (int i = 0; i < size; i++) {
                arr[i] = data[i];
            }

            return arr;
        }

        @SuppressWarnings({"SuspiciousToArrayCall"})
        @Override
        public <T> T[] toArray(T @NotNull [] a) {
            return Arrays.asList(data).toArray(a);
        }

        @Override
        public boolean add(E e) {
            final int size = size();
            E[] newData = Arrays.copyOf(data, size + 1);
            newData[size] = e;
            data = newData;

            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o) {
            final List<E> es = Arrays.asList(data);
            final boolean removed = es.remove(o);
            data = (E[]) es.toArray();

            return removed;
        }

        @SuppressWarnings("SlowListContainsAll")
        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return Arrays.asList(data).containsAll(c);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean addAll(@NotNull Collection<? extends E> c) {
            final List<E> es = Arrays.asList(data);
            final boolean added = es.addAll(c);
            data = (E[]) es.toArray();

            return added;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            final List<E> es = Arrays.asList(data);
            final boolean removed = es.removeAll(c);
            data = (E[]) es.toArray();

            return removed;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            final List<E> es = Arrays.asList(data);
            final boolean retained = es.retainAll(c);
            data = (E[]) es.toArray();

            return retained;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void clear() {
            data = (E[]) new Object[0];
        }
    }
}
