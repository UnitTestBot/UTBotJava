package org.utbot.framework.codegen.model.util

import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.id

/**
 * For now we will count field accessible if it is not private and its class is also accessible,
 * because we generate tests in the same package with the class under test,
 * which means we can access public, protected and package-private fields
 *
 * @param packageName name of the package we check accessibility from
 */
fun FieldId.isAccessibleFrom(packageName: String): Boolean {
    val isClassAccessible = declaringClass.isAccessibleFrom(packageName)
    val isAccessibleByVisibility = isPublic || (declaringClass.packageName == packageName && (isPackagePrivate || isProtected))
    val isAccessibleFromPackageByModifiers = isAccessibleByVisibility && !isSynthetic

    return isClassAccessible && isAccessibleFromPackageByModifiers
}

/**
 * Whether or not a field can be set without reflection
 */
fun FieldId.canBeSetIn(packageName: String): Boolean = isAccessibleFrom(packageName) && !isFinal
