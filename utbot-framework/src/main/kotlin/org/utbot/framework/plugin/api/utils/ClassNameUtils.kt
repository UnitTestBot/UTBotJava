package org.utbot.framework.plugin.api.utils

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.nameWithEnclosingClassesAsContigousString

object ClassNameUtils {
    fun generateTestClassName(
        testClassCustomName: String?,
        testClassPackageName: String,
        classUnderTest: ClassId,
    ): Pair<String, String> {
        val packagePrefix = if (testClassPackageName.isNotEmpty()) "$testClassPackageName." else ""
        val simpleName = testClassCustomName ?: generateTestClassShortName(classUnderTest)
        val name = "$packagePrefix$simpleName"
        return Pair(name, simpleName)
    }

    fun generateTestClassShortName(classUnderTest: ClassId): String =
        "${classUnderTest.nameWithEnclosingClassesAsContigousString}Test"
}


