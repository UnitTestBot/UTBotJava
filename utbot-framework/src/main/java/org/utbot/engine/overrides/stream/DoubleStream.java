package org.utbot.engine.overrides.stream;

import org.utbot.api.annotation.UtClassMock;

import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.BaseStream;

import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;

@UtClassMock(target = java.util.stream.DoubleStream.class, internalUsage = true)
public interface DoubleStream extends BaseStream<Double, java.util.stream.DoubleStream> {
    static java.util.stream.DoubleStream empty() {
        return new UtDoubleStream();
    }

    static java.util.stream.DoubleStream of(double t) {
        Double[] data = new Double[]{t};

        return new UtDoubleStream(data, 1);
    }

    static java.util.stream.DoubleStream of(double... values) {
        int size = values.length;
        Double[] data = new Double[size];
        for (int i = 0; i < size; i++) {
            data[i] = values[i];
        }

        return new UtDoubleStream(data, size);
    }

    @SuppressWarnings("unused")
    static java.util.stream.DoubleStream generate(DoubleSupplier s) {
        // as "generate" method produces an infinite stream, we cannot analyze it symbolically
        executeConcretely();
        return null;
    }

    @SuppressWarnings("unused")
    static java.util.stream.DoubleStream iterate(final double seed, final DoubleUnaryOperator f) {
        // as "iterate" method produces an infinite stream, we cannot analyze it symbolically
        executeConcretely();
        return null;
    }

    @SuppressWarnings("unused")
    static java.util.stream.DoubleStream concat(java.util.stream.DoubleStream a, java.util.stream.DoubleStream b) {
        // as provided streams might be infinite, we cannot analyze this method symbolically
        executeConcretely();
        return null;
    }
}
