package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.TestClassContext
import org.utbot.framework.codegen.domain.models.AnnotationTarget.Method
import org.utbot.framework.codegen.domain.models.CgEqualTo
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgNamedAnnotationArgument
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.framework.TestFrameworkManager
import org.utbot.framework.plugin.api.ClassId
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.api.python.util.pythonBoolClassId
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest
import org.utbot.python.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.python.framework.codegen.model.tree.CgPythonAssertEquals
import org.utbot.python.framework.codegen.model.tree.CgPythonFunctionCall
import org.utbot.python.framework.codegen.model.tree.CgPythonRepr
import org.utbot.python.framework.codegen.model.tree.CgPythonTuple
import org.utbot.python.framework.codegen.model.tree.CgPythonWith

internal class PytestManager(context: CgContext) : TestFrameworkManager(context) {
    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Pytest) { "According to settings, Pytest was expected, but got: $testFramework" }
        require(exception is PythonClassId) { "Exceptions must be PythonClassId" }
        context.importIfNeeded(PythonClassId("pytest.raises"))
        importIfNeeded(exception)
        val withExpression = CgPythonFunctionCall(
            pythonNoneClassId,
            "pytest.raises",
            listOf(CgLiteral(exception, exception.prettyName))
        )
        +CgPythonWith(withExpression, null, context.block(block))
    }

    override val isExpectedExceptionExecutionBreaking: Boolean = true

    override fun addDataProviderAnnotations(dataProviderMethodName: String) {
        error("Parametrized tests are not supported for Python")
    }

    override fun createArgList(length: Int): CgVariable {
        error("Parametrized tests are not supported for Python")
    }

    override fun addParameterizedTestAnnotations(dataProviderMethodName: String?) {
        error("Parametrized tests are not supported for Python")
    }

    override fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun addTestDescription(description: String) = Unit

    override fun disableTestMethod(reason: String) {
        require(testFramework is Pytest) { "According to settings, Pytest was expected, but got: $testFramework" }

        context.importIfNeeded(Pytest.skipDecoratorClassId)
        val reasonArgument = CgNamedAnnotationArgument(
            name = "reason",
            value = CgPythonRepr(pythonStrClassId, "'${reason.replace("\"", "'")}'"),
        )
        statementConstructor.addAnnotation(
            classId = Pytest.skipDecoratorClassId,
            namedArguments = listOf(reasonArgument),
            target = Method
        )
    }

    override val dataProviderMethodsHolder: TestClassContext get() =
        error("Parametrized tests are not supported for Python")

    override fun addAnnotationForNestedClasses() {
        error("Nested classes annotation does not exist in PyTest")
    }

    override fun assertEquals(expected: CgValue, actual: CgValue) {
        +CgPythonAssertEquals(
            CgEqualTo(actual, expected)
        )
    }

    override fun assertSame(expected: CgValue, actual: CgValue) {
        error("assertSame does not exist in PyTest")
    }

    fun assertIsinstance(types: List<PythonClassId>, actual: CgVariable) {
        +CgPythonAssertEquals(
            CgPythonFunctionCall(
                pythonBoolClassId,
                "isinstance",
                listOf(
                    actual,
                    if (types.size == 1)
                        CgLiteral(pythonAnyClassId, types[0].prettyName)
                    else
                        CgPythonTuple(types.map { CgLiteral(pythonAnyClassId, it.prettyName) })
                ),
            ),
        )
    }
}

internal class UnittestManager(context: CgContext) : TestFrameworkManager(context) {
    override val isExpectedExceptionExecutionBreaking: Boolean = true

    override val dataProviderMethodsHolder: TestClassContext
        get() = error("Parametrized tests are not supported in Unittest")

    override fun addAnnotationForNestedClasses() {
        error("Nested classes annotation does not exist in Unittest")
    }

    override fun assertSame(expected: CgValue, actual: CgValue) {
        error("assertSame does not exist in Unittest")
    }

    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Unittest) { "According to settings, Unittest was expected, but got: $testFramework" }
        require(exception is PythonClassId) { "Exceptions must be PythonClassId" }
        importIfNeeded(exception)
        val withExpression = CgPythonFunctionCall(
            pythonNoneClassId,
            "self.assertRaises",
            listOf(CgLiteral(exception, exception.prettyName))
        )
        +CgPythonWith(withExpression, null, context.block(block))
    }

    override fun addDataProviderAnnotations(dataProviderMethodName: String) {
        error("Parametrized tests are not supported for Python")
    }

    override fun createArgList(length: Int): CgVariable {
        error("Parametrized tests are not supported for Python")
    }

    override fun addParameterizedTestAnnotations(dataProviderMethodName: String?) {
        error("Parametrized tests are not supported for Python")
    }

    override fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun addTestDescription(description: String) = Unit

    override fun disableTestMethod(reason: String) {
        require(testFramework is Unittest) { "According to settings, Unittest was expected, but got: $testFramework" }

        val reasonArgument = CgNamedAnnotationArgument(
            name = "reason",
            value = CgPythonRepr(pythonStrClassId, "'${reason.replace("\"", "'")}'"),
        )
        statementConstructor.addAnnotation(
            classId = Unittest.skipDecoratorClassId,
            namedArguments = listOf(reasonArgument),
            target = Method,
        )
    }

    fun assertIsinstance(types: List<PythonClassId>, actual: CgVariable) {
        +assertions[assertTrue](
            CgPythonFunctionCall(
                pythonBoolClassId,
                "isinstance",
                listOf(
                    actual,
                    if (types.size == 1)
                        CgLiteral(pythonAnyClassId, types[0].prettyName)
                    else
                        CgPythonTuple(types.map { CgLiteral(pythonAnyClassId, it.prettyName) })
                ),
            ),
        )
    }
}
