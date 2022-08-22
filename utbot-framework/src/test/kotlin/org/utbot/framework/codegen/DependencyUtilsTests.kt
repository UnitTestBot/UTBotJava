package org.utbot.framework.codegen

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.utbot.common.PathUtil.getUrlsFromClassLoader
import org.utbot.framework.codegen.model.util.checkFrameworkDependencies
import java.io.File

@Disabled
class DependencyUtilsTests {
        @ParameterizedTest
        @MethodSource("provideDependencyPaths")
        fun testDependencyChecker(path: String?, isValid: Boolean, reason: String) {
            try {
                checkFrameworkDependencies(path)
                assertTrue(isValid)
            } catch (ex: IllegalStateException) {
                val message = ex.message
                assertTrue(message != null && message.contains(reason), message)
            }
        }

        companion object {
            private val separator = File.pathSeparatorChar
            private val jarUrls = getUrlsFromClassLoader(Thread.currentThread().contextClassLoader)

            private val mockito = jarUrls.firstOrNull { it.path.contains("mockito-core") }?.path ?: ""
            private val junit4 = jarUrls.firstOrNull { it.path.contains("junit-4") }?.path ?: ""
            private val junit5 = jarUrls.firstOrNull { it.path.contains("junit-jupiter-api") }?.path ?: ""
            private val testNg = jarUrls.firstOrNull { it.path.contains("testng") }?.path ?: ""

            @JvmStatic
            fun provideDependencyPaths(): ArrayList<Arguments> {
                val argList = ArrayList<Arguments>()

                argList.add(Arguments.arguments("$junit4$separator$mockito$separator", true, ""))
                argList.add(Arguments.arguments("$junit5$separator$mockito$separator", true, ""))
                argList.add(Arguments.arguments("$testNg$separator$mockito$separator", true, ""))
                argList.add(Arguments.arguments("", false, "Dependency paths is empty"))
                argList.add(Arguments.arguments(null, false, "Dependency paths is empty"))
                argList.add(Arguments.arguments("$junit4$separator$junit5$separator$testNg$separator", false, "Mock frameworks are not found"))
                argList.add(Arguments.arguments("$mockito$separator", false, "Test frameworks are not found"))

                return argList
            }
        }
}