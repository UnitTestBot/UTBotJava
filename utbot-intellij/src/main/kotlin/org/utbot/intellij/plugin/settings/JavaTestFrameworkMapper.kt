package org.utbot.intellij.plugin.settings

import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.TestNg

object JavaTestFrameworkMapper: TestFrameworkMapper {
    override fun toString(value: TestFramework): String = value.id

    override fun fromString(value: String): TestFramework = when (value) {
        Junit4.id -> Junit4
        Junit5.id -> Junit5
        TestNg.id -> TestNg
        else -> TestFramework.defaultItem
    }
}