package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgDocumentationComment
import org.utbot.framework.codegen.domain.models.CgFieldAccess
import org.utbot.framework.codegen.domain.models.CgGetLength
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgMultilineComment
import org.utbot.framework.codegen.domain.models.CgParameterDeclaration
import org.utbot.framework.codegen.domain.models.CgReferenceExpression
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgTestMethodType
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.convertDocToCg
import org.utbot.framework.codegen.tree.CgMethodConstructor
import org.utbot.framework.codegen.tree.buildTestMethod
import org.utbot.framework.plugin.api.*
import org.utbot.python.framework.api.python.*
import org.utbot.python.framework.api.python.util.pythonExceptionClassId
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.python.framework.codegen.model.tree.*

class PythonCgMethodConstructor(context: CgContext) : CgMethodConstructor(context) {
    private val maxDepth: Int = 5

    private fun CgVariable.deepcopy(): CgVariable {
        val classId = PythonClassId("copy.deepcopy")
        importIfNeeded(classId)
        return newVar(this.type) { CgPythonFunctionCall(classId, "copy.deepcopy", listOf(this)) }
    }

    override fun assertEquality(expected: CgValue, actual: CgVariable) {
        pythonDeepEquals(expected, actual)
    }

    override fun createTestMethod(testSet: CgMethodTestSet, execution: UtExecution): CgTestMethod =
        withTestMethodScope(execution) {
            val constructorState = (execution as PythonUtExecution).stateInit
            val diffIds = execution.diffIds
            (context.cgLanguageAssistant as PythonCgLanguageAssistant).clear()
            val testMethodName = nameGenerator.testMethodNameFor(testSet.executableUnderTest, execution.testMethodName)
            if (execution.testMethodName == null) {
                execution.testMethodName = testMethodName
            }
            // TODO: remove this line when SAT-1273 is completed
            execution.displayName = execution.displayName?.let { "${testSet.executableUnderTest.name}: $it" }
            pythonTestMethod(testMethodName, execution.displayName) {
                val statics = currentExecution!!.stateBefore.statics
                rememberInitialStaticFields(statics)

                // TODO: move such methods to another class and leave only 2 public methods: remember initial and final states
                val mainBody = {
                    substituteStaticFields(statics)
                    setupInstrumentation()
                    // build this instance
                    thisInstance = constructorState.thisInstance?.let {
                        variableConstructor.getOrCreateVariable(it)
                    }

                    val beforeThisInstance = execution.stateBefore.thisInstance
                    val afterThisInstance = execution.stateAfter.thisInstance
                    val assertThisObject = emptyList<Pair<CgVariable, UtModel>>().toMutableList()
                    if (beforeThisInstance is PythonTreeModel && afterThisInstance is PythonTreeModel) {
                        if (diffIds.contains(afterThisInstance.tree.id)) {
                            thisInstance = thisInstance?.let {
                                val newValue =
                                    if (it is CgPythonTree) {
                                        if (it.value is CgVariable) {
                                            it.value
                                        } else {
                                            newVar(it.type) {it.value}
                                        }
                                    } else {
                                        newVar(it.type) {it}
                                    }
                                assertThisObject.add(Pair(newValue, afterThisInstance))
                                newValue
                            }
                        }
                    }
                    if (thisInstance is CgPythonTree) {
                        context.currentBlock.addAll((thisInstance as CgPythonTree).arguments)
                    }

                    // build arguments
                    val stateAssertions = emptyMap<Int, Pair<CgVariable, UtModel>>().toMutableMap()
                    for ((index, param) in constructorState.parameters.withIndex()) {
                        val name = execution.arguments[index].name
                        var argument = variableConstructor.getOrCreateVariable(param, name)

                        val beforeValue = execution.stateBefore.parameters[index]
                        val afterValue = execution.stateAfter.parameters[index]
                        if (afterValue is PythonTreeModel && beforeValue is PythonTreeModel) {
                            if (diffIds.contains(afterValue.tree.id)) {
                                if (argument !is CgVariable) {
                                    argument = newVar(argument.type, name) {argument}
                                }
                                stateAssertions[index] = Pair(argument, afterValue)
                            }
                        }
                        if (execution.arguments[index].isNamed) {
                            argument = CgPythonNamedArgument(name, argument)
                        }

                        methodArguments += argument
                    }
                    methodArguments.forEach {
                        if (it is CgPythonTree) {
                            context.currentBlock.addAll(it.arguments)
                        }
                        if (it is CgPythonNamedArgument && it.value is CgPythonTree) {
                            context.currentBlock.addAll(it.value.arguments)
                        }
                    }

                    recordActualResult()
                    generateResultAssertions()

                    if (methodType == CgTestMethodType.PASSED_EXCEPTION) {
                        generateFieldStateAssertions(
                            stateAssertions,
                            assertThisObject,
                            execution.executableToCall ?: testSet.executableUnderTest
                        )
                    }
                }

                if (statics.isNotEmpty()) {
                    +tryBlock {
                        mainBody()
                    }.finally {
                        recoverStaticFields()
                    }
                } else {
                    mainBody()
                }
            }
        }

    override fun generateResultAssertions() {
        if (currentExecutableToCall is MethodId) {
            val currentExecution = currentExecution!!
            val executionResult = currentExecution.result
            if (executionResult is UtExecutionFailure) {
                val exceptionId = executionResult.rootCauseException.message?.let {PythonClassId(it)} ?: pythonExceptionClassId
                val executionBlock = {
                    with(currentExecutableToCall) {
                        when (this) {
                            is MethodId -> thisInstance[this](*methodArguments.toTypedArray()).intercepted()
                            else -> {}
                        }
                    }
                }
                when (methodType) {
                    CgTestMethodType.PASSED_EXCEPTION -> {
                        testFrameworkManager.expectException(exceptionId) {
                            executionBlock()
                        }
                        return
                    }
                    CgTestMethodType.FAILING -> {
                        val executable = currentExecutableToCall!! as PythonMethodId
                        val executableName = "${executable.moduleName}.${executable.name}"
                        val warningLine =
                            "This test fails because function [$executableName] produces [${exceptionId.prettyName}]"
                        +CgMultilineComment(warningLine)
                        emptyLineIfNeeded()
                        executionBlock()
                        return
                    }
                    else -> {}
                }
            }
        }
        super.generateResultAssertions()
    }

    private fun generateFieldStateAssertions(
        stateAssertions: MutableMap<Int, Pair<CgVariable, UtModel>>,
        assertThisObject: MutableList<Pair<CgVariable, UtModel>>,
        executableId: ExecutableId,
    ) {
        if (stateAssertions.isNotEmpty()) {
            emptyLineIfNeeded()
        }
        stateAssertions.forEach { (index, it) ->
            assertEquality(
                expected = it.second,
                actual = it.first,
                expectedVariableName = paramNames[executableId]?.get(index) + "_modified"
            )
        }
        if (assertThisObject.isNotEmpty()) {
            emptyLineIfNeeded()
        }
        assertThisObject.forEach {
            assertEquality(
                expected = it.second,
                actual = it.first,
                expectedVariableName = it.first.name + "_modified"
            )
        }
    }

    override fun shouldTestPassWithException(execution: UtExecution, exception: Throwable): Boolean {
        if (exception is TimeoutException || exception is InstrumentedProcessDeathException) return false
        return runtimeExceptionTestsBehaviour == RuntimeExceptionTestsBehaviour.PASS
    }

    private fun pythonTestMethod(
        methodName: String,
        displayName: String?,
        params: List<CgParameterDeclaration> = emptyList(),
        body: () -> Unit,
    ): CgTestMethod {
        displayName?.let {
            testFrameworkManager.addTestDescription(displayName)
        }

        val result = currentExecution!!.result
        if (result is UtTimeoutException) {
            testFrameworkManager.disableTestMethod(
                "Disabled due to the fact that the execution is longer then ${hangingTestsTimeout.timeoutMs} ms"
            )
        }

        val testMethod = buildTestMethod {
            name = methodName
            parameters = params
            statements = block(body)
            // Exceptions and annotations assignment must run after the statements block is build,
            // because we collect info about exceptions and required annotations while building the statements
            exceptions += collectedExceptions
            annotations += collectedMethodAnnotations
            methodType = this@PythonCgMethodConstructor.methodType

            val docComment = currentExecution?.summary?.map { convertDocToCg(it) }?.toMutableList() ?: mutableListOf()
            documentation = CgDocumentationComment(docComment)
        }
        testMethods += testMethod
        return testMethod
    }

    private fun pythonDeepEquals(expected: CgValue, actual: CgVariable) {
        require(expected is CgPythonTree) {
            "Expected value have to be CgPythonTree but `${expected::class}` found"
        }
        pythonDeepTreeEquals(expected.tree, expected, actual)
    }

    private fun pythonLenAssertConstructor(expected: CgVariable, actual: CgVariable): CgVariable {
        val expectedValue = newVar(pythonIntClassId, "expected_length") {
            CgGetLength(expected)
        }
        val actualValue = newVar(pythonIntClassId, "actual_length") {
            CgGetLength(actual)
        }
        emptyLineIfNeeded()
        testFrameworkManager.assertEquals(expectedValue, actualValue)
        return expectedValue
    }

    private fun assertIsInstance(expected: CgValue, actual: CgVariable) {
        when (testFrameworkManager) {
            is PytestManager ->
                (testFrameworkManager as PytestManager).assertIsinstance(listOf(expected.type as PythonClassId), actual)
            is UnittestManager ->
                (testFrameworkManager as UnittestManager).assertIsinstance(listOf(expected.type as PythonClassId), actual)
            else -> testFrameworkManager.assertEquals(expected, actual)
        }
    }

    private fun pythonAssertElementsByKey(
        expectedNode: PythonTree.PythonTreeNode,
        expected: CgVariable,
        actual: CgVariable,
        iterator: CgReferenceExpression,
        keyName: String = "index",
    ) {
        val elements = when (expectedNode) {
            is PythonTree.ListNode -> expectedNode.items.values
            is PythonTree.TupleNode -> expectedNode.items.values
            is PythonTree.DictNode -> expectedNode.items.values
            else -> throw UnsupportedOperationException()
        }
        if (elements.isNotEmpty()) {
            val elementsHaveSameStructure = PythonTree.allElementsHaveSameStructure(elements)
            val firstChild =
                elements.first()  // TODO: We can use only structure => we should use another element if the first is empty

            emptyLineIfNeeded()
            if (elementsHaveSameStructure) {
                val index = newVar(pythonNoneClassId, keyName) {
                    CgLiteral(pythonNoneClassId, "None")
                }
                forEachLoop {
                    innerBlock {
                        condition = index
                        iterable = iterator
                        val indexExpected = newVar(firstChild.type, "expected_element") {
                            CgPythonIndex(
                                pythonIntClassId,
                                expected,
                                index
                            )
                        }
                        val indexActual = newVar(firstChild.type, "actual_element") {
                            CgPythonIndex(
                                pythonIntClassId,
                                actual,
                                index
                            )
                        }
                        pythonDeepTreeEquals(firstChild, indexExpected, indexActual, useExpectedAsValue = true)
                        statements = currentBlock
                    }
                }
            } else {
                emptyLineIfNeeded()
                assertIsInstance(expected, actual)
            }
        }
    }

    private fun pythonAssertBuiltinsCollection(
        expectedNode: PythonTree.PythonTreeNode,
        expected: CgValue,
        actual: CgVariable,
        expectedName: String,
        elementName: String = "index",
    ) {
        val expectedCollection = newVar(expected.type, expectedName) { expected }

        val length = pythonLenAssertConstructor(expectedCollection, actual)

        val iterator = if (expectedNode is PythonTree.DictNode) expected else CgPythonRange(length)
        pythonAssertElementsByKey(expectedNode, expectedCollection, actual, iterator, elementName)
    }

    private fun pythonDeepTreeEquals(
        expectedNode: PythonTree.PythonTreeNode,
        expected: CgValue,
        actual: CgVariable,
        depth: Int = maxDepth,
        useExpectedAsValue: Boolean = false
    ) {
        if (expectedNode.comparable || depth == 0) {
            val expectedValue = if (useExpectedAsValue) {
                expected
            } else {
                variableConstructor.getOrCreateVariable(PythonTreeModel(expectedNode))
            }
            testFrameworkManager.assertEquals(
                expectedValue,
                actual,
            )
            return
        }
        when (expectedNode) {
            is PythonTree.PrimitiveNode -> {
                emptyLineIfNeeded()
                assertIsInstance(expected, actual)
            }

            is PythonTree.ListNode -> {
                pythonAssertBuiltinsCollection(
                    expectedNode,
                    expected,
                    actual,
                    "expected_list"
                )
            }

            is PythonTree.TupleNode -> {
                pythonAssertBuiltinsCollection(
                    expectedNode,
                    expected,
                    actual,
                    "expected_tuple"
                )
            }

            is PythonTree.SetNode -> {
                emptyLineIfNeeded()
                testFrameworkManager.assertEquals(
                    expected, actual
                )
            }

            is PythonTree.DictNode -> {
                pythonAssertBuiltinsCollection(
                    expectedNode,
                    expected,
                    actual,
                    "expected_dict",
                    "key"
                )
            }

            is PythonTree.ReduceNode -> {
                if (expectedNode.state.isNotEmpty()) {
                    expectedNode.state.forEach { (field, value) ->
                        val fieldActual = newVar(value.type, "actual_$field") {
                            CgFieldAccess(
                                actual, FieldId(
                                    value.type,
                                    field
                                )
                            )
                        }
                        val fieldExpected = newVar(value.type, "expected_$field") {
                            CgFieldAccess(
                                expected, FieldId(
                                    value.type,
                                    field
                                )
                            )
                        }
                        pythonDeepTreeEquals(value, fieldExpected, fieldActual, depth - 1)
                    }
                } else {
                    emptyLineIfNeeded()
                    assertIsInstance(expected, actual)
                }
            }

            else -> {}
        }
    }
}