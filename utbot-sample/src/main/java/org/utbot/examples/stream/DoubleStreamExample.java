package org.utbot.examples.stream;

import org.utbot.api.mock.UtMock;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;

@SuppressWarnings("IfStatementWithIdenticalBranches")
public class DoubleStreamExample {
    DoubleStream returningStreamExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.isEmpty()) {
            return doubles;
        } else {
            return doubles;
        }
    }

    DoubleStream returningStreamAsParameterExample(DoubleStream s) {
        UtMock.assume(s != null);

        return s;
    }

    boolean filterExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int prevSize = list.size();

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        int newSize = list.stream().mapToDouble(shortToDoubleFunction).filter(x -> x != 0).toArray().length;

        return prevSize != newSize;
    }

    double[] mapExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final DoubleUnaryOperator mapper = value -> value * 2;
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.contains(null)) {
            return doubles.map(mapper).toArray();
        } else {
            return doubles.map(mapper).toArray();
        }
    }

    Object[] mapToObjExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final DoubleFunction<double[]> mapper = value -> new double[]{value, value};
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.contains(null)) {
            return doubles.mapToObj(mapper).toArray();
        } else {
            return doubles.mapToObj(mapper).toArray();
        }
    }

    int[] mapToIntExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final DoubleToIntFunction mapper = value -> (int) value;
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.contains(null)) {
            return doubles.mapToInt(mapper).toArray();
        } else {
            return doubles.mapToInt(mapper).toArray();
        }
    }

    long[] mapToLongExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final DoubleToLongFunction mapper = value -> (long) value;
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.contains(null)) {
            return doubles.mapToLong(mapper).toArray();
        } else {
            return doubles.mapToLong(mapper).toArray();
        }
    }

    double[] flatMapExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        return doubles.flatMap(x -> Arrays.stream(new double[]{x, x})).toArray();
    }

    boolean distinctExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int prevSize = list.size();

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        int newSize = list.stream().mapToDouble(shortToDoubleFunction).distinct().toArray().length;

        return prevSize != newSize;
    }

    double[] sortedExample(List<Short> list) {
        UtMock.assume(list != null && list.size() >= 2);

        Short first = list.get(0);

        int lastIndex = list.size() - 1;
        Short last = list.get(lastIndex);

        UtMock.assume(last < first);

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        return doubles.sorted().toArray();
    }

    static int x = 0;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    int peekExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int beforeStaticValue = x;

        final DoubleConsumer action = value -> x += value;
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.contains(null)) {
            doubles.peek(action);
        } else {
            doubles.peek(action);
        }

        return beforeStaticValue;
    }

    double[] limitExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.size() <= 5) {
            return doubles.limit(5).toArray();
        } else {
            return doubles.limit(5).toArray();
        }
    }

    double[] skipExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.size() <= 5) {
            return doubles.skip(5).toArray();
        } else {
            return doubles.skip(5).toArray();
        }
    }

    int forEachExample(List<Short> list) {
        UtMock.assume(list != null && !list.isEmpty());

        int beforeStaticValue = x;

        final DoubleConsumer action = value -> x += value;
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.contains(null)) {
            doubles.forEach(action);
        } else {
            doubles.forEach(action);
        }

        return beforeStaticValue;
    }

    double[] toArrayExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (size <= 1) {
            return doubles.toArray();
        } else {
            return doubles.toArray();
        }
    }

    double reduceExample(List<Short> list) {
        UtMock.assume(list != null);

        final double identity = 42;
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.isEmpty()) {
            return doubles.reduce(identity, Double::sum);
        } else {
            return doubles.reduce(identity, Double::sum);
        }
    }

    OptionalDouble optionalReduceExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (size == 0) {
            return doubles.reduce(Double::sum);
        }

        return doubles.reduce(Double::sum);
    }

    // TODO collect example

    double sumExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.isEmpty()) {
            return doubles.sum();
        } else {
            return doubles.sum();
        }
    }

    OptionalDouble minExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (size == 0) {
            return doubles.min();
        }

        return doubles.min();
    }

    OptionalDouble maxExample(List<Short> list) {
        UtMock.assume(list != null);

        int size = list.size();

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (size == 0) {
            return doubles.max();
        }

        return doubles.max();
    }

    long countExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.isEmpty()) {
            return doubles.count();
        } else {
            return doubles.count();
        }
    }

    OptionalDouble averageExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.isEmpty()) {
            return doubles.average();
        } else {
            return doubles.average();
        }
    }

    DoubleSummaryStatistics summaryStatisticsExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.isEmpty()) {
            return doubles.summaryStatistics();
        } else {
            return doubles.summaryStatistics();
        }
    }

    boolean anyMatchExample(List<Short> list) {
        UtMock.assume(list != null);

        final DoublePredicate predicate = value -> value != 0;
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);
        if (list.isEmpty()) {
            return doubles.anyMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Short first = list.get(0);
        Short second = list.get(1);

        if ((first == null || first == 0) && (second == null || second == 0)) {
            return doubles.anyMatch(predicate);
        }

        if (first == null || first == 0) {
            return doubles.anyMatch(predicate);
        }

        if (second == null || second == 0) {
            return doubles.anyMatch(predicate);
        }

        return doubles.anyMatch(predicate);
    }

    boolean allMatchExample(List<Short> list) {
        UtMock.assume(list != null);

        final DoublePredicate predicate = value -> value != 0;
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);
        if (list.isEmpty()) {
            return doubles.allMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Short first = list.get(0);
        Short second = list.get(1);

        if ((first == null || first == 0) && (second == null || second == 0)) {
            return doubles.allMatch(predicate);
        }

        if (first == null || first == 0) {
            return doubles.allMatch(predicate);
        }

        if (second == null || second == 0) {
            return doubles.allMatch(predicate);
        }

        return doubles.allMatch(predicate);
    }

    boolean noneMatchExample(List<Short> list) {
        UtMock.assume(list != null);

        final DoublePredicate predicate = value -> value != 0;
        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);
        if (list.isEmpty()) {
            return doubles.noneMatch(predicate);
        }

        UtMock.assume(list.size() == 2);

        Short first = list.get(0);
        Short second = list.get(1);

        if ((first == null || first == 0) && (second == null || second == 0)) {
            return doubles.noneMatch(predicate);
        }

        if (first == null || first == 0) {
            return doubles.noneMatch(predicate);
        }

        if (second == null || second == 0) {
            return doubles.noneMatch(predicate);
        }

        return doubles.noneMatch(predicate);
    }

    OptionalDouble findFirstExample(List<Short> list) {
        UtMock.assume(list != null);

        final ToDoubleFunction<Short> shortToDoubleFunction = value -> value == null ? 0 : value.doubleValue();
        final DoubleStream doubles = list.stream().mapToDouble(shortToDoubleFunction);

        if (list.isEmpty()) {
            return doubles.findFirst();
        }

        if (list.get(0) == null) {
            return doubles.findFirst();
        } else {
            return doubles.findFirst();
        }
    }

    Object[] boxedExample(List<Short> list) {
        UtMock.assume(list != null);

        return list.stream().mapToDouble(value -> value == null ? 0 : value.doubleValue()).boxed().toArray();
    }

    double iteratorSumExample(List<Short> list) {
        UtMock.assume(list != null);

        double sum = 0;
        PrimitiveIterator.OfDouble streamIterator = list.stream().mapToDouble(value -> value == null ? 0 : value.doubleValue()).iterator();

        if (list.isEmpty()) {
            while (streamIterator.hasNext()) {
                // unreachable
                Double value = streamIterator.next();
                sum += value;
            }
        } else {
            while (streamIterator.hasNext()) {
                Double value = streamIterator.next();
                sum += value;
            }
        }

        return sum;
    }

    DoubleStream streamOfExample(double[] values) {
        UtMock.assume(values != null);

        if (values.length == 0) {
            return DoubleStream.empty();
        } else {
            return DoubleStream.of(values);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    long closedStreamExample(List<Short> values) {
        UtMock.assume(values != null);

        DoubleStream doubleStream = values.stream().mapToDouble(value -> value == null ? 0 : value.doubleValue());
        doubleStream.count();

        return doubleStream.count();
    }

    double[] generateExample() {
        return DoubleStream.generate(() -> 42).limit(10).toArray();
    }

    double[] iterateExample() {
        return DoubleStream.iterate(42, x -> x + 1).limit(10).toArray();
    }

    double[] concatExample() {
        final double identity = 42;
        DoubleStream first = DoubleStream.generate(() -> identity).limit(10);
        DoubleStream second = DoubleStream.iterate(identity, x -> x + 1).limit(10);

        return DoubleStream.concat(first, second).toArray();
    }
}
