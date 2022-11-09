package org.utbot.framework.plugin.api

import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.TestFrameworkManager

abstract class LanguageTestFrameworkManager {

    open val testFrameworks: List<TestFramework> = emptyList()
    abstract fun managerByFramework(context: CgContext): TestFrameworkManager
    abstract val defaultTestFramework: TestFramework
}