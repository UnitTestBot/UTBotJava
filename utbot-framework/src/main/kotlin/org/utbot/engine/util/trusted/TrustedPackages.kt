package org.utbot.engine.util.trusted

import org.utbot.framework.TrustedLibraries
import soot.SootClass

/**
 * Cache for already discovered trusted/untrusted packages.
 */
private val isPackageTrusted: MutableMap<String, Boolean> = mutableMapOf()

/**
 * Determines whether [this] class is from trusted libraries as defined in [TrustedLibraries].
 */
fun SootClass.isFromTrustedLibrary(): Boolean {
    isPackageTrusted[packageName]?.let {
        return it
    }

    val isTrusted = TrustedLibraries.trustedLibraries.any { packageName.startsWith(it, ignoreCase = false) }

    return isTrusted.also { isPackageTrusted[packageName] = it }
}
