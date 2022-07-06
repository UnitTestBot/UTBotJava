package org.utbot.engine.overrides.stream;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.engine.overrides.collections.UtArrayList;

import java.util.List;
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

    @SuppressWarnings({"unused", "varargs"})
    @SafeVarargs
    public static <T> List<T> asList(T... a) {
        // TODO immutable collection https://github.com/UnitTestBot/UTBotJava/issues/398
        return new UtArrayList<>(a);
    }

    // TODO primitive arrays https://github.com/UnitTestBot/UTBotJava/issues/146
}
