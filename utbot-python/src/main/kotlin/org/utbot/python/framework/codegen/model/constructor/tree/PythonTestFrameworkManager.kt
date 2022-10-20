package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.constructor.TestClassContext
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.TestFrameworkManager
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.api.python.util.pythonBoolClassId
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest
import org.utbot.python.framework.codegen.model.tree.CgPythonAssertEquals
import org.utbot.python.framework.codegen.model.tree.CgPythonFunctionCall
import org.utbot.python.framework.codegen.model.tree.CgPythonTuple

internal class PytestManager(context: CgContext) : TestFrameworkManager(context) {
    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Pytest) { "According to settings, Pytest was expected, but got: $testFramework" }
        block()
    }

    override val isExpectedExceptionExecutionBreaking: Boolean = true

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

    override fun addTestDescription(description: String) = Unit

    override fun disableTestMethod(reason: String) {
    }

    override val dataProviderMethodsHolder: TestClassContext get() = TODO()
    override val annotationForNestedClasses: CgAnnotation
        get() = TODO("Not yet implemented")
    override val annotationForOuterClasses: CgAnnotation
        get() = TODO("Not yet implemented")

    override fun assertEquals(expected: CgValue, actual: CgValue) {
        +CgPythonAssertEquals(
            CgEqualTo(actual, expected)
        )
    }

    override fun assertIsinstance(types: List<ClassId>, actual: CgVariable) {
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
    override val annotationForNestedClasses: CgAnnotation
        get() = TODO("Not yet implemented")
    override val annotationForOuterClasses: CgAnnotation
        get() = TODO("Not yet implemented")

    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Unittest) { "According to settings, Unittest was expected, but got: $testFramework" }
        block()
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

    override fun addTestDescription(description: String) = Unit

    override fun disableTestMethod(reason: String) {
        require(testFramework is Unittest) { "According to settings, Unittest was expected, but got: $testFramework" }

        collectedMethodAnnotations += CgMultipleArgsAnnotation(
            skipAnnotationClassId,
            mutableListOf(
                CgNamedAnnotationArgument(
                    name = "value",
                    value = reason.resolve()
                )
            )
        )
    }

    private val skipAnnotationClassId = BuiltinClassId(
        name = "skip",
        canonicalName = "unittest.skip",
        simpleName = "skip"
    )

    override fun assertIsinstance(types: List<ClassId>, actual: CgVariable) {
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
