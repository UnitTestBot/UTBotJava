package org.utbot.tests

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.intellij.plugin.ui.utils.ITestSourceRoot
import org.utbot.intellij.plugin.ui.utils.SRC_MAIN
import org.utbot.intellij.plugin.ui.utils.getCommonPrefix
import org.utbot.intellij.plugin.ui.utils.getSortedTestRoots

internal class RootUtilsTest {
    internal class MockTestSourceRoot(override val dirPath: String) : ITestSourceRoot {
        override val dir = null
        override val dirName = dirPath.substring(dirPath.lastIndexOf("/") + 1)
        override val expectedLanguage = if (dirName == "java") CodegenLanguage.JAVA else CodegenLanguage.KOTLIN
        override fun toString()= dirPath
    }

    @Test
    fun testCommonPrefix() {
        val commonPrefix = listOf(
            "/UTBotJavaTest/utbot-framework/src/main/java",
            "/UTBotJavaTest/utbot-framework/src/main/kotlin",
            "/UTBotJavaTest/utbot-framework/src/main/resources"
        ).getCommonPrefix()
        Assertions.assertEquals("/UTBotJavaTest/utbot-framework/src/main/", commonPrefix)
        Assertions.assertTrue(commonPrefix.endsWith(SRC_MAIN))
    }

    @Test
    fun testRootSorting() {
        val allTestRoots = mutableListOf(
            MockTestSourceRoot("/UTBotJavaTest/utbot-analytics/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-analytics/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-analytics-torch/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-cli/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-framework/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-framework-api/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-framework-api/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-framework-test/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-framework-test/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-java-fuzzing/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-java-fuzzing/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-gradle/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-instrumentation/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-instrumentation/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-instrumentation-tests/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-instrumentation-tests/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-intellij/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-maven/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-sample/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-summary/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-summary-tests/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-core/src/test/java"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-core/src/test/kotlin"),
            MockTestSourceRoot("/UTBotJavaTest/utbot-rd/src/test/kotlin"),
        )
        val moduleSourcePaths = listOf(
            "/UTBotJavaTest/utbot-framework/src/main/java",
            "/UTBotJavaTest/utbot-framework/src/main/kotlin",
            "/UTBotJavaTest/utbot-framework/src/main/resources",
        )
        val sortedTestRoots = getSortedTestRoots(
            allTestRoots,
            listOf("/UTBotJavaTest/utbot-core/src/test/java"),
            moduleSourcePaths,
            CodegenLanguage.JAVA
        )
        Assertions.assertEquals("/UTBotJavaTest/utbot-framework/src/test/java", sortedTestRoots.first().toString())
        Assertions.assertEquals("/UTBotJavaTest/utbot-core/src/test/java", sortedTestRoots[1].toString())
    }
}