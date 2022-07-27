package org.utbot.framework.codegen.model.util

import kotlinx.coroutines.runBlocking
import org.utbot.framework.plugin.api.isClassAccessibleFrom
import org.utbot.framework.plugin.api.packageName
import org.utbot.jcdb.api.MethodId
import org.utbot.jcdb.api.isPackagePrivate
import org.utbot.jcdb.api.isProtected
import org.utbot.jcdb.api.isPublic

/**
 * For now we will count executable accessible if it is whether public
 * or package-private inside target package [packageName].
 *
 * @param packageName name of the package we check accessibility from
 */
fun MethodId.isAccessibleFrom(packageName: String): Boolean = runBlocking {
    val isAccessibleFromPackageByModifiers =
        isPublic() || (classId.packageName == packageName && (isPackagePrivate() || isProtected()))

    classId.isClassAccessibleFrom(packageName) && isAccessibleFromPackageByModifiers
}
