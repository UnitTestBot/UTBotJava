package org.utbot.engine.overrides.stream;

import org.utbot.api.annotation.UtClassMock;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.BaseStream;

import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;

@SuppressWarnings("unused")
@UtClassMock(target = java.util.stream.Stream.class, internalUsage = true)
public interface Stream<E> extends BaseStream<E, Stream<E>> {
    @SuppressWarnings("unchecked")
    static <E> java.util.stream.Stream<E> of(E element) {
        Object[] data = new Object[1];
        data[0] = element;

        return new UtStream<>((E[]) data, 1);
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.stream.Stream<E> of(E... elements) {
        int size = elements.length;

        return new UtStream<>(elements, size);
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.stream.Stream<E> empty() {
        return new UtStream<>((E[]) new Object[]{}, 0);
    }

    static <E> java.util.stream.Stream<E> generate(Supplier<E> s) {
        // as "generate" method produces an infinite stream, we cannot analyze it symbolically
        executeConcretely();
        return null;
    }

    static <E> java.util.stream.Stream<E> iterate(final E seed, final UnaryOperator<E> f) {
        // as "iterate" method produces an infinite stream, we cannot analyze it symbolically
        executeConcretely();
        return null;
    }
    
     static <E> java.util.stream.Stream<E> concat(
            java.util.stream.Stream<? extends E> a, 
            java.util.stream.Stream<? extends E> b
    ) {
        // as provided streams might be infinite, we cannot analyze this method symbolically
        executeConcretely();
        return null;
    }
}
