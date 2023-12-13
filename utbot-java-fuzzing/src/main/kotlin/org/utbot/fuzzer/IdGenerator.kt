package org.utbot.fuzzer

import org.utbot.framework.fuzzer.IdentityPreservingIdGenerator
import org.utbot.framework.plugin.api.UtModel
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

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