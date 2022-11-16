package org.utbot.framework.codegen.services.language

import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.services.framework.TestFrameworkManager

abstract class LanguageTestFrameworkManager {

    open val testFrameworks: List<TestFramework> = emptyList()
    abstract fun managerByFramework(context: CgContext): TestFrameworkManager
    abstract val defaultTestFramework: TestFramework
}