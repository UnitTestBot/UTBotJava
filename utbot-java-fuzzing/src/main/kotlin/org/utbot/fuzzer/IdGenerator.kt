package org.utbot.fuzzer

import org.utbot.framework.plugin.api.UtModel
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

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

/**
 * An identity preserving id generator for fuzzer value providers that returns identifiers of type [Int].
 *
 * When identity-preserving identifier is requested, objects are compared by reference.
 * The generator is not thread-safe.
 *
 * @param lowerBound an integer value so that any generated identifier is strictly greater than it.
 *
 * Warning: when generating [UtReferenceModel] identifiers, no identifier should be equal to zero,
 * as this value is reserved for [UtNullModel]. To guarantee it, [lowerBound] should never be negative.
 * Avoid using custom lower bounds (maybe except fuzzer unit tests), use the predefined default value instead.
 */
class ReferencePreservingIntIdGenerator(lowerBound: Int = DEFAULT_LOWER_BOUND) : IdentityPreservingIdGenerator<Int> {
    private val lastId: AtomicInteger = AtomicInteger(lowerBound)
    private val cache: IdentityHashMap<Any?, Int> = IdentityHashMap()

    override fun getOrCreateIdForValue(value: Any): Int {
        return cache.getOrPut(value) { createId() }
    }

    override fun createId(): Int {
        return lastId.incrementAndGet()
    }

    companion object {
        /**
         * The default lower bound (all generated integer identifiers will be greater than it).
         *
         * It is defined as a large value because all synthetic [UtModel] instances
         * must have greater identifiers than the real models.
         */
        const val DEFAULT_LOWER_BOUND: Int = 1500_000_000
    }
}

fun UtModel.fuzzed(block: FuzzedValue.() -> Unit = {}): FuzzedValue = FuzzedValue(this).apply(block)