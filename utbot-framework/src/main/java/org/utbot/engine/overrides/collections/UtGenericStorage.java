package org.utbot.engine.overrides.collections;

public interface UtGenericStorage<E> {
    /**
     * Auxiliary method that tells engine to add constraint, that binds type parameter of this object
     * with type parameter of the specified range modifiable array.
     *
     * @param elements - array, type parameter of which need to be bound.
     */
    @SuppressWarnings("unused")
    default void setEqualGenericType(RangeModifiableUnlimitedArray<E> elements) {}

    /**
     * Auxiliary method that tells engine to add constraint, that binds type parameter of this storage
     * to the type of the specified object value.
     */
    @SuppressWarnings("unused")
    default void setGenericTypeToTypeOfValue(RangeModifiableUnlimitedArray<E> array, E value) {}
}
