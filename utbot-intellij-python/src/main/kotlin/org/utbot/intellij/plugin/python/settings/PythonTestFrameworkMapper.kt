package org.utbot.intellij.plugin.python.settings

import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.intellij.plugin.settings.TestFrameworkMapper
import org.utbot.python.framework.codegen.PythonTestFrameworkManager
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest

object PythonTestFrameworkMapper: TestFrameworkMapper {
    override fun toString(value: TestFramework): String = value.id

    override fun fromString(value: String): TestFramework = when (value) {
        Unittest.id -> Unittest
        Pytest.id -> Pytest
        else -> error("Unknown TestFramework $value")
    }

    override fun handleUnknown(testFramework: TestFramework): TestFramework {
        if (allItems.contains(testFramework)) {
            return testFramework
        }
        return try {
            fromString(testFramework.id)
        } catch (ex: IllegalStateException) {
            defaultItem
        }
    }

    val defaultItem: TestFramework get() = PythonTestFrameworkManager().defaultTestFramework
    val allItems: List<TestFramework> get() = PythonTestFrameworkManager().testFrameworks
}