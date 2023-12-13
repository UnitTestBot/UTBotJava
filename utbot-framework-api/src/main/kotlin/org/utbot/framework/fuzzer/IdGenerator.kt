package org.utbot.framework.fuzzer

/**
 * Identifier generator interface for fuzzer model providers.
 *
 * Provides fresh identifiers for generated models.
 *
 * Warning: specific generators are not guaranteed to be thread-safe.
 *
 * @param Id the identifier type (e.g., [Int] for [UtReferenceModel] providers)
 */
interface IdGenerator<Id> {
    /**
     * Create a fresh identifier. Each subsequent call should return a different value.
     *
     * The method is not guaranteed to be thread-safe, unless an implementation makes such a guarantee.
     */
    fun createId(): Id
}

/**
 * Identity preserving identifier generator interface.
 *
 * It allows to optionally save identifiers assigned to specific objects and later get the same identifiers
 * for these objects instead of fresh identifiers. This feature is necessary, for example, to implement reference
 * equality for enum models.
 *
 * Warning: specific generators are not guaranteed to be thread-safe.
 *
 * @param Id the identifier type (e.g., [Int] for [UtReferenceModel] providers)
 */
interface IdentityPreservingIdGenerator<Id> : IdGenerator<Id> {
    /**
     * Return an identifier for a specified non-null object. If an identifier has already been assigned
     * to an object, subsequent calls should return the same identifier for this object.
     *
     * Note: the interface does not specify whether reference equality or regular `equals`/`compareTo` equality
     * will be used to compare objects. Each implementation may provide these guarantees by itself.
     *
     * The method is not guaranteed to be thread-safe, unless an implementation makes such a guarantee.
     */
    fun getOrCreateIdForValue(value: Any): Id
}