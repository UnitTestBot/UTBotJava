package org.utbot.fuzzing

import mu.KotlinLogging
import kotlin.random.Random

private val logger by lazy { KotlinLogging.logger {} }

/**
 * Entry point to run fuzzing.
 */
suspend fun <TYPE, RESULT, DESCRIPTION : Description<TYPE>, FEEDBACK : Feedback<TYPE, RESULT>> runFuzzing(
    provider: ValueProvider<TYPE, RESULT, DESCRIPTION>,
    description: DESCRIPTION,
    random: Random = Random(0),
    configuration: Configuration = Configuration(),
    handle: suspend (description: DESCRIPTION, values: List<RESULT>) -> FEEDBACK
) {
    BaseFuzzing(listOf(provider), handle).fuzz(description, random, configuration)
}

/**
 * Implements base concepts that use providers to generate values for some types.
 *
 * @param providers is a list of "type to values" generator
 * @param exec this function is called when fuzzer generates values of type R to run it with target program.
 */
class BaseFuzzing<T, R, D : Description<T>, F : Feedback<T, R>>(
    val providers: List<ValueProvider<T, R, D>>,
    val exec: suspend (description: D, values: List<R>) -> F
) : Fuzzing<T, R, D, F> {

    var update: (D, Statistic<T>, Configuration) -> Unit = { d, s, c -> super.update(d, s, c) }

    constructor(vararg providers: ValueProvider<T, R, D>, exec: suspend (description: D, values: List<R>) -> F) : this(providers.toList(), exec)

    override fun generate(description: D, type: T): Sequence<Seed<T, R>> {
        return providers.asSequence().flatMap { provider ->
            try {
                if (provider.accept(type)) {
                    provider.generate(description, type)
                } else {
                    emptySequence()
                }
            } catch (t: Throwable) {
                logger.error(t) { "Error occurs in value provider: $provider" }
                emptySequence()
            }
        }
    }

    override suspend fun handle(description: D, values: List<R>): F {
        return exec(description, values)
    }

    override fun update(description: D, statistic: Statistic<T>, configuration: Configuration) {
        update.invoke(description, statistic, configuration)
    }
}

/**
 * Value provider generates [Seed] and has other methods to combine providers.
 */
fun interface ValueProvider<T, R, D : Description<T>> {
    /**
     * Generate a sequence of [Seed] that is merged with values generated by other provider.
     */
    fun generate(description: D, type: T): Sequence<Seed<T, R>>

    /**
     * Validates if this provider is applicable to some type.
     */
    fun accept(type: T): Boolean = true

    /**
     * Combines this model provider with `anotherValueProviders` into one instance.
     *
     * This model provider is called before `anotherValueProviders`.
     */
    infix fun with(anotherValueProvider: ValueProvider<T, R, D>): ValueProvider<T, R, D> {
        fun toList(m: ValueProvider<T, R, D>) = if (m is Combined<T, R, D>) m.providers else listOf(m)
        return Combined(toList(this) + toList(anotherValueProvider))
    }

    /**
     * Removes `anotherValueProviders<T, R` from current one.
     */
    fun except(anotherValueProvider: ValueProvider<T, R, D>): ValueProvider<T, R, D> {
        return except { it == anotherValueProvider }
    }

    /**
     * Removes `anotherValueProviders<T, R` from current one.
     */
    fun except(filter: (ValueProvider<T, R, D>) -> Boolean): ValueProvider<T, R, D> {
        return if (this is Combined) {
            Combined(providers.filterNot(filter))
        } else {
            Combined(if (filter(this)) emptyList() else listOf(this))
        }
    }

    /**
     * Applies [transform] for current provider
     */
    fun map(transform: (ValueProvider<T, R, D>) -> ValueProvider<T, R, D>): ValueProvider<T, R, D> {
        return if (this is Combined) {
            Combined(providers.map(transform))
        } else {
            transform(this)
        }
    }

    fun withFallback(fallback: ValueProvider<T, R, D>) : ValueProvider<T, R, D> {
        val thisProvider = this
        return ValueProvider { description, type ->
            val default = if (accept(type)) thisProvider.generate(description, type) else emptySequence()
            if (default.iterator().hasNext()) {
                default
            } else if (fallback.accept(type)) {
                fallback.generate(description, type)
            } else {
                emptySequence()
            }
        }
    }

    /**
     * Creates new value provider that creates default value if no values are generated by this provider.
     */
    fun withFallback(fallbackSupplier: (T) -> Seed<T, R>) : ValueProvider<T, R, D> {
        return withFallback { _, type ->
            sequenceOf(fallbackSupplier(type))
        }
    }

    /**
     * Wrapper class that delegates implementation to the [providers].
     */
    private class Combined<T, R, D : Description<T>>(providers: List<ValueProvider<T, R, D>>): ValueProvider<T, R, D> {
        val providers: List<ValueProvider<T, R, D>>

        init {
            // Flattening to avoid Combined inside Combined (for correct work of except, map, etc.)
            this.providers = providers.flatMap {
                if (it is Combined)
                    it.providers
                else
                    listOf(it)
            }
        }

        override fun accept(type: T): Boolean {
            return providers.any { it.accept(type) }
        }

        override fun generate(description: D, type: T): Sequence<Seed<T, R>> = sequence {
            providers.asSequence().filter { it.accept(type) }.forEach { provider ->
                provider.generate(description, type).forEach {
                    yield(it)
                }
            }
        }
    }

    companion object {
        fun <T, R, D : Description<T>> of(valueProviders: List<ValueProvider<T, R, D>>): ValueProvider<T, R, D> {
            return Combined(valueProviders)
        }
    }
}

/**
 * Simple value provider for a concrete type.
 *
 * @param type that is used as a filter to call this provider
 * @param generate yields values for the type
 */
class TypeProvider<T, R, D : Description<T>>(
    val type: T,
    val generate: suspend SequenceScope<Seed<T, R>>.(description: D, type: T) -> Unit
) : ValueProvider<T, R, D> {
    override fun accept(type: T) = this.type == type
    override fun generate(description: D, type: T) = sequence {
        if (accept(type)) {
            this.generate(description, type)
        }
    }
}
