package framework.codegen

import framework.codegen.model.constructor.tree.MochaManager
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.services.language.LanguageTestFrameworkManager

class JsTestFrameworkManager: LanguageTestFrameworkManager() {

    override fun managerByFramework(context: CgContext) = when (context.testFramework) {
        is Mocha -> MochaManager(context)
        else -> throw UnsupportedOperationException("Incorrect TestFramework ${context.testFramework}")
    }

    override val defaultTestFramework = Mocha

    override val testFrameworks = listOf(Mocha)
}