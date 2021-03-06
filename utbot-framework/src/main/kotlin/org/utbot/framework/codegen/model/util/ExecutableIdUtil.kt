package org.utbot.framework.codegen.model.util

import org.utbot.framework.plugin.api.ExecutableId

/**
 * For now we will count executable accessible if it is whether public
 * or package-private inside target package [packageName].
 *
 * @param packageName name of the package we check accessibility from
 */
fun ExecutableId.isAccessibleFrom(packageName: String): Boolean {
    val isAccessibleFromPackageByModifiers = isPublic || (classId.packageName == packageName && (isPackagePrivate || isProtected))

    return classId.isAccessibleFrom(packageName) && isAccessibleFromPackageByModifiers
}
