package framework.codegen.model.constructor.tree

import framework.codegen.Mocha
import framework.codegen.jsAssertEquals
import framework.codegen.jsAssertThrows
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.TestClassContext
import org.utbot.framework.codegen.domain.models.CgAnnotation
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.framework.TestFrameworkManager
import org.utbot.framework.plugin.api.ClassId

class MochaManager(context: CgContext) : TestFrameworkManager(context) {
    override val isExpectedExceptionExecutionBreaking: Boolean = true

    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Mocha) { "According to settings, Mocha.js was expected, but got: $testFramework" }
        val lambda = statementConstructor.lambda(exception) { block() }
        +assertions[jsAssertThrows](lambda, "Error", exception.name)
    }

    override fun addDataProviderAnnotations(dataProviderMethodName: String) {
        error("Parametrized tests are not supported for JavaScript")
    }

    override fun createArgList(length: Int): CgVariable {
        error("Parametrized tests are not supported for JavaScript")
    }

    override fun addParameterizedTestAnnotations(dataProviderMethodName: String?) {
        error("Parametrized tests are not supported for JavaScript")
    }

    override fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int) {
        error("Parametrized tests are not supported for JavaScript")
    }

    override fun addTestDescription(description: String) {
        TODO("Not yet implemented")
    }

    override val dataProviderMethodsHolder: TestClassContext
        get() = error("Parametrized tests are not supported for JavaScript")

    override fun addAnnotationForNestedClasses() {
        error("Nested classes annotation does not exist in Mocha")
    }

    override fun assertEquals(expected: CgValue, actual: CgValue) {
        +assertions[jsAssertEquals](expected, actual)
    }

    override fun assertSame(expected: CgValue, actual: CgValue) {
        error("assertSame does not exist in Mocha")
    }

    override fun disableTestMethod(reason: String) {

    }

}
