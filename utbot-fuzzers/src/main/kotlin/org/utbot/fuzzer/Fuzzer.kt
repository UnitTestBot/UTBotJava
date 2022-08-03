package org.utbot.fuzzer

import org.utbot.fuzzer.providers.ConstantsModelProvider
import org.utbot.fuzzer.providers.ObjectModelProvider
import org.utbot.fuzzer.providers.PrimitivesModelProvider
import org.utbot.fuzzer.providers.StringConstantModelProvider
import mu.KotlinLogging
import org.utbot.fuzzer.providers.ArrayModelProvider
import org.utbot.fuzzer.providers.CharToStringModelProvider
import org.utbot.fuzzer.providers.CollectionModelProvider
import org.utbot.fuzzer.providers.PrimitiveDefaultsModelProvider
import org.utbot.fuzzer.providers.EnumModelProvider
import org.utbot.fuzzer.providers.PrimitiveWrapperModelProvider
import java.lang.IllegalArgumentException
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

private val logger by lazy { KotlinLogging.logger {} }

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

fun fuzz(description: FuzzedMethodDescription, vararg modelProviders: ModelProvider): Sequence<List<FuzzedValue>> {
    if (modelProviders.isEmpty()) {
        throw IllegalArgumentException("At least one model provider is required")
    }

    val values = List<MutableList<FuzzedValue>>(description.parameters.size) { mutableListOf() }
    modelProviders.forEach { fuzzingProvider ->
        fuzzingProvider.generate(description).forEach { (index, model) ->
            values[index].add(model)
        }
    }
    description.parameters.forEachIndexed { index, classId ->
        val models = values[index]
        if (models.isEmpty()) {
            logger.warn { "There's no models provided classId=$classId. No suitable values are generated for ${description.name}" }
            return emptySequence()
        }
    }
    return CartesianProduct(values, Random(0L)).asSequence()
}

/**
 * Creates a model provider from a list of default providers.
 */
fun defaultModelProviders(idGenerator: IdentityPreservingIdGenerator<Int>): ModelProvider {
    return ModelProvider.of(
        ObjectModelProvider(idGenerator),
        CollectionModelProvider(idGenerator),
        ArrayModelProvider(idGenerator),
        EnumModelProvider(idGenerator),
        ConstantsModelProvider,
        StringConstantModelProvider,
        CharToStringModelProvider,
        PrimitivesModelProvider,
        PrimitiveWrapperModelProvider,
    )
}

/**
 * Creates a model provider for [ObjectModelProvider] that generates values for object constructor.
 */
fun objectModelProviders(idGenerator: IdentityPreservingIdGenerator<Int>): ModelProvider {
    return ModelProvider.of(
        CollectionModelProvider(idGenerator),
        ArrayModelProvider(idGenerator),
        EnumModelProvider(idGenerator),
        StringConstantModelProvider,
        CharToStringModelProvider,
        ConstantsModelProvider,
        PrimitiveDefaultsModelProvider,
        PrimitiveWrapperModelProvider,
    )
}
