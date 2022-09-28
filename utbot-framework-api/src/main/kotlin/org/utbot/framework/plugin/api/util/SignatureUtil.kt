package org.utbot.framework.plugin.api.util

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KCallable

val KCallable<*>.signature: String
    get() = executableId.signature

val Executable.signature: String
    get() = when (this) {
        is Method -> executableId.signature
        is Constructor<*> -> executableId.signature
        else -> error("Unknown Executable type: ${this::class}")
    }

/**
 * Makes Java-like signature for constructor. "Like" because it uses "<init>" instead of class name.
 */
fun Constructor<*>.bytecodeSignature() = buildString {
    append("<init>(")
    parameterTypes.forEach { append(it.bytecodeSignature()) }
    append(")V")
}

/**
 * Makes Java signature for Java Class. Uses hack with [java.lang.reflect.Array.newInstance]
 */
//fun Class<*>.bytecodeSignature(): String = when {
//    this === Void.TYPE -> "V"
//    else -> newInstance(this, 0).toString().let {
//        it.substring(1, it.indexOf('@')).replace(".", "/")
//    }
//}


fun Class<*>.bytecodeSignature(): String = id.jvmName

/**
 * Method [Class.getName] works differently for arrays than for other types.
 * - When an element of an array is a reference type (e.g. `java.lang.Object`),
 * the array of `java.lang.Object` will have name `[Ljava.lang.Object;`.
 * - When an element of an array is a primitive type (e.g. `int`),
 * the array of `int` will have name `[I`.
 *
 * So, this property returns the name of the given class in the format of an array element type name.
 * Basically, it performs conversions for primitives and reference types (e.g. `int` -> `I`, `java.lang.Object` -> `Ljava.lang.Object;`.
 */
val ClassId.arrayLikeName: String
    get() = when {
        isPrimitive -> primitiveTypeJvmNameOrNull()!!
        isRefType -> "L$name;"
        else -> name
    }

fun String.toReferenceTypeBytecodeSignature(): String {
    val packageName = this
            .takeIf { "." in this }
            ?.substringBeforeLast(".")
            ?.replace(".", "/")
            ?.let { "$it/" }
            ?: ""
    val className = this.substringAfterLast(".")
    return "L$packageName$className;"
}

fun Class<*>.singleMethod(signature: String): Method =
        singleMethodOrNull(signature) ?: error("Can't find method $signature in $this")

fun Class<*>.singleConstructor(signature: String): Constructor<*> =
        singleConstructorOrNull(signature) ?: error("Can't find constructor $signature in $this")

fun Class<*>.singleMethodOrNull(signature: String): Method? =
        generateSequence(this) { it.superclass }.mapNotNull { clazz ->
            clazz.declaredMethods.firstOrNull { it.signature == signature }
        }.firstOrNull()

/**
 * Returns first declared constructor (even private and protected), matches signature, if it exists, null otherwise.
 * @see Class.getDeclaredConstructors
 */
fun Class<*>.singleConstructorOrNull(signature: String): Constructor<*>? =
    declaredConstructors.firstOrNull { it.bytecodeSignature() == signature }


/**
 * Finds callable for class method by signature. Supports constructors, static and non-static methods.
 */
fun Class<*>.singleExecutableId(signature: String): ExecutableId =
        singleExecutableIdOrNull(signature) ?: error("Can't find method $signature in $this")

/**
 * Finds callable for class method by signature. Supports constructors, static and non-static methods.
 */
fun Class<*>.singleExecutableIdOrNull(signature: String) = if (isConstructorSignature(signature)) {
    singleConstructorOrNull(signature)?.executableId
} else {
    singleMethodOrNull(signature)?.executableId
}

private fun isConstructorSignature(signature: String): Boolean = signature.startsWith("<init>")