package org.utbot.framework.plugin.api

import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.TestNg
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.services.framework.Junit4Manager
import org.utbot.framework.codegen.services.framework.Junit5Manager
import org.utbot.framework.codegen.services.framework.TestNgManager
import org.utbot.framework.codegen.services.language.LanguageTestFrameworkManager

class JVMTestFrameworkManager : LanguageTestFrameworkManager() {

    override fun managerByFramework(context: CgContext) = when (context.testFramework) {
        is Junit4 -> Junit4Manager(context)
        is Junit5 -> Junit5Manager(context)
        is TestNg -> TestNgManager(context)
        else -> throw UnsupportedOperationException("Incorrect TestFramework ${context.testFramework}")
    }

    override val defaultTestFramework = Junit5

    override val testFrameworks = listOf(Junit4, Junit5, TestNg)

}