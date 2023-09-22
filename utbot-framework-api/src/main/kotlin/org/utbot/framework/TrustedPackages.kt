package org.utbot.framework

import org.utbot.framework.plugin.api.ClassId
import soot.SootClass

/**
 * Cache for already discovered trusted/untrusted packages.
 */
private val isPackageTrusted: MutableMap<String, Boolean> = mutableMapOf()

/**
 * Determines whether [this] class is from trusted libraries as defined in [TrustedLibraries].
 */
fun SootClass.isFromTrustedLibrary(): Boolean = isFromTrustedLibrary(packageName)

/**
 * Determines whether [this] class is from trusted libraries as defined in [TrustedLibraries].
 */
fun ClassId.isFromTrustedLibrary(): Boolean = isFromTrustedLibrary(packageName)

/**
 * Determines whether [packageName] is from trusted libraries as defined in [TrustedLibraries].
 */
fun isFromTrustedLibrary(packageName: String): Boolean {
    isPackageTrusted[packageName]?.let {
        return it
    }

    val isTrusted = TrustedLibraries.trustedLibraries.any { packageName.startsWith(it, ignoreCase = false) }

    return isTrusted.also { isPackageTrusted[packageName] = it }
}
