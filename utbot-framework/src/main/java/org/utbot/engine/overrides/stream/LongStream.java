package org.utbot.engine.overrides.stream;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.api.mock.UtMock;

import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.stream.BaseStream;

import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;

@UtClassMock(target = java.util.stream.LongStream.class, internalUsage = true)
public interface LongStream extends BaseStream<Long, java.util.stream.LongStream> {
    static java.util.stream.LongStream empty() {
        return new UtLongStream();
    }

    static java.util.stream.LongStream of(long t) {
        Long[] data = new Long[]{t};

        return new UtLongStream(data, 1);
    }

    static java.util.stream.LongStream of(long... values) {
        int size = values.length;
        Long[] data = new Long[size];
        for (int i = 0; i < size; i++) {
            data[i] = values[i];
        }

        return new UtLongStream(data, size);
    }

    @SuppressWarnings("unused")
    static java.util.stream.LongStream generate(LongSupplier s) {
        // as "generate" method produces an infinite stream, we cannot analyze it symbolically
        executeConcretely();
        return null;
    }

    static java.util.stream.LongStream range(long startInclusive, long endExclusive) {
        int start = (int) startInclusive;
        int end = (int) endExclusive;

        // check that borders fit in int range
        UtMock.assumeOrExecuteConcretely(start == startInclusive);
        UtMock.assumeOrExecuteConcretely(end == endExclusive);

        int size = end - start;

        Long[] data = new Long[size];
        for (int i = start; i < end; i++) {
            data[i - start] = (long) i;
        }

        return new UtLongStream(data, size);
    }

    @SuppressWarnings("unused")
    static java.util.stream.LongStream rangeClosed(long startInclusive, long endInclusive) {
        return range(startInclusive, endInclusive + 1);
    }

    @SuppressWarnings("unused")
    static java.util.stream.LongStream iterate(final long seed, final LongUnaryOperator f) {
        // as "iterate" method produces an infinite stream, we cannot analyze it symbolically
        executeConcretely();
        return null;
    }

    @SuppressWarnings("unused")
    static java.util.stream.LongStream concat(java.util.stream.LongStream a, java.util.stream.LongStream b) {
        // as provided streams might be infinite, we cannot analyze this method symbolically
        executeConcretely();
        return null;
    }
}
