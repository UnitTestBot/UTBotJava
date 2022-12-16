package org.utbot.language.ts.framework.codegen.model.constructor.tree

import org.utbot.language.ts.framework.codegen.Mocha
import org.utbot.language.ts.framework.codegen.tsAssertEquals
import org.utbot.language.ts.framework.codegen.tsAssertThrows
import org.utbot.framework.codegen.model.constructor.TestClassContext
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.TestFrameworkManager
import org.utbot.framework.codegen.model.tree.CgAnnotation
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.plugin.api.ClassId

class MochaManager(context: CgContext) : TestFrameworkManager(context) {
    override val isExpectedExceptionExecutionBreaking: Boolean = true

    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Mocha) { "According to settings, Mocha.js was expected, but got: $testFramework" }
        val lambda = statementConstructor.lambda(exception) { block() }
        +assertions[tsAssertThrows](lambda, "Error", exception.name)
    }

    override fun createDataProviderAnnotations(dataProviderMethodName: String): MutableList<CgAnnotation> {
        TODO("Not yet implemented")
    }

    override fun createArgList(length: Int): CgVariable {
        TODO("Not yet implemented")
    }

    override fun collectParameterizedTestAnnotations(dataProviderMethodName: String?): Set<CgAnnotation> {
        TODO("Not yet implemented")
    }

    override fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun addTestDescription(description: String) {
        TODO("Not yet implemented")
    }

    override val dataProviderMethodsHolder: TestClassContext
        get() = TODO("Not yet implemented")
    override val annotationForNestedClasses: CgAnnotation
        get() = TODO("Not yet implemented")
    override val annotationForOuterClasses: CgAnnotation
        get() = TODO("Not yet implemented")

    override fun assertEquals(expected: CgValue, actual: CgValue) {
        +assertions[tsAssertEquals](expected, actual)
    }

    override fun disableTestMethod(reason: String) {

    }

}
