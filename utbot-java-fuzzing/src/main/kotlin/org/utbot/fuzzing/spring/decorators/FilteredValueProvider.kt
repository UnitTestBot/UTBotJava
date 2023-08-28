package org.utbot.fuzzing.spring.decorators

import org.utbot.fuzzing.Description
import org.utbot.fuzzing.ValueProvider

fun <T, R, D : Description<T>> ValueProvider<T, R, D>.filterTypes(predicate: (T) -> Boolean) =
    FilteredValueProvider(delegate = this, predicate)

class FilteredValueProvider<T, R, D : Description<T>>(
    delegate: ValueProvider<T, R, D>,
    private val predicate: (T) -> Boolean
) : ValueProviderDecorator<T, R, D>(delegate) {
    override fun wrap(provider: ValueProvider<T, R, D>): ValueProvider<T, R, D> =
        provider.filterTypes(predicate)

    override fun accept(type: T): Boolean =
        predicate(type) && super.accept(type)
}