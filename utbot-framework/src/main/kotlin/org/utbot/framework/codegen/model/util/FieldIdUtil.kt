package org.utbot.framework.codegen.model.util

import kotlinx.coroutines.runBlocking
import org.utbot.framework.plugin.api.isFinal
import org.utbot.framework.plugin.api.packageName
import org.utbot.framework.plugin.api.util.id
import org.utbot.jcdb.api.*

/**
 * For now we will count field accessible if it is not private and its class is also accessible,
 * because we generate tests in the same package with the class under test,
 * which means we can access public, protected and package-private fields
 *
 * @param packageName name of the package we check accessibility from
 */
fun FieldId.isAccessibleFrom(packageName: String): Boolean = runBlocking {
    val isClassAccessible = classId.isAccessibleFrom(packageName)
    val isAccessibleByVisibility = isPublic() || (classId.packageName == packageName && (isPackagePrivate() || isProtected()))
    val isAccessibleFromPackageByModifiers = isAccessibleByVisibility && !isSynthetic()

    isClassAccessible && isAccessibleFromPackageByModifiers
}

/**
 * Whether or not a field can be set without reflection
 */
fun FieldId.canBeSetIn(packageName: String): Boolean = isAccessibleFrom(packageName) && !isFinal

private val systemClassId get() = System::class.id

/**
 * Security field is inaccessible in Runtime even via reflection.
 */
val FieldId.isInaccessible: Boolean
    get() = name == "security" && classId == systemClassId
