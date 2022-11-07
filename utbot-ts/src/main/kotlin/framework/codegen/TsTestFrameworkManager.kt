package framework.codegen

import framework.codegen.model.constructor.tree.MochaManager
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.plugin.api.LanguageTestFrameworkManager

class TsTestFrameworkManager: LanguageTestFrameworkManager() {

    override fun managerByFramework(context: CgContext) = when (context.testFramework) {
        is Mocha -> MochaManager(context)
        else -> throw UnsupportedOperationException("Incorrect TestFramework ${context.testFramework}")
    }

    override val defaultTestFramework = Mocha

    override val testFrameworks = listOf(Mocha)
}