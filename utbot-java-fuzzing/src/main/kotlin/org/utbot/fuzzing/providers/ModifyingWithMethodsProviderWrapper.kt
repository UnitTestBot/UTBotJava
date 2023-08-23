package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Scope
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

class ModifyingWithMethodsProviderWrapper(
    private val classUnderTest: ClassId,
    private val delegate: JavaValueProvider
) : JavaValueProvider by delegate {

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> {
        TODO("Not yet implemented")
    }

    override fun enrich(description: FuzzedDescription, type: FuzzedType, scope: Scope) =
        delegate.enrich(description, type, scope)

    override fun accept(type: FuzzedType): Boolean = delegate.accept(type)

    override fun with(anotherValueProvider: JavaValueProvider): JavaValueProvider = delegate.with(anotherValueProvider)

    override fun except(anotherValueProvider: JavaValueProvider): JavaValueProvider = delegate.except(anotherValueProvider)

    override fun except(filter: (JavaValueProvider) -> Boolean): JavaValueProvider = delegate.except(filter)

    override fun map(transform: (JavaValueProvider) -> JavaValueProvider): JavaValueProvider = delegate.map(transform)

    override fun withFallback(fallback: JavaValueProvider): JavaValueProvider = delegate.withFallback(fallback)

    override fun withFallback(fallbackSupplier: (FuzzedType) -> Seed<FuzzedType, FuzzedValue>): JavaValueProvider =
        delegate.withFallback(fallbackSupplier)

    override fun unwrapIfFallback(): JavaValueProvider = delegate.unwrapIfFallback()

    override fun letIf(
        flag: Boolean,
        block: (JavaValueProvider) -> JavaValueProvider,
        ): ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> = delegate.letIf(flag, block)
}