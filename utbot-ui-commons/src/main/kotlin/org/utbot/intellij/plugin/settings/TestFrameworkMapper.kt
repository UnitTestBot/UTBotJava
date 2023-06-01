package org.utbot.intellij.plugin.settings

import org.utbot.framework.codegen.domain.TestFramework

interface TestFrameworkMapper {
    fun toString(value: TestFramework): String
    fun fromString(value: String): TestFramework
    fun handleUnknown(testFramework: TestFramework): TestFramework
}