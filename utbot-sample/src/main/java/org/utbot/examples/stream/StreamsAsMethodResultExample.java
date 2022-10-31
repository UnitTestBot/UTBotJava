package org.utbot.examples.stream;

import org.utbot.api.mock.*;

import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@SuppressWarnings({"IfStatementWithIdenticalBranches", "RedundantOperationOnEmptyContainer"})
public class StreamsAsMethodResultExample {
    Stream<Integer> returningStreamExample(List<Integer> list) {
        UtMock.assume(list != null);

        if (list.isEmpty()) {
            return list.stream();
        }

        return list.stream();
    }

    IntStream returningIntStreamExample(List<Integer> list) {
        UtMock.assume(list != null);

        final int size = list.size();

        if (size == 0) {
            return list.stream().mapToInt(value -> value);
        }

        UtMock.assume(size == 1);

        final Integer integer = list.get(0);

        if (integer == null) {
            return list.stream().mapToInt(value -> value);
        }

        return list.stream().mapToInt(value -> value);
    }

    LongStream returningLongStreamExample(List<Integer> list) {
        UtMock.assume(list != null);

        final int size = list.size();

        if (size == 0) {
            return list.stream().mapToLong(value -> value);
        }

        UtMock.assume(size == 1);

        final Integer integer = list.get(0);

        if (integer == null) {
            return list.stream().mapToLong(value -> value);
        }

        return list.stream().mapToLong(value -> value);
    }

    DoubleStream returningDoubleStreamExample(List<Integer> list) {
        UtMock.assume(list != null);

        final int size = list.size();

        if (size == 0) {
            return list.stream().mapToDouble(value -> value);
        }

        UtMock.assume(size == 1);

        final Integer integer = list.get(0);

        if (integer == null) {
            return list.stream().mapToDouble(value -> value);
        }

        return list.stream().mapToDouble(value -> value);
    }
}

