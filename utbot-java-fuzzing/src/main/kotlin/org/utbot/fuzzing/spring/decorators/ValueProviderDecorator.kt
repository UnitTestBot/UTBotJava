package org.utbot.fuzzing.spring.decorators

import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Scope
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

abstract class ValueProviderDecorator<T, R, D : Description<T>>(
    protected val delegate: ValueProvider<T, R, D>
) : ValueProvider<T, R, D> {
    protected abstract fun wrap(provider: ValueProvider<T, R, D>): ValueProvider<T, R, D>

    override fun enrich(description: D, type: T, scope: Scope) =
        delegate.enrich(description, type, scope)

    override fun accept(type: T): Boolean =
        delegate.accept(type)

    override fun generate(description: D, type: T): Sequence<Seed<T, R>> =
        delegate.generate(description, type)

    override fun map(transform: (ValueProvider<T, R, D>) -> ValueProvider<T, R, D>): ValueProvider<T, R, D> =
        transform(wrap(delegate.map(transform)))
}