package org.utbot.engine

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.jClass

/**
 * Mock strategies.
 *
 * Current implementations:
 * - Do not mock;
 * - Package based approach. Mock everything outside class under test package except system packages such java.util;
 *
 * Note: Not deterministic things such Random, Date/Time and similar are _always_ mocked.
 */
enum class MockStrategy {
    NO_MOCKS {
        override fun eligibleToMock(classToMock: ClassId, classUnderTest: ClassId): Boolean = false
    },

    OTHER_PACKAGES {
        override fun eligibleToMock(classToMock: ClassId, classUnderTest: ClassId): Boolean =
            classToMock != classUnderTest && classToMock.packageName.let {
                it != classUnderTest.packageName && !isSystemPackage(it)
            }
    },

    THIRD_PARTY_LIBRARY_CLASSES {
        // see https://stackoverflow.com/a/1983870
        override fun eligibleToMock(classToMock: ClassId, classUnderTest: ClassId): Boolean {
            val codeSource = runCatching {
                // In case declaring type is not presented in the classpath
                classToMock.jClass.protectionDomain.codeSource ?: return false
            }.onFailure {
                // An exception here means the class is not in the classpath, so it should be from a third-party library
                return true
            }.getOrThrow()

            val location = codeSource.location

            return location.path.endsWith("jar") && !location.path.endsWith("rt.jar") && !location.path.endsWith("jce.jar")
        }
    },

    OTHER_CLASSES {
        override fun eligibleToMock(classToMock: ClassId, classUnderTest: ClassId): Boolean =
            classToMock != classUnderTest && !isSystemPackage(classToMock.packageName)
    };

    /**
     * Checks if instance of class to mock should be mocked. [OTHER_PACKAGES] uses class under test to decide.
     */
    abstract fun eligibleToMock(classToMock: ClassId, classUnderTest: ClassId): Boolean
}

private val systemPackages = setOf(
    "java.lang",
    "java.util",
    "java.io",
    "java.math",
    "java.net",
    "java.security",
    "java.text",
    "sun.reflect", // we cannot mock Reflection since mockers are using it during the execution
    "java.awt",
    "sun.misc",
    "jdk.internal",
    "kotlin.jvm.internal",
    "kotlin.internal",
    "javax"
)

private fun isSystemPackage(packageName: String): Boolean = systemPackages.any { packageName.startsWith(it) }