package org.utbot.fuzzing.spring.utils

import com.google.common.reflect.TypeToken
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedType
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

val Type.typeToken: TypeToken<*> get() = TypeToken.of(this)

val FuzzedType.jType: Type get() = toType(mutableMapOf())

private fun FuzzedType.toType(cache: MutableMap<FuzzedType, Type>): Type = cache.getOrPut(this) {
    when {
        generics.isEmpty() -> classId.jClass
        classId.isArray && generics.size == 1 -> GenericArrayType { generics.single().toType(cache) }
        else -> object : ParameterizedType {
            override fun getActualTypeArguments(): Array<Type> =
                generics.map { it.toType(cache) }.toTypedArray()

            override fun getRawType(): Type =
                classId.jClass

            override fun getOwnerType(): Type? = null
        }
    }
}

/**
 * Returns fully parameterized type, e.g. for `Map` class
 * `Map<K, V>` type is returned, where `K` and `V` are type variables.
 */
fun Class<*>.toTypeParametrizedByTypeVariables(): Type =
    if (typeParameters.isEmpty()) this
    else object : ParameterizedType {
        override fun getActualTypeArguments(): Array<Type> =
            typeParameters.toList().toTypedArray()

        override fun getRawType(): Type =
            this@toTypeParametrizedByTypeVariables

        override fun getOwnerType(): Type? =
            declaringClass?.toTypeParametrizedByTypeVariables()
    }
