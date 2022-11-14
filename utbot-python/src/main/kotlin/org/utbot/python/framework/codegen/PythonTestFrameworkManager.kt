package org.utbot.python.framework.codegen

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.services.language.LanguageTestFrameworkManager
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest
import org.utbot.python.framework.codegen.model.constructor.tree.PytestManager
import org.utbot.python.framework.codegen.model.constructor.tree.UnittestManager

class PythonTestFrameworkManager : LanguageTestFrameworkManager() {

    override fun managerByFramework(context: CgContext) = when (context.testFramework) {
        is Unittest -> UnittestManager(context)
        is Pytest -> PytestManager(context)
        else -> throw UnsupportedOperationException("Incorrect TestFramework ${context.testFramework}")
    }

    override val defaultTestFramework = Unittest

    override val testFrameworks = listOf(Unittest, Pytest)

}