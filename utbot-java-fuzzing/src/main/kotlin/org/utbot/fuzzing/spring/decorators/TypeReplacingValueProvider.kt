package org.utbot.fuzzing.spring.decorators

import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Scope
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

fun <T, R, D : Description<T>> ValueProvider<T, R, D>.replaceTypes(typeReplacer: (D, T) -> T) =
    TypeReplacingValueProvider(delegate = this, typeReplacer)

class TypeReplacingValueProvider<T, R, D : Description<T>>(
    delegate: ValueProvider<T, R, D>,
    private val typeReplacer: (D, T) -> T
) : ValueProviderDecorator<T, R, D>(delegate) {
    override fun wrap(provider: ValueProvider<T, R, D>): ValueProvider<T, R, D> =
        provider.replaceTypes(typeReplacer)

    override fun enrich(description: D, type: T, scope: Scope) =
        super.enrich(description, typeReplacer(description, type), scope)

    override fun accept(type: T): Boolean = true

    override fun generate(description: D, type: T): Sequence<Seed<T, R>> =
        if (super.accept(typeReplacer(description, type)))
            super.generate(description, typeReplacer(description, type))
        else
            emptySequence()
}
