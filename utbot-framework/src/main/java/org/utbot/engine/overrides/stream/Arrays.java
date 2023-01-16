package org.utbot.engine.overrides.stream;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.api.mock.UtMock;
import org.utbot.engine.overrides.collections.UtArrayList;

import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@UtClassMock(target = java.util.Arrays.class, internalUsage = true)
public class Arrays {
    public static <T> Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
        int size = array.length;

        if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > size) {
            throw new ArrayIndexOutOfBoundsException();
        }

        return new UtStream<>(array, startInclusive, endExclusive);
    }

    // from docs - array is assumed to be unmodified during use
    public static IntStream stream(int[] array, int startInclusive, int endExclusive) {
        int size = array.length;

        if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > size) {
            throw new ArrayIndexOutOfBoundsException();
        }

        Integer[] data = new Integer[size];
        for (int i = 0; i < size; i++) {
            data[i] = array[i];
        }

        return new UtIntStream(data, startInclusive, endExclusive);
    }

    // from docs - array is assumed to be unmodified during use
    public static LongStream stream(long[] array, int startInclusive, int endExclusive) {
        int size = array.length;

        if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > size) {
            throw new ArrayIndexOutOfBoundsException();
        }

        Long[] data = new Long[size];
        for (int i = 0; i < size; i++) {
            data[i] = array[i];
        }

        return new UtLongStream(data, startInclusive, endExclusive);
    }

    // from docs - array is assumed to be unmodified during use
    public static DoubleStream stream(double[] array, int startInclusive, int endExclusive) {
        int size = array.length;

        if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > size) {
            throw new ArrayIndexOutOfBoundsException();
        }

        Double[] data = new Double[size];
        for (int i = 0; i < size; i++) {
            data[i] = array[i];
        }

        return new UtDoubleStream(data, startInclusive, endExclusive);
    }

    @SuppressWarnings({"unused", "varargs"})
    @SafeVarargs
    public static <T> List<T> asList(T... a) {
        // TODO immutable collection https://github.com/UnitTestBot/UTBotJava/issues/398
        return new UtArrayList<>(a);
    }
}
