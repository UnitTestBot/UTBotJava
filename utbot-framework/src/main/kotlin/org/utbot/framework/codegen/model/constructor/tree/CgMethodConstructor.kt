package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.common.PathUtil
import org.utbot.common.isStatic
import org.utbot.framework.assemble.assemble
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour.PASS
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.builtin.closeMethodIdOrNull
import org.utbot.framework.codegen.model.constructor.builtin.forName
import org.utbot.framework.codegen.model.constructor.builtin.getClass
import org.utbot.framework.codegen.model.constructor.builtin.getTargetException
import org.utbot.framework.codegen.model.constructor.builtin.invoke
import org.utbot.framework.codegen.model.constructor.builtin.newInstance
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.constructor.util.CgStatementConstructor
import org.utbot.framework.codegen.model.constructor.util.EnvironmentFieldStateCache
import org.utbot.framework.codegen.model.constructor.util.FieldStateCache
import org.utbot.framework.codegen.model.constructor.util.classCgClassId
import org.utbot.framework.codegen.model.constructor.util.needExpectedDeclaration
import org.utbot.framework.codegen.model.constructor.util.overridesEquals
import org.utbot.framework.codegen.model.constructor.util.plus
import org.utbot.framework.codegen.model.constructor.util.setArgumentsArrayElement
import org.utbot.framework.codegen.model.constructor.util.typeCast
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgArrayElementAccess
import org.utbot.framework.codegen.model.tree.CgClassId
import org.utbot.framework.codegen.model.tree.CgDeclaration
import org.utbot.framework.codegen.model.tree.CgDocPreTagStatement
import org.utbot.framework.codegen.model.tree.CgDocRegularStmt
import org.utbot.framework.codegen.model.tree.CgDocumentationComment
import org.utbot.framework.codegen.model.tree.CgEqualTo
import org.utbot.framework.codegen.model.tree.CgErrorTestMethod
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgFieldAccess
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgLiteral
import org.utbot.framework.codegen.model.tree.CgMethod
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.tree.CgMultilineComment
import org.utbot.framework.codegen.model.tree.CgNotNullAssertion
import org.utbot.framework.codegen.model.tree.CgParameterDeclaration
import org.utbot.framework.codegen.model.tree.CgParameterKind
import org.utbot.framework.codegen.model.tree.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.model.tree.CgRegion
import org.utbot.framework.codegen.model.tree.CgSimpleRegion
import org.utbot.framework.codegen.model.tree.CgSingleLineComment
import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgStaticFieldAccess
import org.utbot.framework.codegen.model.tree.CgTestMethod
import org.utbot.framework.codegen.model.tree.CgTestMethodType
import org.utbot.framework.codegen.model.tree.CgTestMethodType.CRASH
import org.utbot.framework.codegen.model.tree.CgTestMethodType.FAILING
import org.utbot.framework.codegen.model.tree.CgTestMethodType.PARAMETRIZED
import org.utbot.framework.codegen.model.tree.CgTestMethodType.SUCCESSFUL
import org.utbot.framework.codegen.model.tree.CgTestMethodType.TIMEOUT
import org.utbot.framework.codegen.model.tree.CgTryCatch
import org.utbot.framework.codegen.model.tree.CgTypeCast
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.tree.buildParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.model.tree.buildTestMethod
import org.utbot.framework.codegen.model.tree.convertDocToCg
import org.utbot.framework.codegen.model.tree.toStatement
import org.utbot.framework.codegen.model.util.canBeSetFrom
import org.utbot.framework.codegen.model.util.equalTo
import org.utbot.framework.codegen.model.util.inc
import org.utbot.framework.codegen.model.util.canBeReadFrom
import org.utbot.framework.codegen.model.util.length
import org.utbot.framework.codegen.model.util.lessThan
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.fields.ExecutionStateAnalyzer
import org.utbot.framework.fields.FieldPath
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ConcreteExecutionFailureException
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtConcreteExecutionFailure
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.isNotNull
import org.utbot.framework.plugin.api.isNull
import org.utbot.framework.plugin.api.onFailure
import org.utbot.framework.plugin.api.onSuccess
import org.utbot.framework.plugin.api.util.doubleArrayClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.doubleWrapperClassId
import org.utbot.framework.plugin.api.util.executable
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.floatArrayClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.floatWrapperClassId
import org.utbot.framework.plugin.api.util.hasField
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isInnerClassEnclosingClassReference
import org.utbot.framework.plugin.api.util.isIterableOrMap
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isPrimitiveArray
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.kClass
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.plugin.api.util.wrapIfPrimitive
import org.utbot.framework.util.isInaccessibleViaReflection
import org.utbot.framework.util.isUnit
import org.utbot.summary.SummarySentenceConstants.TAB
import java.lang.reflect.InvocationTargetException
import java.security.AccessControlException
import java.lang.reflect.ParameterizedType

private const val DEEP_EQUALS_MAX_DEPTH = 5 // TODO move it to plugin settings?

internal class CgMethodConstructor(val context: CgContext) : CgContextOwner by context,
    CgFieldStateManager by CgComponents.getFieldStateManagerBy(context),
    CgCallableAccessManager by CgComponents.getCallableAccessManagerBy(context),
    CgStatementConstructor by CgComponents.getStatementConstructorBy(context) {

    private val nameGenerator = CgComponents.getNameGeneratorBy(context)
    private val testFrameworkManager = CgComponents.getTestFrameworkManagerBy(context)

    private val variableConstructor = CgComponents.getVariableConstructorBy(context)
    private val mockFrameworkManager = CgComponents.getMockFrameworkManagerBy(context)

    private val floatDelta: Float = 1e-6f
    private val doubleDelta = 1e-6

    // a model for execution result (it is lateinit because execution can fail,
    // and we need it only on assertions generation stage
    private lateinit var resultModel: UtModel

    private lateinit var methodType: CgTestMethodType

    private val fieldsOfExecutionResults = mutableMapOf<Pair<FieldId, Int>, MutableList<UtModel>>()

    private fun setupInstrumentation() {
        if (currentExecution is UtSymbolicExecution) {
            val execution = currentExecution as UtSymbolicExecution
            val instrumentation = execution.instrumentation
            if (instrumentation.isEmpty()) return

            if (generateWarningsForStaticMocking && forceStaticMocking == ForceStaticMocking.DO_NOT_FORCE) {
                // warn user about possible flaky tests
                multilineComment(forceStaticMocking.warningMessage)
                return
            }

            instrumentation
                .filterIsInstance<UtNewInstanceInstrumentation>()
                .forEach { mockFrameworkManager.mockNewInstance(it) }
            instrumentation
                .filterIsInstance<UtStaticMethodInstrumentation>()
                .groupBy { it.methodId.classId }
                .forEach { (classId, methodMocks) -> mockFrameworkManager.mockStaticMethodsOfClass(classId, methodMocks) }

            if (generateWarningsForStaticMocking && forceStaticMocking == ForceStaticMocking.FORCE) {
                // warn user about forced using static mocks
                multilineComment(forceStaticMocking.warningMessage)
            }
        }
    }

    /**
     * Create variables for initial values of the static fields and store them in [prevStaticFieldValues]
     * in order to use these variables at the end of the test to restore the initial static fields state.
     *
     * Note:
     * Later in the test method we also cache the 'before' and 'after' states of fields (including static fields).
     * This cache is stored in [statesCache].
     *
     * However, it is _not_ the same cache as [prevStaticFieldValues].
     * The [statesCache] should not be confused with [prevStaticFieldValues] cache.
     *
     * The difference is that [prevStaticFieldValues] contains the static field states before we made _any_ changes.
     * On the other hand, [statesCache] contains 'before' and 'after' states where the 'before' state is
     * the state that we _specifically_ set up in order to cover a certain branch.
     *
     * Thus, this method only caches an actual initial static fields state in order to recover it
     * at the end of the test, and it has nothing to do with the 'before' and 'after' caches.
     */
    private fun rememberInitialStaticFields(statics: Map<FieldId, UtModel>) {
        val accessibleStaticFields = statics.accessibleFields()
        for ((field, _) in accessibleStaticFields) {
            val declaringClass = field.declaringClass
            val fieldAccessible = field.canBeReadFrom(context)

            // prevValue is nullable if not accessible because of getStaticFieldValue(..) : Any?
            val prevValue = newVar(
                CgClassId(field.type, isNullable = !fieldAccessible),
                "prev${field.name.capitalize()}"
            ) {
                if (fieldAccessible) {
                    declaringClass[field]
                } else {
                    val declaringClassVar = newVar(classCgClassId) {
                        Class::class.id[forName](declaringClass.name)
                    }
                    utilsClassId[getStaticFieldValue](declaringClassVar, field.name)
                }
            }
            // remember the previous value of a static field to recover it at the end of the test
            prevStaticFieldValues[field] = prevValue
        }
    }

    private fun substituteStaticFields(statics: Map<FieldId, UtModel>, isParametrized: Boolean = false) {
        val accessibleStaticFields = statics.accessibleFields()
        for ((field, model) in accessibleStaticFields) {
            val declaringClass = field.declaringClass
            val fieldAccessible = field.canBeSetFrom(context)

            val fieldValue = if (isParametrized) {
                currentMethodParameters[CgParameterKind.Statics(model)]
            } else {
                variableConstructor.getOrCreateVariable(model, field.name)
            }

            if (fieldAccessible) {
                declaringClass[field] `=` fieldValue
            } else {
                val declaringClassVar = newVar(classCgClassId) {
                    Class::class.id[forName](declaringClass.name)
                }
                +utilsClassId[setStaticField](declaringClassVar, field.name, fieldValue)
            }
        }
    }

    private fun recoverStaticFields() {
        for ((field, prevValue) in prevStaticFieldValues.accessibleFields()) {
            if (field.canBeSetFrom(context)) {
                field.declaringClass[field] `=` prevValue
            } else {
                val declaringClass = getClassOf(field.declaringClass)
                +utilsClassId[setStaticField](declaringClass, field.name, prevValue)
            }
        }
    }

    private fun <E> Map<FieldId, E>.accessibleFields(): Map<FieldId, E> = filterKeys { !it.isInaccessibleViaReflection }

    /**
     * Generates result assertions for unit tests.
     */
    private fun generateResultAssertions() {
        when (currentExecutable) {
            is ConstructorId -> generateConstructorCall(currentExecutable!!, currentExecution!!)
            is BuiltinMethodId -> error("Unexpected BuiltinMethodId $currentExecutable while generating result assertions")
            is MethodId -> {
                emptyLineIfNeeded()
                val method = currentExecutable as MethodId
                val currentExecution = currentExecution!!
                // build assertions
                currentExecution.result
                    .onSuccess { result ->
                        methodType = SUCCESSFUL

                        // TODO possible engine bug - void method return type and result not UtVoidModel
                        if (result.isUnit() || method.returnType == voidClassId) {
                            +thisInstance[method](*methodArguments.toTypedArray())
                        } else {
                            resultModel = result
                            val expected = variableConstructor.getOrCreateVariable(result, "expected")
                            assertEquality(expected, actual)
                        }
                    }
                    .onFailure { exception ->
                        processExecutionFailure(currentExecution, exception)
                    }
            }
            else -> {} // TODO: check this specific case
        }
    }

    private fun processExecutionFailure(execution: UtExecution, exception: Throwable) {
        val methodInvocationBlock = {
            with(currentExecutable) {
                when (this) {
                    is MethodId -> thisInstance[this](*methodArguments.toTypedArray()).intercepted()
                    is ConstructorId -> this(*methodArguments.toTypedArray()).intercepted()
                    else -> {} // TODO: check this specific case
                }
            }
        }

        if (shouldTestPassWithException(execution, exception)) {
            testFrameworkManager.expectException(exception::class.id) {
                methodInvocationBlock()
            }
            methodType = SUCCESSFUL

            return
        }

        if (shouldTestPassWithTimeoutException(execution, exception)) {
            writeWarningAboutTimeoutExceeding()
            testFrameworkManager.expectTimeout(hangingTestsTimeout.timeoutMs) {
                methodInvocationBlock()
            }
            methodType = TIMEOUT

            return
        }

        when (exception) {
            is ConcreteExecutionFailureException -> {
                methodType = CRASH
                writeWarningAboutCrash()
            }
            is AccessControlException -> {
                methodType = CRASH
                writeWarningAboutFailureTest(exception)
                return
            }
            else -> {
                methodType = FAILING
                writeWarningAboutFailureTest(exception)
            }
        }

        methodInvocationBlock()
    }

    private fun shouldTestPassWithException(execution: UtExecution, exception: Throwable): Boolean {
        if (exception is AccessControlException) return false
        // tests with timeout or crash should be processed differently
        if (exception is TimeoutException || exception is ConcreteExecutionFailureException) return false

        val exceptionRequiresAssert = exception !is RuntimeException || runtimeExceptionTestsBehaviour == PASS
        val exceptionIsExplicit = execution.result is UtExplicitlyThrownException
        return exceptionRequiresAssert || exceptionIsExplicit
    }

    private fun shouldTestPassWithTimeoutException(execution: UtExecution, exception: Throwable): Boolean {
        return execution.result is UtTimeoutException || exception is TimeoutException
    }

    private fun writeWarningAboutTimeoutExceeding() {
        +CgMultilineComment(
            listOf(
                "This execution may take longer than the ${hangingTestsTimeout.timeoutMs} ms timeout",
                " and therefore fail due to exceeding the timeout."
            )
        )
    }

    private fun writeWarningAboutFailureTest(exception: Throwable) {
        require(currentExecutable is ExecutableId)
        val executableName = "${currentExecutable!!.classId.name}.${currentExecutable!!.name}"

        val warningLine = mutableListOf(
            "This test fails because method [$executableName] produces [$exception]".escapeControlChars()
        )

        val neededStackTraceLines = mutableListOf<String>()
        var executableCallFound = false
        exception.stackTrace.reversed().forEach { stackTraceElement ->
            val line = stackTraceElement.toString()
            if (line.startsWith(executableName)) {
                executableCallFound = true
            }
            if (executableCallFound) {
                neededStackTraceLines += TAB + line
            }
        }

        +CgMultilineComment(warningLine + neededStackTraceLines.reversed())
    }

    private fun String.escapeControlChars() : String {
        return this.replace("\b", "\\b").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")
    }

    private fun writeWarningAboutCrash() {
        +CgSingleLineComment("This invocation possibly crashes JVM")
    }

    /**
     * Generates result assertions in parameterized tests for successful executions
     * and just runs the method if all executions are unsuccessful.
     */
    private fun generateAssertionsForParameterizedTest() {
        emptyLineIfNeeded()

        when (currentExecutable) {
            is ConstructorId -> generateConstructorCall(currentExecutable!!, currentExecution!!)
            is MethodId -> {
                val method = currentExecutable as MethodId
                currentExecution!!.result
                    .onSuccess { result ->
                        if (result.isUnit()) {
                            +thisInstance[method](*methodArguments.toTypedArray())
                        } else {
                            //"generic" expected variable is represented with a wrapper if
                            //actual result is primitive to support cases with exceptions.
                            resultModel = if (result is UtPrimitiveModel) assemble(result) else result

                            val expectedVariable = currentMethodParameters[CgParameterKind.ExpectedResult]!!
                            val expectedExpression = CgNotNullAssertion(expectedVariable)

                            assertEquality(expectedExpression, actual)
                        }
                    }
                    .onFailure { thisInstance[method](*methodArguments.toTypedArray()).intercepted() }
            }
            else -> {} // TODO: check this specific case
        }
    }

    /**
     * Generates assertions for field states.
     *
     * Note: not supported in parameterized tests.
     */
    private fun generateFieldStateAssertions() {
        val thisInstanceCache = statesCache.thisInstance
        for (path in thisInstanceCache.paths) {
            assertStatesByPath(thisInstanceCache, path)
        }
        for (argumentCache in statesCache.arguments) {
            for (path in argumentCache.paths) {
                assertStatesByPath(argumentCache, path)
            }
        }
        for ((_, staticFieldCache) in statesCache.classesWithStaticFields) {
            for (path in staticFieldCache.paths) {
                assertStatesByPath(staticFieldCache, path)
            }
        }
    }

    /**
     * If the given field is _not_ of reference type, then the variable for its 'before' state
     * is not created, because we only need its final state to make an assertion.
     * For reference type fields, in turn, we make an assertion assertFalse(before == after).
     */
    private fun assertStatesByPath(cache: FieldStateCache, path: FieldPath) {
        emptyLineIfNeeded()
        val beforeVariable = cache.before[path]?.variable
        val (afterVariable, afterModel) = cache.after[path]!!

        if (afterModel !is UtReferenceModel) {
            val expectedAfter =
                variableConstructor.getOrCreateVariable(afterModel, "expected" + afterVariable.name.capitalize())
            assertEquality(expectedAfter, afterVariable)
        } else {
            if (beforeVariable != null)
                testFrameworkManager.assertBoolean(false, beforeVariable equalTo afterVariable)
            // TODO: fail here
        }
    }

    private fun assertDeepEquals(
        expectedModel: UtModel,
        expected: CgVariable?,
        actual: CgVariable,
        depth: Int,
        visitedModels: MutableSet<UtModel>,
    ) {
        if (expectedModel in visitedModels) return

        var expected = expected
        if (expected == null) {
            require(!needExpectedDeclaration(expectedModel))
            expected = actual
        }

        visitedModels += expectedModel

        with(testFrameworkManager) {
            if (depth >= DEEP_EQUALS_MAX_DEPTH) {
                currentBlock += CgSingleLineComment("Current deep equals depth exceeds max depth $DEEP_EQUALS_MAX_DEPTH")
                currentBlock += getDeepEqualsAssertion(expected, actual).toStatement()
                return
            }

            when (expectedModel) {
                is UtPrimitiveModel -> {
                    currentBlock += when {
                        (expected.type == floatClassId || expected.type == floatWrapperClassId) ->
                            assertions[assertFloatEquals]( // cast have to be not safe here because of signature
                                typeCast(floatClassId, expected, isSafetyCast = false),
                                typeCast(floatClassId, actual, isSafetyCast = false),
                                floatDelta
                            )
                        (expected.type == doubleClassId || expected.type == doubleWrapperClassId) ->
                            assertions[assertDoubleEquals]( // cast have to be not safe here because of signature
                                typeCast(doubleClassId, expected, isSafetyCast = false),
                                typeCast(doubleClassId, actual, isSafetyCast = false),
                                doubleDelta
                            )
                        expectedModel.value is Boolean -> {
                            when (parametrizedTestSource) {
                                ParametrizedTestSource.DO_NOT_PARAMETRIZE ->
                                    if (expectedModel.value as Boolean) {
                                        assertions[assertTrue](actual)
                                    } else {
                                        assertions[assertFalse](actual)
                                    }
                                ParametrizedTestSource.PARAMETRIZE ->
                                    assertions[assertEquals](expected, actual)
                            }
                        }
                        // other primitives and string
                        else -> {
                            require(expected.type.isPrimitive || expected.type == String::class.java) {
                                "Expected primitive or String but got ${expected.type}"
                            }
                            assertions[assertEquals](expected, actual)
                        }
                    }.toStatement()
                }
                is UtEnumConstantModel -> {
                    currentBlock += assertions[assertEquals](
                        expected,
                        actual
                    ).toStatement()
                }
                is UtClassRefModel -> {
                    // TODO this stuff is needed because Kotlin has javaclass property instead of Java getClass method
                    //  probably it is better to change getClass method behaviour in the future
                    val actualObject: CgVariable = when (codegenLanguage) {
                        CodegenLanguage.KOTLIN -> newVar(
                            baseType = objectClassId,
                            baseName = variableConstructor.constructVarName("actualObject"),
                            init = { CgTypeCast(objectClassId, actual) }
                        )
                        else -> actual
                    }

                    currentBlock += assertions[assertEquals](
                        CgGetJavaClass(expected.type),
                        actualObject[getClass]()
                    ).toStatement()
                }
                is UtNullModel -> {
                    currentBlock += assertions[assertNull](actual).toStatement()
                }
                is UtArrayModel -> {
                    val arrayInfo = expectedModel.collectArrayInfo()
                    val nestedElementClassId = arrayInfo.nestedElementClassId
                        ?: error("Expected element class id from array ${arrayInfo.classId} but null found")

                    if (!arrayInfo.isPrimitiveArray) {
                        // array of objects, have to use deep equals

                        // We can't use for loop here because array model can contain different models
                        // and there is no a general approach to process it in loop
                        // For example, actual can be Object[3] and
                        // actual[0] instance of Point[][]
                        // actual[1] instance of int[][][]
                        // actual[2] instance of Object[]

                        addArraysLengthAssertion(expected, actual)
                        currentBlock += getDeepEqualsAssertion(expected, actual).toStatement()
                        return
                    }

                    // It does not work for Double and Float because JUnit does not have equals overloading with wrappers
                    if (nestedElementClassId == floatClassId || nestedElementClassId == doubleClassId) {
                        floatingPointArraysDeepEquals(arrayInfo, expected, actual)
                        return
                    }

                    // common primitive array, can use default array equals
                    addArraysLengthAssertion(expected, actual)
                    currentBlock += getArrayEqualsAssertion(
                        expectedModel.classId,
                        typeCast(expectedModel.classId, expected, isSafetyCast = true),
                        typeCast(expectedModel.classId, actual, isSafetyCast = true)
                    ).toStatement()
                }
                is UtAssembleModel -> {
                    if (expectedModel.classId.isPrimitiveWrapper) {
                        currentBlock += assertions[assertEquals](expected, actual).toStatement()
                        return
                    }

                    // UtCompositeModel deep equals is much more easier and human friendly
                    expectedModel.origin?.let {
                        assertDeepEquals(it, expected, actual, depth, visitedModels)
                        return
                    }

                    // special case for strings as they are constructed from UtAssembleModel but can be compared with equals
                    if (expectedModel.classId == stringClassId) {
                        currentBlock += assertions[assertEquals](
                            expected,
                            actual
                        ).toStatement()
                        return
                    }

                    // We cannot implement deep equals for not field set model
                    // because if modification was made by direct field access, we can compare modifications by field access too
                    // (like in modification expected.value = 5 we can assert equality expected.value and actual.value),
                    // but in other cases we don't know what fields do we need to compare
                    // (like if modification was List add() method invocation)

                    // We can add some heuristics to process standard assemble models like List, Set and Map.
                    // So, there is a space for improvements
                    if (expectedModel.modificationsChain.isEmpty() || expectedModel.modificationsChain.any { it !is UtDirectSetFieldModel }) {
                        currentBlock += getDeepEqualsAssertion(expected, actual).toStatement()
                        return
                    }

                    for (modificationStep in expectedModel.modificationsChain) {
                        modificationStep as UtDirectSetFieldModel
                        val fieldId = modificationStep.fieldId
                        val fieldModel = modificationStep.fieldModel

                        // we should not process enclosing class
                        // (actually, we do not do it properly anyway)
                        if (fieldId.isInnerClassEnclosingClassReference) continue

                        traverseFieldRecursively(
                            fieldId,
                            fieldModel,
                            expected,
                            actual,
                            depth,
                            visitedModels
                        )
                    }
                }
                is UtCompositeModel -> {
                    // Basically, to compare two iterables or maps, we need to iterate over them and compare each entry.
                    // But it leads to a lot of trash code in each test method, and it is more clear to use
                    // outer deep equals here
                    if (expected.isIterableOrMap()) {
                        currentBlock += CgSingleLineComment(
                            "${expected.type.canonicalName} is iterable or Map, use outer deep equals to iterate over"
                        )
                        currentBlock += getDeepEqualsAssertion(expected, actual).toStatement()

                        return
                    }

                    if (expected.hasNotParametrizedCustomEquals()) {
                        // We rely on already existing equals
                        currentBlock += CgSingleLineComment("${expected.type.canonicalName} has overridden equals method")
                        currentBlock += assertions[assertEquals](expected, actual).toStatement()

                        return
                    }

                    for ((fieldId, fieldModel) in expectedModel.fields) {
                        // we should not process enclosing class
                        // (actually, we do not do it properly anyway)
                        if (fieldId.isInnerClassEnclosingClassReference) continue

                        traverseFieldRecursively(
                            fieldId,
                            fieldModel,
                            expected,
                            actual,
                            depth,
                            visitedModels
                        )
                    }
                }
                is UtLambdaModel -> Unit // we do not check equality of lambdas
                is UtVoidModel -> {
                    // Unit result is considered in generateResultAssertions method
                    error("Unexpected UtVoidModel in deep equals")
                }
            }
        }
    }

    private fun TestFrameworkManager.addArraysLengthAssertion(
        expected: CgVariable,
        actual: CgVariable,
    ): CgDeclaration {
        val cgGetLengthDeclaration = CgDeclaration(
            intClassId,
            variableConstructor.constructVarName("${expected.name}Size"),
            expected.length(this@CgMethodConstructor)
        )
        currentBlock += cgGetLengthDeclaration
        currentBlock += assertions[assertEquals](
            cgGetLengthDeclaration.variable,
            actual.length(this@CgMethodConstructor)
        ).toStatement()

        return cgGetLengthDeclaration
    }

    /**
     * Generate deep equals for float and double any-dimensional arrays (DOES NOT includes wrappers)
     */
    private fun TestFrameworkManager.floatingPointArraysDeepEquals(
        expectedArrayInfo: ClassIdArrayInfo,
        expected: CgVariable,
        actual: CgVariable,
    ) {
        val cgGetLengthDeclaration = addArraysLengthAssertion(expected, actual)

        val nestedElementClassId = expectedArrayInfo.nestedElementClassId
            ?: error("Expected from floating point array ${expectedArrayInfo.classId} to contain elements but null found")
        require(nestedElementClassId == floatClassId || nestedElementClassId == doubleClassId) {
            "Expected float or double ClassId but `$nestedElementClassId` found"
        }

        if (expectedArrayInfo.isSingleDimensionalArray) {
            // we can use array equals for all single dimensional arrays
            currentBlock += when (nestedElementClassId) {
                floatClassId -> getFloatArrayEqualsAssertion(
                    typeCast(floatArrayClassId, expected, isSafetyCast = true),
                    typeCast(floatArrayClassId, actual, isSafetyCast = true),
                    floatDelta
                )
                else -> getDoubleArrayEqualsAssertion(
                    typeCast(doubleArrayClassId, expected, isSafetyCast = true),
                    typeCast(doubleArrayClassId, actual, isSafetyCast = true),
                    doubleDelta
                )
            }.toStatement()
        } else {
            // we can't use array equals for multidimensional double and float arrays
            // so we need to go deeper to single-dimensional array
            forLoop {
                val (i, init) = variableConstructor.loopInitialization(intClassId, "i", initializer = 0)
                initialization = init
                condition = i lessThan cgGetLengthDeclaration.variable.resolve()
                update = i.inc()

                statements = block {
                    val expectedNestedElement = newVar(
                        baseType = expected.type.elementClassId!!,
                        baseName = variableConstructor.constructVarName("${expected.name}NestedElement"),
                        init = { CgArrayElementAccess(expected, i) }
                    )

                    val actualNestedElement = newVar(
                        baseType = actual.type.elementClassId!!,
                        baseName = variableConstructor.constructVarName("${actual.name}NestedElement"),
                        init = { CgArrayElementAccess(actual, i) }
                    )

                    emptyLine()

                    ifStatement(
                        CgEqualTo(expectedNestedElement, nullLiteral()),
                        trueBranch = { +assertions[assertNull](actualNestedElement).toStatement() },
                        falseBranch = {
                            floatingPointArraysDeepEquals(
                                expectedArrayInfo.getNested(),
                                expectedNestedElement,
                                actualNestedElement,
                            )
                        }
                    )
                }
            }
        }
    }

    private fun CgVariable.isIterableOrMap(): Boolean = type.isIterableOrMap

    /**
     * Some classes have overridden equals method, but it doesn't work properly.
     * For example, List<T> has overridden equals method but it relies on T equals.
     * So, if T doesn't override equals, assertEquals with List<T> fails.
     * Therefore, all standard collections and map can fail.
     * We overapproximate this assumption for all parametrized classes because we can't be sure that
     * overridden equals doesn't rely on type parameters equals.
     */
    private fun CgVariable.hasNotParametrizedCustomEquals(): Boolean {
        if (type.jClass.overridesEquals()) {
            // type parameters is list of class type parameters - empty if class is not generic
            val typeParameters = type.kClass.typeParameters

            return typeParameters.isEmpty()
        }

        return false
    }

    private fun traverseFieldRecursively(
        fieldId: FieldId,
        fieldModel: UtModel,
        expected: CgVariable,
        actual: CgVariable,
        depth: Int,
        visitedModels: MutableSet<UtModel>
    ) {
        // if field is static, it is represents itself in "before" and
        // "after" state: no need to assert its equality to itself.
        if (fieldId.isStatic) {
            return
        }

        // if model is already processed, so we don't want to add new statements
        if (fieldModel in visitedModels) {
            currentBlock += testFrameworkManager.getDeepEqualsAssertion(expected, actual).toStatement()
            return
        }

        when (parametrizedTestSource) {
            ParametrizedTestSource.DO_NOT_PARAMETRIZE -> {
                traverseField(fieldId, fieldModel, expected, actual, depth, visitedModels)
            }

            ParametrizedTestSource.PARAMETRIZE -> {
                traverseFieldForParametrizedTest(fieldId, fieldModel, expected, actual, depth, visitedModels)
            }
        }
    }

    private fun traverseField(
        fieldId: FieldId,
        fieldModel: UtModel,
        expected: CgVariable,
        actual: CgVariable,
        depth: Int,
        visitedModels: MutableSet<UtModel>
    ) {
        // fieldModel is not visited and will be marked in assertDeepEquals call
        val fieldName = fieldId.name
        var expectedVariable: CgVariable? = null

        if (needExpectedDeclaration(fieldModel)) {
            val expectedFieldDeclaration = createDeclarationForFieldFromVariable(fieldId, expected, fieldName)

            currentBlock += expectedFieldDeclaration
            expectedVariable = expectedFieldDeclaration.variable
        }

        val actualFieldDeclaration = createDeclarationForFieldFromVariable(fieldId, actual, fieldName)
        currentBlock += actualFieldDeclaration

        assertDeepEquals(
            fieldModel,
            expectedVariable,
            actualFieldDeclaration.variable,
            depth + 1,
            visitedModels,
        )
        emptyLineIfNeeded()
    }

    private fun traverseFieldForParametrizedTest(
        fieldId: FieldId,
        fieldModel: UtModel,
        expected: CgVariable,
        actual: CgVariable,
        depth: Int,
        visitedModels: MutableSet<UtModel>
    ) {
        val fieldResultModels = fieldsOfExecutionResults[fieldId to depth]
        val nullResultModelInExecutions = fieldResultModels?.find { it.isNull() }
        val notNullResultModelInExecutions = fieldResultModels?.find { it.isNotNull() }

        val hasNullResultModel = nullResultModelInExecutions != null
        val hasNotNullResultModel = notNullResultModelInExecutions != null

        val needToSubstituteFieldModel = fieldModel is UtNullModel && hasNotNullResultModel

        val fieldModelForAssert = if (needToSubstituteFieldModel) notNullResultModelInExecutions!! else fieldModel

        // fieldModel is not visited and will be marked in assertDeepEquals call
        val fieldName = fieldId.name
        var expectedVariable: CgVariable? = null

        val needExpectedDeclaration = needExpectedDeclaration(fieldModelForAssert)
        if (needExpectedDeclaration) {
            val expectedFieldDeclaration = createDeclarationForFieldFromVariable(fieldId, expected, fieldName)

            currentBlock += expectedFieldDeclaration
            expectedVariable = expectedFieldDeclaration.variable
        }

        val actualFieldDeclaration = createDeclarationForFieldFromVariable(fieldId, actual, fieldName)
        currentBlock += actualFieldDeclaration

        if (needExpectedDeclaration && hasNullResultModel) {
            ifStatement(
                CgEqualTo(expectedVariable!!, nullLiteral()),
                trueBranch = { +testFrameworkManager.assertions[testFramework.assertNull](actualFieldDeclaration.variable).toStatement() },
                falseBranch = {
                    assertDeepEquals(
                        fieldModelForAssert,
                        expectedVariable,
                        actualFieldDeclaration.variable,
                        depth + 1,
                        visitedModels,
                    )
                }
            )
        } else {
            assertDeepEquals(
                fieldModelForAssert,
                expectedVariable,
                actualFieldDeclaration.variable,
                depth + 1,
                visitedModels,
            )
        }
        emptyLineIfNeeded()
    }

    private fun collectExecutionsResultFields() {
        val successfulExecutionsModels = allExecutions
            .filter {
                it.result is UtExecutionSuccess
            }.map {
                (it.result as UtExecutionSuccess).model
            }

        for (model in successfulExecutionsModels) {
            when (model) {
                is UtCompositeModel -> {
                    for ((fieldId, fieldModel) in model.fields) {
                        collectExecutionsResultFieldsRecursively(fieldId, fieldModel, 0)
                    }
                }

                is UtAssembleModel -> {
                    model.origin?.let {
                        for ((fieldId, fieldModel) in it.fields) {
                            collectExecutionsResultFieldsRecursively(fieldId, fieldModel, 0)
                        }
                    }
                }

                // Lambdas do not have fields. They have captured values, but we do not consider them here.
                is UtLambdaModel,
                is UtNullModel,
                is UtPrimitiveModel,
                is UtArrayModel,
                is UtClassRefModel,
                is UtEnumConstantModel,
                is UtVoidModel -> {
                    // only [UtCompositeModel] and [UtAssembleModel] have fields to traverse
                }
            }
        }
    }

    private fun collectExecutionsResultFieldsRecursively(
        fieldId: FieldId,
        fieldModel: UtModel,
        depth: Int,
    ) {
        if (depth >= DEEP_EQUALS_MAX_DEPTH) {
            return
        }

        val fieldKey = fieldId to depth
        fieldsOfExecutionResults.getOrPut(fieldKey) { mutableListOf() } += fieldModel

        when (fieldModel) {
            is UtCompositeModel -> {
                for ((id, model) in fieldModel.fields) {
                    collectExecutionsResultFieldsRecursively(id, model, depth + 1)
                }
            }

            is UtAssembleModel -> {
                fieldModel.origin?.let {
                    for ((id, model) in it.fields) {
                        collectExecutionsResultFieldsRecursively(id, model, depth + 1)
                    }
                }
            }

            // Lambdas do not have fields. They have captured values, but we do not consider them here.
            is UtLambdaModel,
            is UtNullModel,
            is UtPrimitiveModel,
            is UtArrayModel,
            is UtClassRefModel,
            is UtEnumConstantModel,
            is UtVoidModel -> {
                // only [UtCompositeModel] and [UtAssembleModel] have fields to traverse
            }
        }
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun createDeclarationForFieldFromVariable(
        fieldId: FieldId,
        variable: CgVariable,
        fieldName: String
    ): CgDeclaration {
        val expectedFieldDeclaration = createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
            baseType = fieldId.type,
            baseName = "${variable.name}${fieldName.capitalize()}",
            init = { fieldId.getAccessExpression(variable) }
        ).either(
            { declaration -> declaration },
            { unexpectedExistingVariable ->
                error(
                    "Unexpected existing variable for field $fieldName with type ${fieldId.type} " +
                            "from expected variable ${variable.name} with type ${variable.type}"
                )
            }
        )
        return expectedFieldDeclaration
    }

    private fun FieldId.getAccessExpression(variable: CgVariable): CgExpression =
        // Can directly access field only if it is declared in variable class (or in its ancestors)
        // and is accessible from current package
        if (variable.type.hasField(this) && canBeReadFrom(context)) {
            if (jField.isStatic) CgStaticFieldAccess(this) else CgFieldAccess(variable, this)
        } else {
            utilsClassId[getFieldValue](variable, this.declaringClass.name, this.name)
        }

    /**
     * Stores array information about ClassId.
     * @property classId ClassId itself.
     * @property nestedElementClassId the most nested element ClassId if it is array and null otherwise.
     * @property dimensions 0 for non-arrays and number of dimensions in case of arrays.
     */
    private data class ClassIdArrayInfo(val classId: ClassId, val nestedElementClassId: ClassId?, val dimensions: Int) {
        val isArray get(): Boolean = dimensions > 0

        val isPrimitiveArray get(): Boolean = isArray && nestedElementClassId!!.isPrimitive

        val isSingleDimensionalArray get(): Boolean = dimensions == 1

        fun getNested(): ClassIdArrayInfo {
            require(dimensions > 0) { "Trying to get nested array from not array type $classId" }

            return copy(dimensions = dimensions - 1)
        }
    }

    private val UtArrayModel.isArray: Boolean
        get() = this.classId.isArray

    private fun UtArrayModel.collectArrayInfo(): ClassIdArrayInfo {
        if (!isArray) return ClassIdArrayInfo(
            classId = classId,
            nestedElementClassId = null,
            dimensions = 0
        )

        val nestedElementClassIdList = generateSequence(classId.elementClassId) { it.elementClassId }.toList()
        val dimensions = nestedElementClassIdList.size
        val nestedElementClassId = nestedElementClassIdList.last()

        return ClassIdArrayInfo(classId, nestedElementClassId, dimensions)
    }

    private fun assertEquality(expected: CgValue, actual: CgVariable) {
        when {
            expected.type.isArray -> {
                // TODO: How to compare arrays of Float and Double wrappers?
                // TODO: For example, JUnit5 does not have an assertEquals() overload for these wrappers.
                // TODO: So for now we compare arrays of these wrappers as arrays of Objects, but that is probably wrong.
                when (expected.type.elementClassId!!) {
                    floatClassId -> testFrameworkManager.assertFloatArrayEquals(
                        typeCast(floatArrayClassId, expected, isSafetyCast = true),
                        typeCast(floatArrayClassId, actual, isSafetyCast = true),
                        floatDelta
                    )
                    doubleClassId -> testFrameworkManager.assertDoubleArrayEquals(
                        typeCast(doubleArrayClassId, expected, isSafetyCast = true),
                        typeCast(doubleArrayClassId, actual, isSafetyCast = true),
                        doubleDelta
                    )
                    else -> {
                        val targetType = when {
                            expected.type.isPrimitiveArray -> expected.type
                            actual.type.isPrimitiveArray -> actual.type
                            else -> objectArrayClassId
                        }
                        if (targetType.isPrimitiveArray) {
                            // we can use simple arrayEquals for primitive arrays
                            testFrameworkManager.assertArrayEquals(
                                targetType,
                                typeCast(targetType, expected, isSafetyCast = true),
                                typeCast(targetType, actual, isSafetyCast = true)
                            )
                        } else {
                            // array of objects, have to use deep equals

                            when (expected) {
                                is CgLiteral -> testFrameworkManager.assertEquals(expected, actual)
                                is CgNotNullAssertion -> generateForNotNullAssertion(expected, actual)
                                else -> {
                                    require(resultModel is UtArrayModel) {
                                        "Result model have to be UtArrayModel to generate arrays assertion " +
                                                "but `${resultModel::class}` found"
                                    }
                                    generateDeepEqualsOrNullAssertion(expected, actual)
                                }
                            }
                        }
                    }
                }
            }
            else -> when {
                (expected.type == floatClassId || expected.type == floatWrapperClassId) -> {
                    testFrameworkManager.assertFloatEquals(
                        typeCast(floatClassId, expected, isSafetyCast = true),
                        typeCast(floatClassId, actual, isSafetyCast = true),
                        floatDelta
                    )
                }
                (expected.type == doubleClassId || expected.type == doubleWrapperClassId) -> {
                    testFrameworkManager.assertDoubleEquals(
                        typeCast(doubleClassId, expected, isSafetyCast = true),
                        typeCast(doubleClassId, actual, isSafetyCast = true),
                        doubleDelta
                    )
                }
                expected == nullLiteral() -> testFrameworkManager.assertNull(actual)
                expected is CgLiteral && expected.value is Boolean -> {
                    when (parametrizedTestSource) {
                        ParametrizedTestSource.DO_NOT_PARAMETRIZE ->
                            testFrameworkManager.assertBoolean(expected.value, actual)
                        ParametrizedTestSource.PARAMETRIZE ->
                            testFrameworkManager.assertEquals(expected, actual)
                    }
                }
                else -> {
                    when (expected) {
                        is CgLiteral -> testFrameworkManager.assertEquals(expected, actual)
                        is CgNotNullAssertion -> generateForNotNullAssertion(expected, actual)
                        else -> generateDeepEqualsOrNullAssertion(expected, actual)
                    }
                }
            }
        }
    }

    private fun generateForNotNullAssertion(expected: CgNotNullAssertion, actual: CgVariable) {
        require(expected.expression is CgVariable) {
            "Only CgVariable wrapped in CgNotNullAssertion is supported in deepEquals"
        }
        generateDeepEqualsOrNullAssertion(expected.expression, actual)
    }

    private fun generateConstructorCall(currentExecutableId: ExecutableId, currentExecution: UtExecution) {
        // we cannot generate any assertions for constructor testing
        // but we need to generate a constructor call
        val constructorCall = currentExecutableId as ConstructorId
        currentExecution.result
            .onSuccess {
                methodType = SUCCESSFUL

                require(!constructorCall.classId.isInner) {
                    "Inner class ${constructorCall.classId} constructor testing is not supported yet"
                }

                actual = newVar(constructorCall.classId, "actual") {
                    constructorCall(*methodArguments.toTypedArray())
                }
            }
            .onFailure { exception ->
                processExecutionFailure(currentExecution, exception)
            }
    }

    /**
     * We can't use standard deepEquals method in parametrized tests
     * because nullable objects require different asserts.
     * See https://github.com/UnitTestBot/UTBotJava/issues/252 for more details.
     */
    private fun generateDeepEqualsOrNullAssertion(
        expected: CgValue,
        actual: CgVariable,
    ) {
        when (parametrizedTestSource) {
            ParametrizedTestSource.DO_NOT_PARAMETRIZE -> generateDeepEqualsAssertion(expected, actual)
            ParametrizedTestSource.PARAMETRIZE -> {
                collectExecutionsResultFields()

                when {
                    actual.type.isPrimitive -> generateDeepEqualsAssertion(expected, actual)
                    else -> ifStatement(
                        CgEqualTo(expected, nullLiteral()),
                        trueBranch = { +testFrameworkManager.assertions[testFramework.assertNull](actual).toStatement() },
                        falseBranch = {
                            +testFrameworkManager.assertions[testFrameworkManager.assertNotNull](actual).toStatement()
                            generateDeepEqualsAssertion(expected, actual)
                        }
                    )
                }
            }
        }
    }

    private fun generateDeepEqualsAssertion(
        expected: CgValue,
        actual: CgVariable,
    ) {
        require(expected is CgVariable) {
            "Expected value have to be Literal or Variable but `${expected::class}` found"
        }

        assertDeepEquals(
            resultModel,
            expected,
            actual,
            depth = 0,
            visitedModels = hashSetOf()
        )
    }

    private fun recordActualResult() {
        currentExecution!!.result.onSuccess { result ->
            when (val executable = currentExecutable) {
                is ConstructorId -> {
                    // there is nothing to generate for constructors
                    return
                }
                is BuiltinMethodId -> error("Unexpected BuiltinMethodId $currentExecutable while generating actual result")
                is MethodId -> {
                    // TODO possible engine bug - void method return type and result not UtVoidModel
                    if (result.isUnit() || executable.returnType == voidClassId) return

                    emptyLineIfNeeded()

                    actual = newVar(
                        CgClassId(result.classId, isNullable = result is UtNullModel),
                        "actual"
                    ) {
                        thisInstance[executable](*methodArguments.toTypedArray())
                    }
                }
                else -> {} // TODO: check this specific case
            }
        }
    }

    fun createTestMethod(executableId: ExecutableId, execution: UtExecution): CgTestMethod =
        withTestMethodScope(execution) {
            val testMethodName = nameGenerator.testMethodNameFor(executableId, execution.testMethodName)
            // TODO: remove this line when SAT-1273 is completed
            execution.displayName = execution.displayName?.let { "${executableId.name}: $it" }
            testMethod(testMethodName, execution.displayName) {
                val statics = currentExecution!!.stateBefore.statics
                rememberInitialStaticFields(statics)
                val stateAnalyzer = ExecutionStateAnalyzer(execution)
                val modificationInfo = stateAnalyzer.findModifiedFields()
                // TODO: move such methods to another class and leave only 2 public methods: remember initial and final states
                val mainBody = {
                    substituteStaticFields(statics)
                    setupInstrumentation()
                    // build this instance
                    thisInstance = execution.stateBefore.thisInstance?.let {
                        variableConstructor.getOrCreateVariable(it)
                    }
                    // build arguments
                    for ((index, param) in execution.stateBefore.parameters.withIndex()) {
                        val name = paramNames[executableId]?.get(index)
                        methodArguments += variableConstructor.getOrCreateVariable(param, name)
                    }
                    rememberInitialEnvironmentState(modificationInfo)
                    recordActualResult()
                    generateResultAssertions()
                    rememberFinalEnvironmentState(modificationInfo)
                    generateFieldStateAssertions()
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

                mockFrameworkManager.getAndClearMethodResources()?.let { resources ->
                    val closeFinallyBlock = resources.map {
                        val variable = it.variable
                        variable.type.closeMethodIdOrNull?.let { closeMethod ->
                            CgMethodCall(variable, closeMethod, arguments = emptyList()).toStatement()
                        } ?: error("Resource $variable was expected to be auto closeable but it is not")
                    }

                    val tryWithMocksFinallyClosing = CgTryCatch(currentBlock, handlers = emptyList(), closeFinallyBlock)
                    currentBlock = currentBlock.clear()
                    resources.forEach {
                        // First argument for mocked resource declaration initializer is a target type.
                        // Pass this argument as a type parameter for the mocked resource
                        val typeParameter = when (val firstArg = (it.initializer as CgMethodCall).arguments.first()) {
                            is CgGetJavaClass -> firstArg.classId
                            is CgVariable -> firstArg.type
                            else -> error("Unexpected mocked resource declaration argument $firstArg")
                        }
                        val varType = CgClassId(
                            it.variableType,
                            TypeParameters(listOf(typeParameter)),
                            isNullable = true,
                        )
                        +CgDeclaration(
                            varType,
                            it.variableName,
                            // guard initializer to reuse typecast creation logic
                            initializer = guardExpression(varType, nullLiteral()).expression,
                            isMutable = true,
                        )
                    }
                    +tryWithMocksFinallyClosing
                }
            }
        }

    private val expectedResultVarName = "expectedResult"
    private val expectedErrorVarName = "expectedError"

    fun createParameterizedTestMethod(testSet: CgMethodTestSet, dataProviderMethodName: String): CgTestMethod {
        //TODO: orientation on generic execution may be misleading, but what is the alternative?
        //may be a heuristic to select a model with minimal number of internal nulls should be used
        val genericExecution = chooseGenericExecution(testSet.executions)

        val statics = genericExecution.stateBefore.statics

        return withTestMethodScope(genericExecution) {
            val testName = nameGenerator.parameterizedTestMethodName(dataProviderMethodName)
            withNameScope {
                val testParameterDeclarations = createParameterDeclarations(testSet, genericExecution)

                methodType = PARAMETRIZED
                testMethod(
                    testName,
                    displayName = null,
                    testParameterDeclarations,
                    parameterized = true,
                    dataProviderMethodName
                ) {
                    rememberInitialStaticFields(statics)
                    substituteStaticFields(statics, isParametrized = true)

                    // build this instance
                    thisInstance = genericExecution.stateBefore.thisInstance?.let { currentMethodParameters[CgParameterKind.ThisInstance] }

                    // build arguments for method under test and parameterized test
                    for (index in genericExecution.stateBefore.parameters.indices) {
                        methodArguments += currentMethodParameters[CgParameterKind.Argument(index)]!!
                    }

                    if (containsFailureExecution(testSet) || statics.isNotEmpty()) {
                        var currentTryBlock = tryBlock {
                            recordActualResult()
                            generateAssertionsForParameterizedTest()
                        }

                        if (containsFailureExecution(testSet)) {
                            val expectedErrorVariable = currentMethodParameters[CgParameterKind.ExpectedException]
                                ?: error("Test set $testSet contains failure execution, but test method signature has no error parameter")
                            currentTryBlock =
                                if (containsReflectiveCall) {
                                    currentTryBlock.catch(InvocationTargetException::class.java.id) { exception ->
                                        testFrameworkManager.assertBoolean(
                                            expectedErrorVariable.isInstance(exception[getTargetException]())
                                        )
                                    }
                                } else {
                                    currentTryBlock.catch(Throwable::class.java.id) { throwable ->
                                        testFrameworkManager.assertBoolean(
                                            expectedErrorVariable.isInstance(throwable)
                                        )
                                    }
                                }
                        }

                        if (statics.isNotEmpty()) {
                            currentTryBlock = currentTryBlock.finally {
                                recoverStaticFields()
                            }
                        }
                        +currentTryBlock
                    } else {
                        recordActualResult()
                        generateAssertionsForParameterizedTest()
                    }
                }
            }
        }
    }

    private fun chooseGenericExecution(executions: List<UtExecution>): UtExecution {
        return executions
            .firstOrNull { it.result is UtExecutionSuccess && (it.result as UtExecutionSuccess).model !is UtNullModel }
            ?: executions
                .firstOrNull { it.result is UtExecutionSuccess } ?: executions.first()
    }

    private fun createParameterDeclarations(
        testSet: CgMethodTestSet,
        genericExecution: UtExecution,
    ): List<CgParameterDeclaration> {
        val executableUnderTest = testSet.executableId
        val executableUnderTestParameters = testSet.executableId.executable.parameters

        return mutableListOf<CgParameterDeclaration>().apply {
            // this instance
            val thisInstanceModel = genericExecution.stateBefore.thisInstance
            if (thisInstanceModel != null) {
                val type = wrapTypeIfRequired(thisInstanceModel.classId)
                val thisInstance = CgParameterDeclaration(
                    parameter = declareParameter(
                        type = type,
                        name = nameGenerator.variableName(type)
                    ),
                    isReferenceType = true
                )
                this += thisInstance
                currentMethodParameters[CgParameterKind.ThisInstance] = thisInstance.parameter
            }
            // arguments
            for (index in genericExecution.stateBefore.parameters.indices) {
                val argumentName = paramNames[executableUnderTest]?.get(index)
                val paramType = executableUnderTestParameters[index].parameterizedType

                val argumentType = when {
                    paramType is Class<*> && paramType.isArray -> paramType.id
                    paramType is ParameterizedType -> paramType.id
                    else -> ClassId(paramType.typeName)
                }

                val argument = CgParameterDeclaration(
                    parameter = declareParameter(
                        type = argumentType,
                        name = nameGenerator.variableName(argumentType, argumentName),
                    ),
                    isReferenceType = argumentType.isRefType
                )
                this += argument
                currentMethodParameters[CgParameterKind.Argument(index)] = argument.parameter
            }

            val statics = genericExecution.stateBefore.statics
            if (statics.isNotEmpty()) {
                for ((fieldId, model) in statics) {
                    val staticType = wrapTypeIfRequired(model.classId)
                    val static = CgParameterDeclaration(
                        parameter = declareParameter(
                            type = staticType,
                            name = nameGenerator.variableName(fieldId.name, isStatic = true)
                        ),
                        isReferenceType = staticType.isRefType
                    )
                    this += static
                    currentMethodParameters[CgParameterKind.Statics(model)] = static.parameter
                }
            }

            val expectedResultClassId = wrapTypeIfRequired(testSet.resultType())
            if (expectedResultClassId != voidClassId) {
                val wrappedType = wrapIfPrimitive(expectedResultClassId)
                //We are required to wrap the type of expected result if it is primitive
                //to support nulls for throwing exceptions executions.
                val expectedResult = CgParameterDeclaration(
                    parameter = declareParameter(
                        type = wrappedType,
                        name = nameGenerator.variableName(expectedResultVarName)
                    ),
                    isReferenceType = wrappedType.isRefType
                )
                this += expectedResult
                currentMethodParameters[CgParameterKind.ExpectedResult] = expectedResult.parameter
            }

            val containsFailureExecution = containsFailureExecution(testSet)
            if (containsFailureExecution) {
                val classClassId = Class::class.id
                val expectedException = CgParameterDeclaration(
                    parameter = declareParameter(
                        type = BuiltinClassId(
                            name = classClassId.name,
                            simpleName = classClassId.simpleName,
                            canonicalName = classClassId.canonicalName,
                            packageName = classClassId.packageName,
                            typeParameters = TypeParameters(listOf(Throwable::class.java.id))
                        ),
                        name = nameGenerator.variableName(expectedErrorVarName)
                    ),
                    // exceptions are always reference type
                    isReferenceType = true,
                )
                this += expectedException
                currentMethodParameters[CgParameterKind.ExpectedException] = expectedException.parameter
            }
        }
    }

    /**
     * Constructs data provider method for parameterized tests.
     *
     * The body of this method is constructed manually, statement by statement.
     * Standard logic for generating each test case parameter code is used.
     */
    fun createParameterizedTestDataProvider(
        testSet: CgMethodTestSet,
        dataProviderMethodName: String
    ): CgParameterizedTestDataProviderMethod {
        return withDataProviderScope {
            dataProviderMethod(dataProviderMethodName) {
                val argListLength = testSet.executions.size
                val argListVariable = testFrameworkManager.createArgList(argListLength)

                emptyLine()

                for ((execIndex, execution) in testSet.executions.withIndex()) {
                    // create a block for current test case
                    innerBlock {
                        val arguments = createExecutionArguments(testSet, execution)
                        createArgumentsCallRepresentation(execIndex, argListVariable, arguments)
                    }
                }

                emptyLineIfNeeded()

                returnStatement { argListVariable }
            }
        }
    }

    private fun createExecutionArguments(testSet: CgMethodTestSet, execution: UtExecution): List<CgExpression> {
        val arguments = mutableListOf<CgExpression>()
        execution.stateBefore.thisInstance?.let {
            arguments += variableConstructor.getOrCreateVariable(it)
        }

        for ((paramIndex, paramModel) in execution.stateBefore.parameters.withIndex()) {
            val argumentName = paramNames[testSet.executableId]?.get(paramIndex)
            arguments += variableConstructor.getOrCreateVariable(paramModel, argumentName)
        }

        val statics = execution.stateBefore.statics
        for ((field, model) in statics) {
            arguments += variableConstructor.getOrCreateVariable(model, field.name)
        }


        val method = currentExecutable!!
        val needsReturnValue = method.returnType != voidClassId
        val containsFailureExecution = containsFailureExecution(testSet)
        execution.result
            .onSuccess {
                if (needsReturnValue) {
                    arguments += variableConstructor.getOrCreateVariable(it)
                }
                if (containsFailureExecution) {
                    arguments += nullLiteral()
                }
            }
            .onFailure {
                if (needsReturnValue) {
                    arguments += nullLiteral()
                }
                if (containsFailureExecution) {
                    arguments += CgGetJavaClass(it::class.id)
                }
            }

        emptyLineIfNeeded()

        return arguments
    }

    private fun <R> withTestMethodScope(execution: UtExecution, block: () -> R): R {
        clearTestMethodScope()
        currentExecution = execution
        statesCache = EnvironmentFieldStateCache.emptyCacheFor(execution)
        return try {
            block()
        } finally {
            clearTestMethodScope()
        }
    }

    private fun <R> withDataProviderScope(block: () -> R): R {
        clearMethodScope()
        return try {
            block()
        } finally {
            clearMethodScope()
        }
    }

    /**
     * This function makes sure some information about the method currently being generated is empty.
     * It clears only the information that is relevant to all kinds of methods:
     * - test methods
     * - data provider methods
     * - and any other kinds of methods that may be added in the future
     */
    private fun clearMethodScope() {
        collectedExceptions.clear()
        collectedMethodAnnotations.clear()
    }

    /**
     * This function makes sure some information about the **test method** currently being generated is empty.
     * It is used at the start of test method generation and right after it.
     */
    private fun clearTestMethodScope() {
        clearMethodScope()
        prevStaticFieldValues.clear()
        thisInstance = null
        methodArguments.clear()
        currentExecution = null
        containsReflectiveCall = false
        mockFrameworkManager.clearExecutionResources()
        currentMethodParameters.clear()
    }

    /**
     * Generates a collection of [CgStatement] to prepare arguments
     * for current execution in parameterized tests.
     */
    private fun createArgumentsCallRepresentation(
        executionIndex: Int,
        argsVariable: CgVariable,
        arguments: List<CgExpression>,
    ) {
        val argsArray = newVar(objectArrayClassId, "testCaseObjects") {
            CgAllocateArray(objectArrayClassId, objectClassId, arguments.size)
        }
        for ((i, argument) in arguments.withIndex()) {
            setArgumentsArrayElement(argsArray, i, argument, this)
        }
        testFrameworkManager.passArgumentsToArgsVariable(argsVariable, argsArray, executionIndex)
    }

    private fun containsFailureExecution(testSet: CgMethodTestSet) =
        testSet.executions.any { it.result is UtExecutionFailure }


    private fun testMethod(
        methodName: String,
        displayName: String?,
        params: List<CgParameterDeclaration> = emptyList(),
        parameterized: Boolean = false,
        dataProviderMethodName: String? = null,
        body: () -> Unit,
    ): CgTestMethod {
        collectedMethodAnnotations += if (parameterized) {
            testFrameworkManager.collectParameterizedTestAnnotations(dataProviderMethodName)
        } else {
            setOf(annotation(testFramework.testAnnotationId))
        }

        displayName?.let {
            testFrameworkManager.addTestDescription(displayName)
        }

        val result = currentExecution!!.result
        if (result is UtTimeoutException) {
            testFrameworkManager.setTestExecutionTimeout(hangingTestsTimeout.timeoutMs)
        }

        if (result is UtTimeoutException && !enableTestsTimeout) {
            testFrameworkManager.disableTestMethod(
                "Disabled due to failing by exceeding the timeout"
            )
        }

        if (result is UtConcreteExecutionFailure) {
            testFrameworkManager.disableTestMethod(
                "Disabled due to possible JVM crash"
            )
        }

        if (result is UtSandboxFailure) {
            testFrameworkManager.disableTestMethod(
                "Disabled due to sandbox"
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
            methodType = this@CgMethodConstructor.methodType
            val docComment = currentExecution?.summary?.map { convertDocToCg(it) }?.toMutableList() ?: mutableListOf()

            // add JVM crash report path if exists
            if (result is UtConcreteExecutionFailure) {
                result.extractJvmReportPathOrNull()?.let {
                    val jvmReportDocumentation = CgDocRegularStmt(getJvmReportDocumentation(it))
                    val lastTag = docComment.lastOrNull()
                    // if the last statement is a <pre> tag, put the path inside it
                    if (lastTag == null || lastTag !is CgDocPreTagStatement) {
                        docComment += jvmReportDocumentation
                    } else {
                        val tagContent = lastTag.content
                        docComment.removeLast()
                        docComment += CgDocPreTagStatement(tagContent + jvmReportDocumentation)
                    }
                }
            }

            documentation = CgDocumentationComment(docComment)
            documentation = if (parameterized) {
                CgDocumentationComment(text = null)
            } else {
                CgDocumentationComment(docComment)
            }
        }
        testMethods += testMethod
        return testMethod
    }

    private fun dataProviderMethod(dataProviderMethodName: String, body: () -> Unit): CgParameterizedTestDataProviderMethod {
        return buildParameterizedTestDataProviderMethod {
            name = dataProviderMethodName
            returnType = testFramework.argListClassId
            statements = block(body)
            // Exceptions and annotations assignment must run after the statements block is build,
            // because we collect info about exceptions and required annotations while building the statements
            exceptions += collectedExceptions
            annotations += testFrameworkManager.createDataProviderAnnotations(dataProviderMethodName)
        }
    }

    fun errorMethod(executable: ExecutableId, errors: Map<String, Int>): CgRegion<CgMethod> {
        val name = nameGenerator.errorMethodNameFor(executable)
        val body = block {
            comment("Couldn't generate some tests. List of errors:")
            comment()
            errors.entries.sortedByDescending { it.value }.forEach { (message, repeated) ->
                val multilineMessage = message
                    .split("\r") // split stacktrace from concrete if present
                    .flatMap { line ->
                        line
                            .split(" ")
                            .windowed(size = 10, step = 10, partialWindows = true) {
                                it.joinToString(separator = " ")
                            }
                    }
                comment("$repeated occurrences of:")

                if (multilineMessage.size <= 1) {
                    // wrap one liner with line comment
                    multilineMessage.singleOrNull()?.let { comment(it) }
                } else {
                    // wrap all lines with multiline comment
                    multilineComment(multilineMessage)
                }

                emptyLine()
            }
        }
        val errorTestMethod = CgErrorTestMethod(name, body)
        return CgSimpleRegion("Errors report for ${executable.name}", listOf(errorTestMethod))
    }

    private fun getJvmReportDocumentation(jvmReportPath: String): String {
        val pureJvmReportPath = jvmReportPath.substringAfter("# ")

        // \n is here because IntellijIdea cannot process other separators
        return PathUtil.toHtmlLinkTag(PathUtil.replaceSeparator(pureJvmReportPath), fileName = "JVM crash report") + "\n"
    }

    private fun UtConcreteExecutionFailure.extractJvmReportPathOrNull(): String? =
        exception.processStdout.singleOrNull {
            "hs_err_pid" in it
        }

    private fun CgExecutableCall.wrapReflectiveCall() {
        +tryBlock {
            +this@wrapReflectiveCall
        }.catch(InvocationTargetException::class.id) { e ->
            throwStatement {
                e[getTargetException]()
            }
        }
    }

    /**
     * Intercept calls to [java.lang.reflect.Method.invoke] and to [java.lang.reflect.Constructor.newInstance]
     * in order to wrap these calls in a try-catch block that will handle [InvocationTargetException]
     * that may be thrown by these calls.
     */
    private fun CgExecutableCall.intercepted() {
        val executableToWrap = when (executableId) {
            is MethodId -> invoke
            is ConstructorId -> newInstance
        }
        if (executableId == executableToWrap) {
            this.wrapReflectiveCall()
        } else {
            +this
        }
    }
}