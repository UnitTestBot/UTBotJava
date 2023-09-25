package org.utbot.fuzzing.spring.decorators

import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

fun <T, R, D : Description<T>> ValueProvider<T, R, D>.filterSeeds(predicate: (Seed<T, R>) -> Boolean) =
    SeedFilteringValueProvider(delegate = this, predicate)

class SeedFilteringValueProvider<T, R, D : Description<T>>(
    delegate: ValueProvider<T, R, D>,
    private val predicate: (Seed<T, R>) -> Boolean
) : ValueProviderDecorator<T, R, D>(delegate) {
    override fun wrap(provider: ValueProvider<T, R, D>): ValueProvider<T, R, D> =
        provider.filterSeeds(predicate)

    override fun generate(description: D, type: T): Sequence<Seed<T, R>> =
        delegate.generate(description, type).filter(predicate)
}