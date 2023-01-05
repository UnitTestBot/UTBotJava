package org.utbot.framework.plugin.api.utils

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.nameWithEnclosingClassesAsContigousString

fun testClassNameGenerator(
    testClassCustomName: String?,
    testClassPackageName: String,
    classUnderTest: ClassId
): Pair<String, String> {
    val packagePrefix = if (testClassPackageName.isNotEmpty()) "$testClassPackageName." else ""
    val simpleName = testClassCustomName ?: "${classUnderTest.nameWithEnclosingClassesAsContigousString}Test"
    val name = "$packagePrefix$simpleName"
    return Pair(name, simpleName)
}
