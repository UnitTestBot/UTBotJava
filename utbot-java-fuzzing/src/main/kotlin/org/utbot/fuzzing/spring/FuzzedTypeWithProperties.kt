package org.utbot.fuzzing.spring

import org.utbot.common.DynamicProperties
import org.utbot.common.DynamicProperty
import org.utbot.common.dynamicPropertiesOf
import org.utbot.common.plus
import org.utbot.common.withoutProperty
import org.utbot.fuzzer.FuzzedType

typealias FuzzedTypeProperties = DynamicProperties<FuzzedTypeWithProperties>
typealias FuzzedTypeProperty<T> = DynamicProperty<FuzzedTypeWithProperties, T>
typealias FuzzedTypeFlag = FuzzedTypeProperty<Unit>

data class FuzzedTypeWithProperties(
    val origin: FuzzedType,
    val properties: FuzzedTypeProperties
) : FuzzedType(origin.classId, origin.generics)

val FuzzedType.origin: FuzzedType
    get() =
        if (this is FuzzedTypeWithProperties) origin
        else this

val FuzzedType.properties: FuzzedTypeProperties
    get() =
        if (this is FuzzedTypeWithProperties) properties
        else dynamicPropertiesOf()

fun FuzzedType.withoutProperty(property: FuzzedTypeProperty<*>): FuzzedTypeWithProperties =
    FuzzedTypeWithProperties(origin, properties.withoutProperty(property))

fun FuzzedType.addProperties(properties: FuzzedTypeProperties): FuzzedTypeWithProperties =
    FuzzedTypeWithProperties(origin, this.properties + properties)

/**
 * Unlike [addProperties] adds [properties] not just to the type itself, but also to its
 * [FuzzedType.generics], generics of generics, and so on.
 */
fun FuzzedType.addPropertiesDeeply(properties: FuzzedTypeProperties): FuzzedTypeWithProperties {
    val cache = mutableMapOf<FuzzedType, FuzzedTypeWithProperties>()

    fun FuzzedType.addPropertiesDeeply(): FuzzedTypeWithProperties {
        cache[this]?.let { return it }
        val generics = mutableListOf<FuzzedTypeWithProperties>()
        val result = FuzzedTypeWithProperties(
            origin = FuzzedType(classId, generics),
            properties = this.properties + properties
        )
        cache[this] = result
        this.generics.forEach { generics.add(it.addPropertiesDeeply()) }
        return result
    }

    return addPropertiesDeeply()
}