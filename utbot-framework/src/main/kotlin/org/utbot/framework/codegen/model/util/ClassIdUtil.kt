package org.utbot.framework.codegen.model.util

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
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
    if (this.isLocal || this.isAnonymous || this.isSynthetic) {
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

/**
 * Returns field of [this], such that [methodId] is a getter for it (or null if methodId doesn't represent a getter)
 */
internal fun ClassId.fieldThatIsGotWith(methodId: MethodId): FieldId? =
    allDeclaredFieldIds.singleOrNull { !it.isStatic && it.getter == methodId }

/**
 * Returns field of [this], such that [methodId] is a setter for it (or null if methodId doesn't represent a setter)
 */
internal fun ClassId.fieldThatIsSetWith(methodId: MethodId): FieldId? =
    allDeclaredFieldIds.singleOrNull { !it.isStatic && it.setter == methodId }