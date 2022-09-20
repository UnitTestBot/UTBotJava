package org.utbot.engine

import org.utbot.framework.plugin.api.ClassId

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
    "kotlin.internal"
)

private fun isSystemPackage(packageName: String): Boolean = systemPackages.any { packageName.startsWith(it) }