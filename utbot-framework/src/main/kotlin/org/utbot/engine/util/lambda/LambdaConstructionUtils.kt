package org.utbot.engine.util.lambda

import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * This class represents the `type` and `value` of a value captured by lambda.
 * Captured values are represented as arguments of a synthetic method that lambda is compiled into,
 * hence the name of the class.
 */
data class CapturedArgument(val type: Class<*>, val value: Any?)

/**
 * @param clazz a class to create lookup instance for.
 * @return [MethodHandles.Lookup] instance for the given [clazz].
 * It can be used, for example, to search methods of this [clazz], even the `private` ones.
 */
private fun getLookupIn(clazz: Class<*>): MethodHandles.Lookup {
    val lookup = MethodHandles.lookup().`in`(clazz)

    // Allow lookup to access all members of declaringClass, including the private ones.
    // For example, it is useful to access private synthetic methods representing lambdas.
    val allowedModes = MethodHandles.Lookup::class.java.getDeclaredField("allowedModes")
    allowedModes.isAccessible = true
    allowedModes.setInt(
        lookup,
        Modifier.PUBLIC or Modifier.PROTECTED or Modifier.PRIVATE or Modifier.STATIC
    )

    return lookup
}

/**
 * @param lambdaMethod [Method] that represents a synthetic method for lambda.
 * @param capturedArgumentTypes types of values captured by lambda.
 * @return [MethodType] that represents the value of argument `instantiatedMethodType`
 * of method [LambdaMetafactory.metafactory].
 */
private fun getInstantiatedMethodType(
    lambdaMethod: Method,
    capturedArgumentTypes: Array<Class<*>>
): MethodType {
    // Types of arguments of synthetic method (representing lambda) without the types of captured values.
    val instantiatedMethodParamTypes = lambdaMethod.parameterTypes
        .drop(capturedArgumentTypes.size)
        .toTypedArray()

    return MethodType.methodType(lambdaMethod.returnType, instantiatedMethodParamTypes)
}

/**
 * @param declaringClass class where a lambda is declared.
 * @param lambdaName name of synthetic method that represents a lambda.
 * @return [Method] instance for the synthetic method that represent a lambda.
 */
private fun getLambdaMethod(declaringClass: Class<*>, lambdaName: String): Method {
    return declaringClass.declaredMethods.firstOrNull { it.name == lambdaName }
        ?: throw IllegalArgumentException("No lambda method named $lambdaName was found in class: ${declaringClass.canonicalName}")
}

/**
 * This class contains some info that is needed by both [constructLambda] and [constructStaticLambda].
 * We obtain this info in [prepareLambdaInfo] to avoid duplicated code in [constructLambda] and [constructStaticLambda].
 */
private data class LambdaMetafactoryInfo(
    val caller: MethodHandles.Lookup,
    val invokedName: String,
    val samMethodType: MethodType,
    val lambdaMethod: Method,
    val lambdaMethodType: MethodType
)

/**
 * Obtain and prepare [LambdaMetafactoryInfo] that is needed by [constructLambda] and [constructStaticLambda].
 */
private fun prepareLambdaInfo(
    samType: Class<*>,
    declaringClass: Class<*>,
    lambdaName: String,
): LambdaMetafactoryInfo {
    // Create lookup for class where the lambda is declared in.
    val caller = getLookupIn(declaringClass)

    // Obtain the single abstract method of a functional interface whose instance we are building.
    // For example, for `java.util.function.Predicate` it will be method `test`.
    val singleAbstractMethod = getSingleAbstractMethod(samType)

    val invokedName = singleAbstractMethod.name

    // Method type of single abstract method of the target functional interface.
    val samMethodType = MethodType.methodType(singleAbstractMethod.returnType, singleAbstractMethod.parameterTypes)

    val lambdaMethod = getLambdaMethod(declaringClass, lambdaName)
    lambdaMethod.isAccessible = true
    val lambdaMethodType = MethodType.methodType(lambdaMethod.returnType, lambdaMethod.parameterTypes)

    return LambdaMetafactoryInfo(caller, invokedName, samMethodType, lambdaMethod, lambdaMethodType)
}

/**
 * @param clazz functional interface
 * @return a [Method] for the single abstract method of the given functional interface `clazz`.
 */
private fun getSingleAbstractMethod(clazz: Class<*>): Method {
    val abstractMethods = clazz.methods.filter { Modifier.isAbstract(it.modifiers) }
    require(abstractMethods.isNotEmpty()) { "No abstract methods found in class: " + clazz.canonicalName }
    require(abstractMethods.size <= 1) { "More than one abstract method found in class: " + clazz.canonicalName }
    return abstractMethods[0]
}

/**
 * @return an [Any] that represents an instance of the given functional interface `samType`
 * and implements its single abstract method with the behavior of the given lambda.
 */
internal fun constructStaticLambda(
    samType: Class<*>,
    declaringClass: Class<*>,
    lambdaName: String,
    vararg capturedArguments: CapturedArgument
): Any {
    val (caller, invokedName, samMethodType, lambdaMethod, lambdaMethodType) =
        prepareLambdaInfo(samType, declaringClass, lambdaName)

    val lambdaMethodHandle = caller.findStatic(declaringClass, lambdaName, lambdaMethodType)

    val capturedArgumentTypes = capturedArguments.map { it.type }.toTypedArray()
    val invokedType = MethodType.methodType(samType, capturedArgumentTypes)
    val instantiatedMethodType = getInstantiatedMethodType(lambdaMethod, capturedArgumentTypes)

    // Create a CallSite for the given lambda.
    val site = LambdaMetafactory.metafactory(
        caller,
        invokedName,
        invokedType,
        samMethodType,
        lambdaMethodHandle,
        instantiatedMethodType
    )
    val capturedValues = capturedArguments.map { it.value }.toTypedArray()

    // Get MethodHandle and pass captured values to it to obtain an object
    // that represents the target functional interface instance.
    val handle = site.target
    return handle.invokeWithArguments(*capturedValues)
}

/**
 * @return an [Any] that represents an instance of the given functional interface `samType`
 * and implements its single abstract method with the behavior of the given lambda.
 */
internal fun constructLambda(
    samType: Class<*>,
    declaringClass: Class<*>,
    lambdaName: String,
    capturedReceiver: Any?,
    vararg capturedArguments: CapturedArgument
): Any {
    val (caller, invokedName, samMethodType, lambdaMethod, lambdaMethodType) =
        prepareLambdaInfo(samType, declaringClass, lambdaName)

    val lambdaMethodHandle = caller.findVirtual(declaringClass, lambdaName, lambdaMethodType)

    val capturedArgumentTypes = capturedArguments.map { it.type }.toTypedArray()
    val invokedType = MethodType.methodType(samType, declaringClass, *capturedArgumentTypes)
    val instantiatedMethodType = getInstantiatedMethodType(lambdaMethod, capturedArgumentTypes)

    // Create a CallSite for the given lambda.
    val site = LambdaMetafactory.metafactory(
        caller,
        invokedName,
        invokedType,
        samMethodType,
        lambdaMethodHandle,
        instantiatedMethodType
    )
    val capturedValues = mutableListOf<Any?>()
        .apply {
            add(capturedReceiver)
            val capturedArgumentValues = capturedArguments.map { it.value }
            addAll(capturedArgumentValues)
        }.toTypedArray()


    // Get MethodHandle and pass captured values to it to obtain an object
    // that represents the target functional interface instance.
    val handle = site.target
    return handle.invokeWithArguments(*capturedValues)
}
