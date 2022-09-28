package org.utbot.engine.overrides.collections;

import java.util.function.IntFunction;
import org.utbot.api.annotation.UtClassMock;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.api.mock.UtMock.makeSymbolic;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;

@UtClassMock(target = java.util.AbstractCollection.class, internalUsage = true)
public abstract class AbstractCollection<E> implements java.util.Collection<E> {
    @Override
    public String toString() {
        return makeSymbolic();
    }

    public Object[] toArray() {
        executeConcretely();
        return new Object[0];
    }

    public <T> T[] toArray(T[] a) {
        assume(a != null);
        executeConcretely();
        return a;
    }

    @SuppressWarnings("Since15")
    public <T> T[] toArray(IntFunction<T[]> generator) {
        assume(generator != null);
        final int size = size();
        T[] data = generator.apply(size);
        return toArray(data);
    }
}
