package org.utbot.framework.plugin.api.util

import org.utbot.common.Reflection
import org.utbot.framework.plugin.api.FieldId
import soot.RefType

/**
 * Several fields are inaccessible in runtime even via reflection
 */
val FieldId.isInaccessibleViaReflection: Boolean
    get() {
        val declaringClassName = declaringClass.name
        return declaringClassName in inaccessibleViaReflectionClasses ||
                (name to declaringClassName) in inaccessibleViaReflectionFields
    }

val RefType.isInaccessibleViaReflection: Boolean
    get() {
        return className in inaccessibleViaReflectionClasses
    }

private val inaccessibleViaReflectionClasses = setOf(
    "jdk.internal.reflect.ReflectionFactory",
    "jdk.internal.reflect.Reflection",
    "jdk.internal.loader.ClassLoaderValue",
    "sun.reflect.Reflection",
)

private val inaccessibleViaReflectionFields = setOf(
    "security" to "java.lang.System",
)

@Suppress("DEPRECATION")
val Class<*>.anyInstance: Any
    get() {
//        val defaultCtor = declaredConstructors.singleOrNull { it.parameterCount == 0}
//        if (defaultCtor != null) {
//            try {
//                defaultCtor.isAccessible = true
//                return defaultCtor.newInstance()
//            } catch (e : Throwable) {
//                logger.warn(e) { "Can't create object with default ctor. Fallback to Unsafe." }
//            }
//        }
        return Reflection.unsafe.allocateInstance(this)

//        val constructors = runCatching {
//            arrayOf(getDeclaredConstructor())
//        }.getOrElse { declaredConstructors }
//
//        return constructors.asSequence().mapNotNull { constructor ->
//            runCatching {
//                val parameters = constructor.parameterTypes.map { defaultParameterValue(it) }
//                val isAccessible = constructor.isAccessible
//                try {
//                    constructor.isAccessible = true
//                    constructor.newInstance(*parameters.toTypedArray())
//                } finally {
//                    constructor.isAccessible = isAccessible
//                }
//            }.getOrNull()
//        }.firstOrNull() ?: error("Failed to create instance of $this")
    }