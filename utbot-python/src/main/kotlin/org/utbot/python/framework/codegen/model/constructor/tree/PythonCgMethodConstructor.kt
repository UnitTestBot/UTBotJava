package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgFieldAccess
import org.utbot.framework.codegen.domain.models.CgGetLength
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgReferenceExpression
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.tree.CgMethodConstructor
import org.utbot.framework.fields.StateModificationInfo
import org.utbot.framework.plugin.api.*
import org.utbot.python.framework.api.python.*
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.framework.codegen.model.tree.*

class PythonCgMethodConstructor(context: CgContext) : CgMethodConstructor(context) {
    val maxDepth: Int = 5

    override fun assertEquality(expected: CgValue, actual: CgVariable) {
        pythonDeepEquals(expected, actual)
    }

    private fun generatePythonTestComments(execution: UtExecution) {
        when (execution.result) {
            is UtExplicitlyThrownException ->
                (execution.result as UtExplicitlyThrownException).exception.message?.let {
                    emptyLineIfNeeded()
                    comment("raises $it")
                    println("pass")
                }

            else -> {
                // nothing
            }
        }
    }

    override fun createTestMethod(executableId: ExecutableId, execution: UtExecution): CgTestMethod =
        withTestMethodScope(execution) {
            val testMethodName = nameGenerator.testMethodNameFor(executableId, execution.testMethodName)
            // TODO: remove this line when SAT-1273 is completed
            execution.displayName = execution.displayName?.let { "${executableId.name}: $it" }
            testMethod(testMethodName, execution.displayName) {
                val statics = currentExecution!!.stateBefore.statics
                rememberInitialStaticFields(statics)
//                 (context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects.clear()

                val modificationInfo = StateModificationInfo()
                val fieldStateManager = context.cgLanguageAssistant.getCgFieldStateManager(context)
                // TODO: move such methods to another class and leave only 2 public methods: remember initial and final states
                val mainBody = {
                    substituteStaticFields(statics)
                    setupInstrumentation()
                    // build this instance
                    thisInstance = execution.stateBefore.thisInstance?.let {
                        variableConstructor.getOrCreateVariable(it)
                    }
                    if (thisInstance is CgPythonTree) {
                        context.currentBlock.addAll((thisInstance as CgPythonTree).arguments)
                    }
                    // build arguments
                    for ((index, param) in execution.stateBefore.parameters.withIndex()) {
                        val name = paramNames[executableId]?.get(index)
                        methodArguments += variableConstructor.getOrCreateVariable(param, name)
                    }
                    methodArguments.forEach {
                        if (it is CgPythonTree) {
                            context.currentBlock.addAll(it.arguments)
                        }
                    }
                    fieldStateManager.rememberInitialEnvironmentState(modificationInfo)
                    recordActualResult()
                    generateResultAssertions()
                    fieldStateManager.rememberFinalEnvironmentState(modificationInfo)
                    generateFieldStateAssertions()
                    if (executableId is PythonMethodId)
                        generatePythonTestComments(execution)
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
    private fun pythonBuildObject(objectNode: PythonTree.PythonTreeNode): CgValue {
        return when (objectNode) {
            is PythonTree.PrimitiveNode -> {
                CgLiteral(objectNode.type, objectNode.repr)
            }

            is PythonTree.ListNode -> {
                CgPythonList(
                    objectNode.items.values.map { pythonBuildObject(it) }
                )
            }

            is PythonTree.TupleNode -> {
                CgPythonTuple(
                    objectNode.items.values.map { pythonBuildObject(it) }
                )
            }

            is PythonTree.SetNode -> {
                CgPythonSet(
                    objectNode.items.map { pythonBuildObject(it) }.toSet()
                )
            }

            is PythonTree.DictNode -> {
                CgPythonDict(
                    objectNode.items.map { (key, value) ->
                        pythonBuildObject(key) to pythonBuildObject(value)
                    }.toMap()
                )
            }

            is PythonTree.ReduceNode -> {
                val id = objectNode.id
                if ((context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects.containsKey(id)) {
                    return (context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects[id]!!
                }

                val initArgs = objectNode.args.map {
                    pythonBuildObject(it)
                }
                val constructor = ConstructorId(
                    objectNode.constructor,
                    initArgs.map { it.type }
                )

                val obj = newVar(objectNode.type) {
                    CgConstructorCall(
                        constructor,
                        initArgs
                    )
                }
                (context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects[id] = obj

                val state = objectNode.state.map { (key, value) ->
                    key to pythonBuildObject(value)
                }.toMap()
                val listitems = objectNode.listitems.map {
                    pythonBuildObject(it)
                }
                val dictitems = objectNode.dictitems.map { (key, value) ->
                    pythonBuildObject(key) to pythonBuildObject(value)
                }

                state.forEach { (key, value) ->
                    val fieldAccess = CgFieldAccess(obj, FieldId(objectNode.type, key))
                    fieldAccess `=` value
                }
                listitems.forEach {
                    +CgMethodCall(
                        obj,
                        PythonMethodId(
                            obj.type as PythonClassId,
                            "append",
                            NormalizedPythonAnnotation(pythonNoneClassId.name),
                            listOf(RawPythonAnnotation(it.type.name))
                        ),
                        listOf(it)
                    )
                }
                dictitems.forEach { (key, value) ->
                    val index = CgPythonIndex(
                        value.type as PythonClassId,
                        obj,
                        key
                    )
                    index `=` value
                }

                return obj
            }

            else -> {
                throw UnsupportedOperationException()
            }
        }
    }

    private fun pythonDeepEquals(expected: CgValue, actual: CgVariable) {
        require(expected is CgPythonTree) {
            "Expected value have to be CgPythonTree but `${expected::class}` found"
        }
        (context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects.clear()
        val expectedValue = pythonBuildObject(expected.tree)
        pythonDeepTreeEquals(expected.tree, expectedValue, actual)
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
                (testFrameworkManager as PytestManager).assertIsinstance(listOf(expected.type), actual)
            is UnittestManager ->
                (testFrameworkManager as UnittestManager).assertIsinstance(listOf(expected.type), actual)
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

            emptyLine()
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
                        pythonDeepTreeEquals(firstChild, indexExpected, indexActual)
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
        depth: Int = maxDepth
    ) {
        if (expectedNode.comparable || depth == 0) {
            emptyLineIfNeeded()
            testFrameworkManager.assertEquals(
                expected,
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