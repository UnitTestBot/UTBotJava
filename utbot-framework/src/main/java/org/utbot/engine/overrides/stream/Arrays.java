package org.utbot.engine.overrides.stream;

import org.utbot.api.annotation.UtClassMock;

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

    // TODO primitive arrays https://github.com/UnitTestBot/UTBotJava/issues/146
}
