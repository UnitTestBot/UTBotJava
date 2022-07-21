package org.utbot.examples.stream;

import org.utbot.api.mock.UtMock;

import java.util.Arrays;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ToLongFunction;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

@SuppressWarnings("IfStatementWithIdenticalBranches")
public class LongStreamExample {
    LongStream returningStreamExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.isEmpty()) {
            return longs;
        } else {
            return longs;
        }
    }

    LongStream returningStreamAsParameterExample(LongStream s) {
        UtMock.assume(s != null);

        return s;
    }

    boolean filterExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int prevSize = list.size();

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        int newSize = list.stream().mapToLong(shortToLongFunction).filter(x -> x != 0).toArray().length;

        return prevSize != newSize;
    }

    long[] mapExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final LongUnaryOperator mapper = value -> value * 2;
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.contains(null)) {
            return longs.map(mapper).toArray();
        } else {
            return longs.map(mapper).toArray();
        }
    }

    Object[] mapToObjExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final LongFunction<long[]> mapper = value -> new long[]{value, value};
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.contains(null)) {
            return longs.mapToObj(mapper).toArray();
        } else {
            return longs.mapToObj(mapper).toArray();
        }
    }

    int[] mapToIntExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final LongToIntFunction mapper = value -> (int) value;
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.contains(null)) {
            return longs.mapToInt(mapper).toArray();
        } else {
            return longs.mapToInt(mapper).toArray();
        }
    }

    double[] mapToDoubleExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final LongToDoubleFunction mapper = value -> (double) value / 2;
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.contains(null)) {
            return longs.mapToDouble(mapper).toArray();
        } else {
            return longs.mapToDouble(mapper).toArray();
        }
    }

    long[] flatMapExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        return longs.flatMap(x -> Arrays.stream(new long[]{x, x})).toArray();
    }

    boolean distinctExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int prevSize = list.size();

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        int newSize = list.stream().mapToLong(shortToLongFunction).distinct().toArray().length;

        return prevSize != newSize;
    }

    long[] sortedExample(List<Short> list) {
        UtMock.assume(list != null && list.size() >= 2);

        Short first = list.get(0);

        int lastIndex = list.size() - 1;
        Short last = list.get(lastIndex);

        UtMock.assume(last < first);

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        return longs.sorted().toArray();
    }

    static int x = 0;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    int peekExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int beforeStaticValue = x;

        final LongConsumer action = value -> x += value;
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.contains(null)) {
            longs.peek(action);
        } else {
            longs.peek(action);
        }

        return beforeStaticValue;
    }

    long[] limitExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.size() <= 5) {
            return longs.limit(5).toArray();
        } else {
            return longs.limit(5).toArray();
        }
    }

    long[] skipExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.size() <= 5) {
            return longs.skip(5).toArray();
        } else {
            return longs.skip(5).toArray();
        }
    }

    int forEachExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int beforeStaticValue = x;

        final LongConsumer action = value -> x += value;
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.contains(null)) {
            longs.forEach(action);
        } else {
            longs.forEach(action);
        }

        return beforeStaticValue;
    }

    long[] toArrayExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (size <= 1) {
            return longs.toArray();
        } else {
            return longs.toArray();
        }
    }

    long reduceExample(List<Short> list) {
        UtMock.assume(list != null);

        final long identity = 42;
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.isEmpty()) {
            return longs.reduce(identity, Long::sum);
        } else {
            return longs.reduce(identity, Long::sum);
        }
    }

    OptionalLong optionalReduceExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (size == 0) {
            return longs.reduce(Long::sum);
        }

        return longs.reduce(Long::sum);
    }

    // TODO collect example

    long sumExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.isEmpty()) {
            return longs.sum();
        } else {
            return longs.sum();
        }
    }

    OptionalLong minExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (size == 0) {
            return longs.min();
        }

        return longs.min();
    }

    OptionalLong maxExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (size == 0) {
            return longs.max();
        }

        return longs.max();
    }

    long countExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.isEmpty()) {
            return longs.count();
        } else {
            return longs.count();
        }
    }

    OptionalDouble averageExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.isEmpty()) {
            return longs.average();
        } else {
            return longs.average();
        }
    }

    LongSummaryStatistics summaryStatisticsExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.isEmpty()) {
            return longs.summaryStatistics();
        } else {
            return longs.summaryStatistics();
        }
    }

    boolean anyMatchExample(List<Short> list) {
        UtMock.assume(list != null);

        final LongPredicate predicate = value -> value != 0;
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);
        if (list.isEmpty()) {
            return longs.anyMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Short first = list.get(0);
        Short second = list.get(1);

        if ((first == null || first == 0) && (second == null || second == 0)) {
            return longs.anyMatch(predicate);
        }

        if (first == null || first == 0) {
            return longs.anyMatch(predicate);
        }

        if (second == null || second == 0) {
            return longs.anyMatch(predicate);
        }

        return longs.anyMatch(predicate);
    }

    boolean allMatchExample(List<Short> list) {
        UtMock.assume(list != null);

        final LongPredicate predicate = value -> value != 0;
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);
        if (list.isEmpty()) {
            return longs.allMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Short first = list.get(0);
        Short second = list.get(1);

        if ((first == null || first == 0) && (second == null || second == 0)) {
            return longs.allMatch(predicate);
        }

        if (first == null || first == 0) {
            return longs.allMatch(predicate);
        }

        if (second == null || second == 0) {
            return longs.allMatch(predicate);
        }

        return longs.allMatch(predicate);
    }

    boolean noneMatchExample(List<Short> list) {
        UtMock.assume(list != null);

        final LongPredicate predicate = value -> value != 0;
        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);
        if (list.isEmpty()) {
            return longs.noneMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Short first = list.get(0);
        Short second = list.get(1);

        if ((first == null || first == 0) && (second == null || second == 0)) {
            return longs.noneMatch(predicate);
        }

        if (first == null || first == 0) {
            return longs.noneMatch(predicate);
        }

        if (second == null || second == 0) {
            return longs.noneMatch(predicate);
        }

        return longs.noneMatch(predicate);
    }

    OptionalLong findFirstExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToLongFunction<Short> shortToLongFunction = value -> value == null ? 0 : value.longValue();
        final LongStream longs = list.stream().mapToLong(shortToLongFunction);

        if (list.isEmpty()) {
            return longs.findFirst();
        }

        if (list.get(0) == null) {
            return longs.findFirst();
        } else {
            return longs.findFirst();
        }
    }

    DoubleStream asDoubleStreamExample(List<Short> list) {
        UtMock.assume(list != null);

        return list.stream().mapToLong(value -> value == null ? 0 : value.longValue()).asDoubleStream();
    }

    Object[] boxedExample(List<Short> list) {
        UtMock.assume(list != null);

        return list.stream().mapToLong(value -> value == null ? 0 : value.longValue()).boxed().toArray();
    }

    long iteratorSumExample(List<Short> list) {
        UtMock.assume(list != null);

        long sum = 0;
        PrimitiveIterator.OfLong streamIterator = list.stream().mapToLong(value -> value == null ? 0 : value.longValue()).iterator();

        if (list.isEmpty()) {
            while (streamIterator.hasNext()) {
                // unreachable
                Long value = streamIterator.next();
                sum += value;
            }
        } else {
            while (streamIterator.hasNext()) {
                Long value = streamIterator.next();
                sum += value;
            }
        }

        return sum;
    }

    LongStream streamOfExample(long[] values) {
        UtMock.assume(values != null);

        if (values.length == 0) {
            return LongStream.empty();
        } else {
            return LongStream.of(values);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    long closedStreamExample(List<Short> values) {
        UtMock.assume(values != null);

        LongStream intStream = values.stream().mapToLong(value -> value == null ? 0 : value.longValue());
        intStream.count();

        return intStream.count();
    }

    long[] generateExample() {
        return LongStream.generate(() -> 42).limit(10).toArray();
    }

    long[] iterateExample() {
        return LongStream.iterate(42, x -> x + 1).limit(10).toArray();
    }

    long[] concatExample() {
        final long identity = 42;
        LongStream first = LongStream.generate(() -> identity).limit(10);
        LongStream second = LongStream.iterate(identity, x -> x + 1).limit(10);

        return LongStream.concat(first, second).toArray();
    }

    long[] rangeExample() {
        return LongStream.range(0, 10).toArray();
    }

    long[] rangeClosedExample() {
        return LongStream.rangeClosed(0, 10).toArray();
    }
}
