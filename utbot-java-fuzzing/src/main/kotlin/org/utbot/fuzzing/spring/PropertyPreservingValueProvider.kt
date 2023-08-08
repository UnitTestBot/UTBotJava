package org.utbot.fuzzing.spring

import org.utbot.common.toDynamicProperties
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Scope
import org.utbot.fuzzing.Seed

/**
 * @see preserveProperties
 */
interface PreservableFuzzedTypeProperty<T> : FuzzedTypeProperty<T>

/**
 * Returns wrapper of [this] provider that preserves [preservable properties][PreservableFuzzedTypeProperty],
 * i.e. all [FuzzedType]s mentioned in any returned [Seed] will have all [preservable properties]
 * [PreservableFuzzedTypeProperty] of the type that was used to generate that seed.
 *
 * That's useful if we want a whole part of the object tree created by fuzzer to possess some property
 * (e.g. all entities used to create MANAGED entity should themselves be also MANAGED).
 */
fun JavaValueProvider.preserveProperties() : JavaValueProvider =
    PropertyPreservingValueProvider(this)

class PropertyPreservingValueProvider(private val delegateProvider: JavaValueProvider) : JavaValueProvider {
    override fun enrich(description: FuzzedDescription, type: FuzzedType, scope: Scope) =
        delegateProvider.enrich(description, type, scope)

    override fun accept(type: FuzzedType): Boolean = delegateProvider.accept(type)

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> {
        val delegateSeeds = delegateProvider.generate(description, type)

        val preservedProperties = type.properties.entries
            .filter { it.property is PreservableFuzzedTypeProperty }
            .toDynamicProperties()
        if (preservedProperties.entries.isEmpty()) return delegateSeeds

        fun List<FuzzedType>.addPreservedProperties() = map { it.addProperties(preservedProperties) }

        return delegateSeeds.map { seed ->
            when (seed) {
                is Seed.Recursive -> Seed.Recursive(
                    construct = Routine.Create(
                        types = seed.construct.types.addPreservedProperties(),
                        builder = seed.construct.builder
                    ),
                    modify = seed.modify.map { modification ->
                        Routine.Call(
                            types = modification.types.addPreservedProperties(),
                            callable = modification.callable
                        )
                    },
                    empty = seed.empty
                )
                is Seed.Collection -> Seed.Collection(
                    construct = seed.construct,
                    modify = Routine.ForEach(
                        types = seed.modify.types.addPreservedProperties(),
                        callable = seed.modify.callable
                    )
                )
                is Seed.Known<FuzzedType, FuzzedValue, *>, is Seed.Simple -> seed
            }
        }
    }

    override fun map(transform: (JavaValueProvider) -> JavaValueProvider): JavaValueProvider =
        delegateProvider.map(transform).preserveProperties()

    override fun except(filter: (JavaValueProvider) -> Boolean): JavaValueProvider =
        delegateProvider.except(filter).preserveProperties()
}