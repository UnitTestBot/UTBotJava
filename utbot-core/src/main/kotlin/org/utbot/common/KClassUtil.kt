package org.utbot.common

import java.lang.reflect.Array
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KClass

val Class<*>.nameOfPackage: String get() = `package`?.name?:""

fun Method.invokeCatching(obj: Any?, args: List<Any?>) = try {
    val invocation = if (isVarArgs) {
        // In java only last parameter could be vararg
        val firstNonVarargParametersCount = parameterCount - 1

        val varargArray = constructVarargParameterArray(firstNonVarargParametersCount, args)

        if (firstNonVarargParametersCount == 0) {
            // Only vararg parameter, just pass only it as an array
            invoke(obj, varargArray)
        } else {
            // Pass first non vararg parameters as vararg, and the vararg parameter as an array
            val firstNonVarargParameters = args.take(firstNonVarargParametersCount)

            invoke(obj, *firstNonVarargParameters.toTypedArray(), varargArray)
        }
    } else {
        invoke(obj, *args.toTypedArray())
    }

    Result.success(invocation)
} catch (e: InvocationTargetException) {
    Result.failure<Nothing>(e.targetException)
}

// https://stackoverflow.com/a/59857242
private fun Method.constructVarargParameterArray(firstNonVarargParametersCount: Int, args: List<Any?>): Any {
    val varargCount = args.size - firstNonVarargParametersCount
    val varargElements = args.drop(firstNonVarargParametersCount)

    val varargElementType = parameterTypes.last().componentType
    requireNotNull(varargElementType) {
        "Vararg parameter of method $this was expected to be array but ${parameterTypes.last()} found"
    }

    val varargArray = Array.newInstance(parameterTypes.last().componentType, varargCount)

    varargElements.forEachIndexed { index, value ->
        Array.set(varargArray, index, value)
    }

    return varargArray
}

val KClass<*>.allNestedClasses: List<KClass<*>>
    get() = listOf(this) + nestedClasses.flatMap { it.allNestedClasses }
