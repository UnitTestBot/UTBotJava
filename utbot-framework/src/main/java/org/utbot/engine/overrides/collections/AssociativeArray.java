package org.utbot.engine.overrides.collections;

/**
 * Interface shows API for UtArrayExpressionBase.
 */
public class AssociativeArray<I, T> {
    int size;

    @SuppressWarnings("unused")
    T[] storage;
    I[] touched;

    /**
     * UtArraySelectExpression(this, index)
     */
    @SuppressWarnings("unused")
    T select(I index) {
        return null;
    }

    /**
     * UtArrayMultiStoreExpression(this, UtStore(index, element))
     */
    @SuppressWarnings("unused")
    void store(I index, T element) {
    }
}
