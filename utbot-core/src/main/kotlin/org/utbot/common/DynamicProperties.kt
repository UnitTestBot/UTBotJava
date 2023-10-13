package org.utbot.common

import java.util.*

/**
 * @param TOwner used purely to make type system enforce the use of properties with correct receiver,
 * e.g. if property `NotEmptyTypeFlag` is defined for `FuzzedType` it can't be used on `CgContext`.
 *
 * **See also:** [this post](https://stackoverflow.com/a/58219723/10536125).
 */
interface DynamicProperty<TOwner, T>

data class InitialisedDynamicProperty<TOwner, T>(
    val property: DynamicProperty<TOwner, T>,
    val value: T
)

fun <TOwner, T> DynamicProperty<TOwner, T>.withValue(value: T) =
    InitialisedDynamicProperty(this, value)

interface DynamicProperties<TOwner> {
    val entries: Set<InitialisedDynamicProperty<TOwner, *>>

    operator fun <T> get(property: DynamicProperty<TOwner, T>): T?
    fun <T> getValue(property: DynamicProperty<TOwner, T>): T
    operator fun contains(property: DynamicProperty<TOwner, *>): Boolean

    /**
     * Two instances of [DynamicProperties] implementations are equal iff their [entries] are equal
     */
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

interface MutableDynamicProperties<TOwner> : DynamicProperties<TOwner> {
    operator fun <T> set(property: DynamicProperty<TOwner, T>, value: T)
}

fun <TOwner, T> MutableDynamicProperties<TOwner>.getOrPut(property: DynamicProperty<TOwner, T>, default: () -> T): T {
    if (property !in this) set(property, default())
    return getValue(property)
}

fun <TOwner> Iterable<InitialisedDynamicProperty<TOwner, *>>.toMutableDynamicProperties(): MutableDynamicProperties<TOwner> =
    DynamicPropertiesImpl<TOwner>().also { properties ->
        forEach { properties.add(it) }
    }

fun <TOwner> Iterable<InitialisedDynamicProperty<TOwner, *>>.toDynamicProperties(): DynamicProperties<TOwner> =
    toMutableDynamicProperties()

fun <TOwner> mutableDynamicPropertiesOf(
    vararg initialisedDynamicProperties: InitialisedDynamicProperty<TOwner, *>
): MutableDynamicProperties<TOwner> = initialisedDynamicProperties.asIterable().toMutableDynamicProperties()

fun <TOwner> dynamicPropertiesOf(
    vararg initialisedDynamicProperties: InitialisedDynamicProperty<TOwner, *>
): DynamicProperties<TOwner> = initialisedDynamicProperties.asIterable().toDynamicProperties()

fun <TOwner> DynamicProperties<TOwner>.withoutProperty(property: DynamicProperty<TOwner, *>): DynamicProperties<TOwner> =
    entries.filterNot { it.property == property }.toMutableDynamicProperties()

operator fun <TOwner> DynamicProperties<TOwner>.plus(other: DynamicProperties<TOwner>): DynamicProperties<TOwner> =
    (entries + other.entries).toMutableDynamicProperties()

class DynamicPropertiesImpl<TOwner> : MutableDynamicProperties<TOwner> {
    /**
     * Actual type of [properties] should be `Map<DynamicProperty<TOwner, T>, T>`, but
     * such type is not representable withing kotlin type system, hence unchecked casts are
     * used later.
     */
    private val properties = IdentityHashMap<DynamicProperty<TOwner, *>, Any?>()
    override val entries: Set<InitialisedDynamicProperty<TOwner, *>>
        get() = properties.mapTo(mutableSetOf()) { unsafeInitializedDynamicProperty(it.key, it.value) }

    @Suppress("UNCHECKED_CAST")
    private fun <T> unsafeInitializedDynamicProperty(property: DynamicProperty<TOwner, T>, value: Any?) =
        InitialisedDynamicProperty(property, value as T)

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(property: DynamicProperty<TOwner, T>): T? =
        properties[property] as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(property: DynamicProperty<TOwner, T>): T =
        properties.getValue(property) as T

    override fun <T> set(property: DynamicProperty<TOwner, T>, value: T) {
        properties[property] = value
    }

    override fun contains(property: DynamicProperty<TOwner, *>): Boolean =
        property in properties

    fun <T> add(initialisedDynamicProperty: InitialisedDynamicProperty<TOwner, T>) =
        set(initialisedDynamicProperty.property, initialisedDynamicProperty.value)

    override fun toString(): String {
        return "DynamicPropertiesImpl(properties=$properties)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DynamicProperties<*>) return false
        return entries == other.entries
    }

    override fun hashCode(): Int =
        properties.hashCode()
}
