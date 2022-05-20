package org.utbot.engine.overrides.collections;

public interface UtGenericAssociative<K, V> {
    /**
     * Auxiliary method that tells engine to add constraint, that binds type parameters of this object
     * with type parameters of the specified associative array.
     *
     * @param elements - associative array, type parameter of which need to be bound.
     */
    @SuppressWarnings("unused")
    default void setEqualGenericType(AssociativeArray<K, V> elements) {
    }
}
