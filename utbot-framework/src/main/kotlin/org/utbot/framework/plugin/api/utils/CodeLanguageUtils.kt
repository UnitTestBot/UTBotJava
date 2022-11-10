package org.utbot.framework.plugin.api.utils

import org.utbot.framework.plugin.api.ClassId

fun testClassNameGenerator(
    testClassCustomName: String?,
    testClassPackageName: String,
    classUnderTest: ClassId
): Pair<String, String> {
    val packagePrefix = if (testClassPackageName.isNotEmpty()) "$testClassPackageName." else ""
    val simpleName = testClassCustomName ?: "${classUnderTest.simpleName}Test"
    val name = "$packagePrefix$simpleName"
    return Pair(name, simpleName)
}
