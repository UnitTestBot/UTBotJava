package org.utbot.intellij.plugin.language.python.settings

import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.intellij.plugin.settings.TestFrameworkMapper
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest

object PythonTestFrameworkMapper: TestFrameworkMapper {
    override fun toString(value: TestFramework): String = value.id

    override fun fromString(value: String): TestFramework = when (value) {
        Unittest.id -> Unittest
        Pytest.id -> Pytest
        else -> Unittest
    }
}