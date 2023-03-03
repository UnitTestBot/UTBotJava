package org.utbot.framework.codegen.util

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.isFinal
import org.utbot.framework.plugin.api.util.isPackagePrivate
import org.utbot.framework.plugin.api.util.isProtected
import org.utbot.framework.plugin.api.util.isPublic
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.voidClassId

/**
 * For now we will count field accessible if it is not private and its class is also accessible,
 * because we generate tests in the same package with the class under test,
 * which means we can access public, protected and package-private fields
 *
 * @param context context in which code is generated (it is needed because the method needs to know package and language)
 * @param callerClassId object on which we try to access the field
 */
fun FieldId.isAccessibleFrom(packageName: String, callerClassId: ClassId): Boolean {
    val isClassAccessible = declaringClass.isAccessibleFrom(packageName)

    /*
    * There is a corner case which we ignore now.
    * Protected fields are accessible in nested classes of inheritors.
    */
    val isAccessibleByVisibility =
        isPublic ||
        isPackagePrivate && callerClassId.packageName == packageName && declaringClass.packageName == packageName ||
        isProtected && declaringClass.packageName == packageName
    val isAccessibleFromPackageByModifiers = isAccessibleByVisibility && !isSynthetic

    return isClassAccessible && isAccessibleFromPackageByModifiers
}

private fun FieldId.canBeReadViaGetterFrom(context: CgContext): Boolean =
    declaringClass.allMethods.contains(getter) && getter.isAccessibleFrom(context.testClassPackageName)

/**
 * Returns whether you can read field's value without reflection
 */
internal fun FieldId.canBeReadFrom(context: CgContext, callerClassId: ClassId): Boolean {
    if (context.codegenLanguage == CodegenLanguage.KOTLIN) {
        // Kotlin will allow direct field access for non-static fields with accessible getter
        if (!isStatic && canBeReadViaGetterFrom(context))
            return true
    }

    return isAccessibleFrom(context.testClassPackageName, callerClassId)
}

private fun FieldId.canBeSetViaSetterFrom(context: CgContext): Boolean =
    declaringClass.allMethods.contains(setter) && setter.isAccessibleFrom(context.testClassPackageName)

/**
 * Whether or not a field can be set without reflection
 */
internal fun FieldId.canBeSetFrom(context: CgContext, callerClassId: ClassId): Boolean {
    if (context.codegenLanguage == CodegenLanguage.KOTLIN) {
        // Kotlin will allow direct write access if both getter and setter is defined
        // !isAccessibleFrom(context) is important here because above rule applies to final fields only if they are not accessible in Java terms
        if (!isAccessibleFrom(context.testClassPackageName, callerClassId) && !isStatic && canBeReadViaGetterFrom(context) && canBeSetViaSetterFrom(context)) {
            return true
        }
    }

    return isAccessibleFrom(context.testClassPackageName, callerClassId) && !isFinal
}

/**
 * The default getter method for field (the one which is generated by Kotlin compiler)
 */
val FieldId.getter: MethodId
    get() = MethodId(declaringClass, "get${name.replaceFirstChar { it.uppercase() } }", type, emptyList())

/**
 * The default setter method for field (the one which is generated by Kotlin compiler)
 */
val FieldId.setter: MethodId
    get() = MethodId(declaringClass, "set${name.replaceFirstChar { it.uppercase() } }", voidClassId, listOf(type))
