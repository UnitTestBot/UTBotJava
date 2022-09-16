package org.utbot.engine.overrides.collections;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.api.mock.UtMock;
import org.utbot.engine.overrides.stream.UtStream;

import java.util.stream.Stream;

@UtClassMock(target = java.util.Collection.class, internalUsage = true)
public interface Collection<E> extends java.util.Collection<E> {
    @SuppressWarnings("unchecked")
    @Override
    default Stream<E> stream() {
        Object[] data = toArray();
        UtMock.disableClassCastExceptionCheck(data);

        int size = data.length;

        return new UtStream<>((E[]) data, size);
    }

    @Override
    default Stream<E> parallelStream() {
        return stream();
    }
}
