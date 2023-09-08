package org.utbot.fuzzer

import com.google.common.reflect.TypeResolver
import com.google.common.reflect.TypeToken
import mu.KotlinLogging
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.constructor
import org.utbot.framework.plugin.api.util.executable
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.method
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Optional

private val logger = KotlinLogging.logger {}
private val loggedUnresolvedExecutables = mutableSetOf<ExecutableId>()

val Type.typeToken: TypeToken<*> get() = TypeToken.of(this)
inline fun <reified T> typeTokenOf(): TypeToken<T> = object : TypeToken<T>() {}
inline fun <reified T> jTypeOf(): Type = typeTokenOf<T>().type

val FuzzedType.jType: Type get() = toType(cache = mutableMapOf())

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

/**
 * Returns types of arguments that should be passed to [executableId] for it to return [neededType] and `null` iff
 * [executableId] can't return [neededType] (e.g. if it returns `List<String>` while `List<Integer>` is needed).
 *
 * For example, if [executableId] is [Optional.of] and [neededType] is `Optional<String>`,
 * then one element list containing `String` type is returned.
 */
fun resolveParameterTypes(
    executableId: ExecutableId,
    neededType: Type
): List<Type>? {
    return try {
        val actualType = when (executableId) {
            is MethodId -> executableId.method.genericReturnType
            is ConstructorId -> executableId.constructor.declaringClass.toTypeParametrizedByTypeVariables()
        }

        val neededClass = neededType.typeToken.rawType
        val actualClass = actualType.typeToken.rawType

        if (!neededClass.isAssignableFrom(actualClass))
            return null

        @Suppress("UNCHECKED_CAST")
        val actualSuperType = actualType.typeToken.getSupertype(neededClass as Class<in Any>).type
        val typeResolver = try {
            TypeResolver().where(actualSuperType, neededType)
        } catch (e: Exception) {
            // TypeResolver.where() throws an exception when unification of actual & needed types fails
            // e.g. when unifying Optional<Integer> and Optional<String>
            return null
        }

        // in some cases when bounded wildcards are involved TypeResolver.where() doesn't throw even though types are
        // incompatible (e.g. when needed type is `List<? super Integer>` while actual super type is `List<String>`)
        if (!typeResolver.resolveType(actualSuperType).typeToken.isSubtypeOf(neededType))
            return null

        executableId.executable.genericParameterTypes.map {
            typeResolver.resolveType(it)
        }
    } catch (e: Exception) {
        if (loggedUnresolvedExecutables.add(executableId))
            logger.error(e) { "Failed to resolve types for $executableId, using unresolved generic type" }

        executableId.executable.genericParameterTypes.toList()
    }
}
