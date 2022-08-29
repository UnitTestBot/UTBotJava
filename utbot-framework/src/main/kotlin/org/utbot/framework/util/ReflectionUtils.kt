package org.utbot.framework.util

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
