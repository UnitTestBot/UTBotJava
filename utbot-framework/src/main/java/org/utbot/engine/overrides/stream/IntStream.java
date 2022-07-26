package org.utbot.engine.overrides.stream;

import org.utbot.api.annotation.UtClassMock;

import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.stream.BaseStream;

import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
@UtClassMock(target = java.util.stream.IntStream.class, internalUsage = true)
public interface IntStream extends BaseStream<Integer, java.util.stream.IntStream> {
    static java.util.stream.IntStream empty() {
        return new UtIntStream();
    }

    static java.util.stream.IntStream of(int t) {
        Integer[] data = new Integer[]{t};

        return new UtIntStream(data, 1);
    }

    static java.util.stream.IntStream of(int... values) {
        int size = values.length;
        Integer[] data = new Integer[size];
        for (int i = 0; i < size; i++) {
            data[i] = values[i];
        }

        return new UtIntStream(data, size);
    }

    @SuppressWarnings("unused")
    static java.util.stream.IntStream generate(IntSupplier s) {
        // as "generate" method produces an infinite stream, we cannot analyze it symbolically
        executeConcretely();
        return null;
    }

    static java.util.stream.IntStream range(int startInclusive, int endExclusive) {
        int size = endExclusive - startInclusive;
        Integer[] data = new Integer[size];
        for (int i = startInclusive; i < endExclusive; i++) {
            data[i - startInclusive] = i;
        }

        return new UtIntStream(data, size);
    }

    @SuppressWarnings("unused")
    static java.util.stream.IntStream rangeClosed(int startInclusive, int endInclusive) {
        return range(startInclusive, endInclusive + 1);
    }

    @SuppressWarnings("unused")
    static java.util.stream.IntStream iterate(final int seed, final IntUnaryOperator f) {
        // as "iterate" method produces an infinite stream, we cannot analyze it symbolically
        executeConcretely();
        return null;
    }

    @SuppressWarnings("unused")
    static java.util.stream.IntStream concat(java.util.stream.IntStream a, java.util.stream.IntStream b) {
        // as provided streams might be infinite, we cannot analyze this method symbolically
        executeConcretely();
        return null;
    }
}
