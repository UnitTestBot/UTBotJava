package org.utbot.examples.stream;

import org.utbot.api.mock.UtMock;

import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@SuppressWarnings("IfStatementWithIdenticalBranches")
public class IntStreamExample {
    IntStream returningStreamExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.isEmpty()) {
            return ints;
        } else {
            return ints;
        }
    }

    IntStream returningStreamAsParameterExample(IntStream s) {
        UtMock.assume(s != null);

        return s;
    }

    boolean filterExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int prevSize = list.size();

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        int newSize = list.stream().mapToInt(shortToIntFunction).filter(x -> x != 0).toArray().length;

        return prevSize != newSize;
    }

    int[] mapExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final IntUnaryOperator mapper = value -> value * 2;
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.contains(null)) {
            return ints.map(mapper).toArray();
        } else {
            return ints.map(mapper).toArray();
        }
    }

    Object[] mapToObjExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final IntFunction<int[]> mapper = value -> new int[]{value, value};
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.contains(null)) {
            return ints.mapToObj(mapper).toArray();
        } else {
            return ints.mapToObj(mapper).toArray();
        }
    }

    long[] mapToLongExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final IntToLongFunction mapper = value -> value * 2L;
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.contains(null)) {
            return ints.mapToLong(mapper).toArray();
        } else {
            return ints.mapToLong(mapper).toArray();
        }
    }

    double[] mapToDoubleExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final IntToDoubleFunction mapper = value -> (double) value / 2;
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.contains(null)) {
            return ints.mapToDouble(mapper).toArray();
        } else {
            return ints.mapToDouble(mapper).toArray();
        }
    }

    int[] flatMapExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        return ints.flatMap(x -> Arrays.stream(new int[]{x, x})).toArray();
    }

    boolean distinctExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int prevSize = list.size();

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        int newSize = list.stream().mapToInt(shortToIntFunction).distinct().toArray().length;

        return prevSize != newSize;
    }

    int[] sortedExample(List<Short> list) {
        UtMock.assume(list != null && list.size() >= 2);

        Short first = list.get(0);

        int lastIndex = list.size() - 1;
        Short last = list.get(lastIndex);

        UtMock.assume(last < first);

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        return ints.sorted().toArray();
    }

    static int x = 0;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    int peekExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int beforeStaticValue = x;

        final IntConsumer action = value -> x += value;
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.contains(null)) {
            ints.peek(action);
        } else {
            ints.peek(action);
        }

        return beforeStaticValue;
    }

    int[] limitExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.size() <= 5) {
            return ints.limit(5).toArray();
        } else {
            return ints.limit(5).toArray();
        }
    }

    int[] skipExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.size() <= 5) {
            return ints.skip(5).toArray();
        } else {
            return ints.skip(5).toArray();
        }
    }

    int forEachExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int beforeStaticValue = x;

        final IntConsumer action = value -> x += value;
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.contains(null)) {
            ints.forEach(action);
        } else {
            ints.forEach(action);
        }

        return beforeStaticValue;
    }

    int[] toArrayExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (size <= 1) {
            return ints.toArray();
        } else {
            return ints.toArray();
        }
    }

    int reduceExample(List<Short> list) {
        UtMock.assume(list != null);

        final int identity = 42;
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.isEmpty()) {
            return ints.reduce(identity, Integer::sum);
        } else {
            return ints.reduce(identity, Integer::sum);
        }
    }

    OptionalInt optionalReduceExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (size == 0) {
            return ints.reduce(Integer::sum);
        }

        return ints.reduce(Integer::sum);
    }

    // TODO collect example

    int sumExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.isEmpty()) {
            return ints.sum();
        } else {
            return ints.sum();
        }
    }

    OptionalInt minExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (size == 0) {
            return ints.min();
        }

        return ints.min();
    }

    OptionalInt maxExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (size == 0) {
            return ints.max();
        }

        return ints.max();
    }

    long countExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.isEmpty()) {
            return ints.count();
        } else {
            return ints.count();
        }
    }

    OptionalDouble averageExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.isEmpty()) {
            return ints.average();
        } else {
            return ints.average();
        }
    }

    IntSummaryStatistics summaryStatisticsExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.isEmpty()) {
            return ints.summaryStatistics();
        } else {
            return ints.summaryStatistics();
        }
    }

    boolean anyMatchExample(List<Short> list) {
        UtMock.assume(list != null);

        final IntPredicate predicate = value -> value != 0;
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);
        if (list.isEmpty()) {
            return ints.anyMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Short first = list.get(0);
        Short second = list.get(1);

        if (first == 0 && second == 0) {
            return ints.anyMatch(predicate);
        }

        if (first == 0) {
            return ints.anyMatch(predicate);
        }

        if (second == 0) {
            return ints.anyMatch(predicate);
        }

        return ints.anyMatch(predicate);
    }

    boolean allMatchExample(List<Short> list) {
        UtMock.assume(list != null);

        final IntPredicate predicate = value -> value != 0;
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);
        if (list.isEmpty()) {
            return ints.allMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Short first = list.get(0);
        Short second = list.get(1);

        if (first == 0 && second == 0) {
            return ints.allMatch(predicate);
        }

        if (first == 0) {
            return ints.allMatch(predicate);
        }

        if (second == 0) {
            return ints.allMatch(predicate);
        }

        return ints.allMatch(predicate);
    }

    boolean noneMatchExample(List<Short> list) {
        UtMock.assume(list != null);

        final IntPredicate predicate = value -> value != 0;
        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);
        if (list.isEmpty()) {
            return ints.noneMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Short first = list.get(0);
        Short second = list.get(1);

        if (first == 0 && second == 0) {
            return ints.noneMatch(predicate);
        }

        if (first == 0) {
            return ints.noneMatch(predicate);
        }

        if (second == 0) {
            return ints.noneMatch(predicate);
        }

        return ints.noneMatch(predicate);
    }

    OptionalInt findFirstExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToIntFunction<Short> shortToIntFunction = value -> value == null ? 0 : value.intValue();
        final IntStream ints = list.stream().mapToInt(shortToIntFunction);

        if (list.isEmpty()) {
            return ints.findFirst();
        }

        if (list.get(0) == null) {
            return ints.findFirst();
        } else {
            return ints.findFirst();
        }
    }

    LongStream asLongStreamExample(List<Short> list) {
        UtMock.assume(list != null);

        return list.stream().mapToInt(value -> value == null ? 0 : value.intValue()).asLongStream();
    }

    DoubleStream asDoubleStreamExample(List<Short> list) {
        UtMock.assume(list != null);

        return list.stream().mapToInt(value -> value == null ? 0 : value.intValue()).asDoubleStream();
    }

    Object[] boxedExample(List<Short> list) {
        UtMock.assume(list != null);

        return list.stream().mapToInt(value -> value == null ? 0 : value.intValue()).boxed().toArray();
    }

    @SuppressWarnings("DuplicatedCode")
    int iteratorSumExample(List<Short> list) {
        UtMock.assume(list != null);

        int sum = 0;
        PrimitiveIterator.OfInt streamIterator = list.stream().mapToInt(value -> value == null ? 0 : value.intValue()).iterator();

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

    IntStream streamOfExample(int[] values) {
        UtMock.assume(values != null);

        if (values.length == 0) {
            return IntStream.empty();
        } else {
            return IntStream.of(values);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    long closedStreamExample(List<Short> values) {
        UtMock.assume(values != null);

        IntStream intStream = values.stream().mapToInt(value -> value == null ? 0 : value.intValue());
        intStream.count();

        return intStream.count();
    }

    int[] generateExample() {
        return IntStream.generate(() -> 42).limit(10).toArray();
    }

    int[] iterateExample() {
        return IntStream.iterate(42, x -> x + 1).limit(10).toArray();
    }

    int[] concatExample() {
        final int identity = 42;
        IntStream first = IntStream.generate(() -> identity).limit(10);
        IntStream second = IntStream.iterate(identity, x -> x + 1).limit(10);

        return IntStream.concat(first, second).toArray();
    }

    int[] rangeExample() {
        return IntStream.range(0, 10).toArray();
    }

    int[] rangeClosedExample() {
        return IntStream.rangeClosed(0, 10).toArray();
    }
}
