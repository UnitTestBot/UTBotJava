package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.context.TestClassContext
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.*
import org.utbot.framework.codegen.domain.models.AnnotationTarget.*
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
        val withExpression = CgPythonFunctionCall(
            pythonNoneClassId,
            "pytest.raises",
            listOf(CgLiteral(exception, exception.prettyName))
        )
        +CgPythonWith(withExpression, null, context.block(block))
    }

    override val isExpectedExceptionExecutionBreaking: Boolean = true

    override fun createDataProviderAnnotations(dataProviderMethodName: String): MutableList<CgAnnotation> {
        TODO("Not yet implemented")
    }

    override fun createArgList(length: Int): CgVariable {
        TODO("Not yet implemented")
    }

    override fun addParameterizedTestAnnotations(dataProviderMethodName: String?) { }

    override fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun addTestDescription(description: String) = Unit

    override fun disableTestMethod(reason: String) {
        require(testFramework is Pytest) { "According to settings, Pytest was expected, but got: $testFramework" }

        context.importIfNeeded(testFramework.skipDecoratorClassId)
        val reasonArgument = CgNamedAnnotationArgument(
            name = "reason",
            value = CgPythonRepr(pythonStrClassId, "'${reason.replace("\"", "'")}'"),
        )
        statementConstructor.addAnnotation(
            classId = testFramework.skipDecoratorClassId,
            namedArguments = listOf(reasonArgument),
            target = Method
        )
    }

    override val dataProviderMethodsHolder: TestClassContext get() = TODO()
    override fun addAnnotationForNestedClasses() {
        error("Nested classes annotation does not exists in PyTest")
    }

    override fun addAnnotationForSpringRunner() {
        error("Spring runner annotation does not exists in PyTest")
    }

    override fun assertEquals(expected: CgValue, actual: CgValue) {
        +CgPythonAssertEquals(
            CgEqualTo(actual, expected)
        )
    }

    fun assertIsInstance(types: List<ClassId>, actual: CgVariable) {
        +CgPythonAssertEquals(
            CgPythonFunctionCall(
                pythonBoolClassId,
                "isinstance",
                listOf(
                    actual,
                    if (types.size == 1)
                        CgLiteral(pythonAnyClassId, types[0].name)
                    else
                        CgPythonTuple(types.map { CgLiteral(pythonAnyClassId, it.name) })
                ),
            ),
        )
    }
}

internal class UnittestManager(context: CgContext) : TestFrameworkManager(context) {
    override val isExpectedExceptionExecutionBreaking: Boolean = true

    override val dataProviderMethodsHolder: TestClassContext
        get() = TODO()
    override fun addAnnotationForNestedClasses() {
        error("Nested classes annotation does not exists in Unittest")
    }

    override fun addAnnotationForSpringRunner() {
        error("Spring runner annotation does not exists in Unittest")
    }

    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Unittest) { "According to settings, Unittest was expected, but got: $testFramework" }
        require(exception is PythonClassId) { "Exceptions must be PythonClassId" }
        val withExpression = CgPythonFunctionCall(
            pythonNoneClassId,
            "self.assertRaises",
            listOf(CgLiteral(exception, exception.prettyName))
        )
        +CgPythonWith(withExpression, null, context.block(block))
    }

    override fun createDataProviderAnnotations(dataProviderMethodName: String): MutableList<CgAnnotation> {
        TODO("Not yet implemented")
    }

    override fun createArgList(length: Int): CgVariable {
        TODO("Not yet implemented")
    }

    override fun addParameterizedTestAnnotations(dataProviderMethodName: String?) { }

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
            classId = testFramework.skipDecoratorClassId,
            namedArguments = listOf(reasonArgument),
            target = Method,

            )
    }

    fun assertIsInstance(types: List<ClassId>, actual: CgVariable) {
        +assertions[assertTrue](
            CgPythonFunctionCall(
                pythonBoolClassId,
                "isinstance",
                listOf(
                    actual,
                    if (types.size == 1)
                        CgLiteral(pythonAnyClassId, types[0].name)
                    else
                        CgPythonTuple(types.map { CgLiteral(pythonAnyClassId, it.name) })
                ),
            ),
        )
    }
}
