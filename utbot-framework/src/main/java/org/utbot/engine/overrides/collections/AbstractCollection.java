package org.utbot.engine.overrides.collections;

import java.util.function.IntFunction;
import org.utbot.api.annotation.UtClassMock;

import static org.utbot.api.mock.UtMock.makeSymbolic;

@UtClassMock(target = java.util.AbstractCollection.class, internalUsage = true)
public abstract class AbstractCollection<E> implements java.util.Collection<E> {
    @Override
    public String toString() {
        return makeSymbolic();
    }

    public <T> T[] toArray(IntFunction<T[]> generator) {
        T[] data = generator.apply(size());
        return toArray(data);
    }
}
