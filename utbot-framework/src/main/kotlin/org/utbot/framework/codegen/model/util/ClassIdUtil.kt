package org.utbot.framework.codegen.model.util

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isArray

/**
 * For now we will count class accessible if it is:
 * - Public or package-private within package [packageName].
 * - It's outer class (if exists) is accessible too.
 * NOTE: local and synthetic classes are considered as inaccessible.
 * NOTE: protected classes cannot be accessed because test class does not extend any classes.
 *
 * @param packageName name of the package we check accessibility from
 */
infix fun ClassId.isAccessibleFrom(packageName: String): Boolean {

    if (this.isLocal || this.isSynthetic) {
        return false
    }

    val outerClassId = outerClass?.id
    if (outerClassId != null && !outerClassId.isAccessibleFrom(packageName)) {
        return false
    }

    return if (this.isArray) {
        elementClassId!!.isAccessibleFrom(packageName)
    } else {
        isPublic || (this.packageName == packageName && (isPackagePrivate || isProtected))
    }
}