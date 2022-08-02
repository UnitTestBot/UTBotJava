package org.utbot.framework.codegen.model.constructor.tree

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.runBlocking
import org.utbot.common.PathUtil
import org.utbot.engine.isStatic
import org.utbot.framework.assemble.assemble
import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour.PASS
import org.utbot.framework.codegen.model.constructor.builtin.*
import org.utbot.framework.codegen.model.constructor.builtin.invoke
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.*
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.tree.CgTestMethodType.*
import org.utbot.framework.codegen.model.util.*
import org.utbot.framework.fields.ExecutionStateAnalyzer
import org.utbot.framework.fields.FieldPath
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.util.isUnit
import org.utbot.jcdb.api.*
import org.utbot.jcdb.api.ext.findClass
import org.utbot.summary.SummarySentenceConstants.TAB
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.InvocationTargetException
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.jvm.javaType

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

    private fun setupInstrumentation() {
        val instrumentation = currentExecution!!.instrumentation
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
    private fun rememberInitialStaticFields() {
        for ((field, _) in currentExecution!!.stateBefore.statics.accessibleFields()) {
            val declaringClass = field.classId
            val fieldAccessible = field.isAccessibleFrom(testClassPackageName)

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
                    testClassThisInstance[getStaticFieldValue](declaringClassVar, field.name)
                }
            }
            // remember the previous value of a static field to recover it at the end of the test
            prevStaticFieldValues[field] = prevValue
        }
    }

    private fun mockStaticFields() {
        for ((field, model) in currentExecution!!.stateBefore.statics.accessibleFields()) {
            val declaringClass = field.classId
            val fieldAccessible = field.canBeSetIn(testClassPackageName)
            val fieldValue = variableConstructor.getOrCreateVariable(model, field.name)
            if (fieldAccessible) {
                declaringClass[field] `=` fieldValue
            } else {
                val declaringClassVar = newVar(classCgClassId) {
                    Class::class.id[forName](declaringClass.name)
                }
                +testClassThisInstance[setStaticField](declaringClassVar, field.name, fieldValue)
            }
        }
    }

    private fun recoverStaticFields() {
        for ((field, prevValue) in prevStaticFieldValues.accessibleFields()) {
            if (field.canBeSetIn(testClassPackageName)) {
                field.classId[field] `=` prevValue
            } else {
                val declaringClass = getClassOf(field.classId)
                +testClassThisInstance[setStaticField](declaringClass, field.name, prevValue)
            }
        }
    }

    private fun <E> Map<FieldId, E>.accessibleFields(): Map<FieldId, E> = filterKeys { !it.isInaccessible }

    /**
     * @return expression for [java.lang.Class] of the given [classId]
     */
    // TODO: move this method somewhere, because now it duplicates the identical method from MockFrameworkManager
    private fun getClassOf(classId: ClassId): CgExpression =
        if (classId isAccessibleFrom testClassPackageName) {
            CgGetJavaClass(classId)
        } else {
            newVar(classCgClassId) { Class::class.id[forName](classId.name) }
        }

    /**
     * Generates result assertions for unit tests.
     */
    private fun generateResultAssertions() {
        when (val executable = currentExecutable) {
            is ConstructorExecutableId -> {
                // we cannot generate any assertions for constructor testing
                // but we need to generate a constructor call
                val currentExecution = currentExecution!!
                currentExecution.result
                    .onSuccess {
                        methodType = SUCCESSFUL

                        // TODO engine returns UtCompositeModel sometimes (concrete execution?)

                        // TODO support inner classes constructors testing JIRA:1461
                        require(!executable.classId.isInner) {
                            "Inner class ${executable.classId} constructor testing is not supported yet"
                        }

                        actual = newVar(executable.classId, "actual") {
                            methodArguments.toTypedArray()
                            executable(*methodArguments.toTypedArray())
                        }
                    }
                    .onFailure { exception ->
                        processExecutionFailure(currentExecution, exception)
                    }
            }
            is MethodExecutableId -> {
                val method = executable.methodId
                if (method is BuiltinMethodId) {
                    error("Unexpected BuiltinMethodId $currentExecutable while generating result assertions")
                }
                emptyLineIfNeeded()

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
        }
    }

    private fun processExecutionFailure(execution: UtExecution, exception: Throwable) {
        val methodInvocationBlock = {
            with(currentExecutable) {
                when (this) {
                    is MethodExecutableId -> thisInstance[this](*methodArguments.toTypedArray()).intercepted()
                    is ConstructorExecutableId -> this(*methodArguments.toTypedArray()).intercepted()
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
            else -> {
                methodType = FAILING
                writeWarningAboutFailureTest(exception)
            }
        }

        methodInvocationBlock()
    }

    private fun shouldTestPassWithException(execution: UtExecution, exception: Throwable): Boolean {
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
            "This test fails because method [$executableName] produces [$exception]"
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

    private fun writeWarningAboutCrash() {
        +CgSingleLineComment("This invocation possibly crashes JVM")
    }

    /**
     * Generates result assertions in parameterized tests for successful executions
     * and just runs the method if all executions are unsuccessful.
     */
    private fun generateAssertionsForParameterizedTest() {
        emptyLineIfNeeded()
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
                    println()
                }
            }
            .onFailure { thisInstance[method](*methodArguments.toTypedArray()).intercepted() }
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
        statements: MutableList<CgStatement>,
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
                statements += CgSingleLineComment("Current deep equals depth exceeds max depth $DEEP_EQUALS_MAX_DEPTH")
                statements += getDeepEqualsAssertion(expected, actual).toStatement()
                return
            }

            when (expectedModel) {
                is UtPrimitiveModel -> {
                    statements += when {
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
                            when (parameterizedTestSource) {
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
                            require(expected.type.isPrimitive || expected.type == stringClassId) {
                                "Expected primitive or String but got ${expected.type}"
                            }
                            assertions[assertEquals](expected, actual)
                        }
                    }.toStatement()
                }
                is UtEnumConstantModel -> {
                    statements += assertions[assertEquals](
                        expected,
                        actual
                    ).toStatement()
                }
                is UtClassRefModel -> {
                    // TODO this stuff is needed because Kotlin has javaclass property instead of Java getClass method
                    //  probably it is better to change getClass method behaviour in the future
                    val actualObject: CgVariable
                    if (codegenLanguage == CodegenLanguage.KOTLIN) {
                        val actualCastedToObject = CgDeclaration(
                            objectClassId,
                            variableConstructor.constructVarName("actualObject"),
                            CgTypeCast(objectClassId, actual)
                        )
                        statements += actualCastedToObject
                        actualObject = actualCastedToObject.variable
                    } else {
                        actualObject = actual
                    }

                    statements += assertions[assertEquals](
                        CgGetJavaClass(expected.type),
                        actualObject[getClass]()
                    ).toStatement()
                }
                is UtNullModel -> {
                    statements += assertions[assertNull](actual).toStatement()
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

                        addArraysLengthAssertion(expected, actual, statements)
                        statements += getDeepEqualsAssertion(expected, actual).toStatement()
                        return
                    }

                    // It does not work for Double and Float because JUnit does not have equals overloading with wrappers
                    if (nestedElementClassId == floatClassId || nestedElementClassId == doubleClassId) {
                        floatingPointArraysDeepEquals(arrayInfo, expected, actual, statements)
                        return
                    }

                    // common primitive array, can use default array equals
                    addArraysLengthAssertion(expected, actual, statements)
                    statements += getArrayEqualsAssertion(
                        expectedModel.classId,
                        typeCast(expectedModel.classId, expected, isSafetyCast = true),
                        typeCast(expectedModel.classId, actual, isSafetyCast = true)
                    ).toStatement()
                }
                is UtAssembleModel -> {
                    if (expectedModel.classId.isPrimitiveWrapper) {
                        statements += assertions[assertEquals](expected, actual).toStatement()
                        return
                    }

                    // UtCompositeModel deep equals is much more easier and human friendly
                    expectedModel.origin?.let {
                        assertDeepEquals(it, expected, actual, statements, depth, visitedModels)
                        return
                    }

                    // special case for strings as they are constructed from UtAssembleModel but can be compared with equals
                    if (expectedModel.classId == stringClassId) {
                        statements += assertions[assertEquals](
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
                        statements += getDeepEqualsAssertion(expected, actual).toStatement()
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
                            statements,
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
                        statements += CgSingleLineComment(
                            "${expected.type.name} is iterable or Map, use outer deep equals to iterate over"
                        )
                        statements += getDeepEqualsAssertion(expected, actual).toStatement()

                        return
                    }

                    if (expected.hasNotParametrizedCustomEquals()) {
                        // We rely on already existing equals
                        statements += CgSingleLineComment("${expected.type.name} has overridden equals method")
                        statements += assertions[assertEquals](expected, actual).toStatement()

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
                            statements,
                            depth,
                            visitedModels
                        )
                    }
                }
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
        statements: MutableList<CgStatement>
    ): CgDeclaration {
        val cgGetLengthDeclaration = CgDeclaration(
            intClassId,
            variableConstructor.constructVarName("${expected.name}Size"),
            expected.length(this, testClassThisInstance, getArrayLength)
        )
        statements += cgGetLengthDeclaration
        statements += assertions[assertEquals](
            cgGetLengthDeclaration.variable,
            actual.length(this, testClassThisInstance, getArrayLength)
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
        statements: MutableList<CgStatement>,
    ) {
        val cgGetLengthDeclaration = addArraysLengthAssertion(expected, actual, statements)

        val nestedElementClassId = expectedArrayInfo.nestedElementClassId
            ?: error("Expected from floating point array ${expectedArrayInfo.classId} to contain elements but null found")
        require(nestedElementClassId == floatClassId || nestedElementClassId == doubleClassId) {
            "Expected float or double ClassId but `$nestedElementClassId` found"
        }

        if (expectedArrayInfo.isSingleDimensionalArray) {
            // we can use array equals for all single dimensional arrays
            statements += when (nestedElementClassId) {
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
            val loop = buildForLoop {
                val (i, init) = variableConstructor.loopInitialization(intClassId, "i", initializer = 0)
                initialization = init
                condition = i lessThan cgGetLengthDeclaration.variable.resolve()
                update = i.inc()

                val loopStatements = mutableListOf<CgStatement>()
                val expectedNestedElement = CgDeclaration(
                    expected.type.ifArrayGetElementClass()!!,
                    variableConstructor.constructVarName("${expected.name}NestedElement"),
                    CgArrayElementAccess(expected, i)
                )
                val actualNestedElement = CgDeclaration(
                    actual.type.ifArrayGetElementClass()!!,
                    variableConstructor.constructVarName("${actual.name}NestedElement"),
                    CgArrayElementAccess(actual, i)
                )

                loopStatements += expectedNestedElement
                loopStatements += actualNestedElement
                loopStatements += CgEmptyLine()

                val nullBranchStatements = listOf<CgStatement>(
                    assertions[assertNull](actualNestedElement.variable).toStatement()
                )

                val notNullBranchStatements = mutableListOf<CgStatement>()
                floatingPointArraysDeepEquals(
                    expectedArrayInfo.getNested(),
                    expectedNestedElement.variable,
                    actualNestedElement.variable,
                    notNullBranchStatements
                )

                loopStatements += CgIfStatement(
                    CgEqualTo(expectedNestedElement.variable, nullLiteral()),
                    nullBranchStatements,
                    notNullBranchStatements
                )


                this@buildForLoop.statements = loopStatements
            }

            statements += loop
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
    private fun CgVariable.hasNotParametrizedCustomEquals(): Boolean = runBlocking {
        if (type.overridesEquals()) {
            // type parameters is list of class type parameters - empty if class is not generic
            type.resolution() == Raw
        } else {
            false
        }
    }

    /**
     * We can't use [emptyLineIfNeeded] from here because it changes field [currentBlock].
     * Also, we can't extract generic part because [currentBlock] is PersistentList (not mutable).
     */
    private fun MutableList<CgStatement>.addEmptyLineIfNeeded() {
        val lastStatement = lastOrNull() ?: return
        if (lastStatement is CgEmptyLine) return

        this += CgEmptyLine()
    }

    private fun traverseFieldRecursively(
        fieldId: FieldId,
        fieldModel: UtModel,
        expected: CgVariable,
        actual: CgVariable,
        statements: MutableList<CgStatement>,
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
            return
        }

        // fieldModel is not visited and will be marked in assertDeepEquals call
        val fieldName = fieldId.name
        var expectedVariable: CgVariable? = null

        if (needExpectedDeclaration(fieldModel)) {
            val expectedFieldDeclaration = createDeclarationForFieldFromVariable(fieldId, expected, fieldName)

            statements += expectedFieldDeclaration
            expectedVariable = expectedFieldDeclaration.variable
        }

        val actualFieldDeclaration = createDeclarationForFieldFromVariable(fieldId, actual, fieldName)
        statements += actualFieldDeclaration

        assertDeepEquals(
            fieldModel,
            expectedVariable,
            actualFieldDeclaration.variable,
            statements,
            depth + 1,
            visitedModels,
        )
        statements.addEmptyLineIfNeeded()
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
        if (variable.type.hasField(name) && isAccessibleFrom(testClassPackageName)) {
            if (isStatic) CgStaticFieldAccess(this) else CgFieldAccess(variable, this)
        } else {
            testClassThisInstance[getFieldValue](variable, stringLiteral(name))
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

        val nestedElementClassIdList =
            generateSequence(classId.ifArrayGetElementClass()) { it.ifArrayGetElementClass() }.toList()
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
                when (expected.type.ifArrayGetElementClass()!!) {
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
                    when (parameterizedTestSource) {
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

    /**
     * We can't use standard deepEquals method in parametrized tests
     * because nullable objects require different asserts.
     * See https://github.com/UnitTestBot/UTBotJava/issues/252 for more details.
     */
    private fun generateDeepEqualsOrNullAssertion(
        expected: CgValue,
        actual: CgVariable,
    ) {
        when (parameterizedTestSource) {
            ParametrizedTestSource.DO_NOT_PARAMETRIZE ->
                currentBlock = currentBlock.addAll(generateDeepEqualsAssertion(expected, actual))
            ParametrizedTestSource.PARAMETRIZE -> {
                currentBlock = if (actual.type.isPrimitive) {
                    currentBlock.addAll(generateDeepEqualsAssertion(expected, actual))
                } else {
                    val assertNullStmt =
                        listOf(testFrameworkManager.assertions[testFramework.assertNull](actual).toStatement())
                    currentBlock.add(
                        CgIfStatement(
                            CgEqualTo(expected, nullLiteral()),
                            assertNullStmt,
                            generateDeepEqualsAssertion(expected, actual)
                        )
                    )
                }
            }
        }
    }

    private fun generateDeepEqualsAssertion(
        expected: CgValue,
        actual: CgVariable,
    ): List<CgStatement> {
        require(expected is CgVariable) {
            "Expected value have to be Literal or Variable but `${expected::class}` found"
        }

        val statements = mutableListOf<CgStatement>(CgEmptyLine())
        assertDeepEquals(
            resultModel,
            expected,
            actual,
            statements,
            depth = 0,
            visitedModels = hashSetOf()
        )

        return statements.dropLastWhile { it is CgEmptyLine }
    }

    private fun recordActualResult() {
        currentExecution!!.result.onSuccess { result ->
            when (val executable = currentExecutable) {
                is ConstructorExecutableId -> {
                    // there is nothing to generate for constructors
                    return
                }
                is MethodExecutableId -> {
                    if (executable.methodId is BuiltinMethodId) {
                        error("Unexpected BuiltinMethodId $currentExecutable while generating actual result")
                    }

                    // TODO possible engine bug - void method return type and result not UtVoidModel
                    if (result.isUnit() || executable.returnType == voidClassId) return

                    emptyLineIfNeeded()

                    actual = newVar(
                        CgClassId(executable.returnType, isNullable = result is UtNullModel),
                        "actual"
                    ) {
                        thisInstance[executable](*methodArguments.toTypedArray())
                    }
                }
            }
        }
    }

    fun createTestMethod(utMethod: UtMethod<*>, execution: UtExecution): CgTestMethod =
        withTestMethodScope(execution) {
            val testMethodName = nameGenerator.testMethodNameFor(utMethod, execution.testMethodName)
            // TODO: remove this line when SAT-1273 is completed
            execution.displayName = execution.displayName?.let { "${utMethod.callable.name}: $it" }
            testMethod(testMethodName, execution.displayName) {
                rememberInitialStaticFields()
                val stateAnalyzer = ExecutionStateAnalyzer(execution)
                val modificationInfo = stateAnalyzer.findModifiedFields()
                // TODO: move such methods to another class and leave only 2 public methods: remember initial and final states
                val mainBody = {
                    mockStaticFields()
                    setupInstrumentation()
                    // build this instance
                    thisInstance = execution.stateBefore.thisInstance?.let {
                        variableConstructor.getOrCreateVariable(it)
                    }
                    // build arguments
                    for ((index, param) in execution.stateBefore.parameters.withIndex()) {
                        val name = paramNames[utMethod]?.get(index)
                        methodArguments += variableConstructor.getOrCreateVariable(param, name)
                    }
                    rememberInitialEnvironmentState(modificationInfo)
                    recordActualResult()
                    generateResultAssertions()
                    rememberFinalEnvironmentState(modificationInfo)
                    generateFieldStateAssertions()
                }

                val statics = currentExecution!!.stateBefore.statics
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
                            CgMethodCall(
                                variable,
                                closeMethod.asExecutableMethod(),
                                arguments = emptyList()
                            ).toStatement()
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

    fun createParameterizedTestMethod(testSet: UtMethodTestSet, dataProviderMethodName: String): CgTestMethod {
        //TODO: orientation on generic execution may be misleading, but what is the alternative?
        //may be a heuristic to select a model with minimal number of internal nulls should be used
        val genericExecution = testSet.executions
            .firstOrNull { it.result is UtExecutionSuccess && (it.result as UtExecutionSuccess).model !is UtNullModel }
            ?: testSet.executions.first()

        return withTestMethodScope(genericExecution) {
            val testName = nameGenerator.parameterizedTestMethodName(dataProviderMethodName)
            withNameScope {
                val testParameterDeclarations = createParameterDeclarations(testSet, genericExecution)
                val mainBody = {
                    // build this instance
                    thisInstance =
                        genericExecution.stateBefore.thisInstance?.let { currentMethodParameters[CgParameterKind.ThisInstance] }

                    // build arguments for method under test and parameterized test
                    for (index in genericExecution.stateBefore.parameters.indices) {
                        methodArguments += currentMethodParameters[CgParameterKind.Argument(index)]!!
                    }

                    //record result and generate result assertions
                    recordActualResult()
                    generateAssertionsForParameterizedTest()
                }

                methodType = PARAMETRIZED
                testMethod(
                    testName,
                    displayName = null,
                    testParameterDeclarations,
                    parameterized = true,
                    dataProviderMethodName
                ) {
                    if (containsFailureExecution(testSet)) {
                        +tryBlock(mainBody)
                            .catch(Throwable::class.java.id) { e ->
                                val pseudoExceptionVarName = when (codegenLanguage) {
                                    CodegenLanguage.JAVA -> "${expectedErrorVarName}.isInstance(${e.name.decapitalize()})"
                                    CodegenLanguage.KOTLIN -> "${expectedErrorVarName}!!.isInstance(${e.name.decapitalize()})"
                                }

                                testFrameworkManager.assertBoolean(CgVariable(pseudoExceptionVarName, booleanClassId))
                            }
                    } else {
                        mainBody()
                    }
                }
            }
        }
    }

    private fun createParameterDeclarations(
        testSet: UtMethodTestSet,
        genericExecution: UtExecution,
    ): List<CgParameterDeclaration> {
        val methodUnderTest = testSet.method
        val methodUnderTestParameters = testSet.method.callable.parameters

        return mutableListOf<CgParameterDeclaration>().apply {
            // this instance
            val thisInstanceModel = genericExecution.stateBefore.thisInstance
            if (thisInstanceModel != null) {
                val type = thisInstanceModel.classId
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
                val argumentName = paramNames[methodUnderTest]?.get(index)
                val paramIndex = if (methodUnderTest.isStatic) index else index + 1
                val paramType = methodUnderTestParameters[paramIndex].type.javaType

                val argumentType = when {
                    paramType is Class<*> && paramType.isArray -> paramType.id
                    paramType is ParameterizedTypeImpl -> paramType.rawType.id
                    else -> runBlocking { utContext.classpath.findClass(paramType.typeName) }
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

            val method = currentExecutable as MethodId
            val containsFailureExecution = containsFailureExecution(testSet)

            val expectedResultClassId = wrapTypeIfRequired(method.returnType)

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

            if (containsFailureExecution) {
                val expectedException = CgParameterDeclaration(
                    parameter = declareParameter(
                        type = throwableClassId(),
                        name = nameGenerator.variableName(expectedErrorVarName)
                    ),
                    // exceptions are always reference type
                    isReferenceType = true
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
        testSet: UtMethodTestSet,
        dataProviderMethodName: String
    ): CgParameterizedTestDataProviderMethod {
        val dataProviderStatements = mutableListOf<CgStatement>()
        val dataProviderExceptions = mutableSetOf<ClassId>()

        val argListLength = testSet.executions.size
        val argListDeclaration = createArgList(argListLength)
        val argListVariable = argListDeclaration.variable

        dataProviderStatements += argListDeclaration
        dataProviderStatements += CgEmptyLine()

        for ((execIndex, execution) in testSet.executions.withIndex()) {
            withTestMethodScope(execution) {
                //collect arguments
                val arguments = mutableListOf<CgExpression>()
                val executionArgumentsBody = {
                    execution.stateBefore.thisInstance?.let {
                        arguments += variableConstructor.getOrCreateVariable(it)
                    }

                    for ((paramIndex, paramModel) in execution.stateBefore.parameters.withIndex()) {
                        val argumentName = paramNames[testSet.method]?.get(paramIndex)
                        arguments += variableConstructor.getOrCreateVariable(paramModel, argumentName)
                    }

                    val method = currentExecutable as MethodId
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
                }

                //create a block for current test case
                dataProviderStatements += innerBlock(
                    {},
                    block(executionArgumentsBody)
                            + createArgumentsCallRepresentation(execIndex, argListVariable, arguments).toPersistentList()
                )

                dataProviderExceptions += collectedExceptions
            }
        }

        dataProviderStatements.addEmptyLineIfNeeded()
        dataProviderStatements += CgReturnStatement(argListVariable)

        return buildParameterizedTestDataProviderMethod {
            name = dataProviderMethodName
            returnType = argListClassId()
            statements = dataProviderStatements
            annotations = createDataProviderAnnotations(dataProviderMethodName)
            exceptions = dataProviderExceptions
        }
    }

    private fun <R> withTestMethodScope(execution: UtExecution, block: () -> R): R {
        clearMethodScope()
        currentExecution = execution
        statesCache = EnvironmentFieldStateCache.emptyCacheFor(execution)
        return try {
            block()
        } finally {
            clearMethodScope()
        }
    }

    private fun clearMethodScope() {
        collectedExceptions.clear()
        collectedTestMethodAnnotations.clear()
        prevStaticFieldValues.clear()
        thisInstance = null
        methodArguments.clear()
        currentExecution = null
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
    ): List<CgStatement> = when (testFramework) {
        Junit5 -> {
            val argumentsMethodCall = CgMethodCall(caller = null, argumentsMethodId().asExecutableMethod(), arguments)
            listOf(
                CgStatementExecutableCall(
                    CgMethodCall(
                        argsVariable,
                        addToListMethodId().asExecutableMethod(),
                        listOf(argumentsMethodCall)
                    )
                )
            )
        }
        TestNg -> {
            val statements = mutableListOf<CgStatement>()
            val argsArrayAllocation = CgAllocateArray(Array<Any?>::class.java.id, objectClassId, arguments.size)
            val argsArrayDeclaration = CgDeclaration(objectArrayClassId, "testCaseObjects", argsArrayAllocation)
            statements += argsArrayDeclaration
            for ((i, argument) in arguments.withIndex()) {
                statements += setArgumentsArrayElement(argsArrayDeclaration.variable, i, argument)
            }
            statements += setArgumentsArrayElement(argsVariable, executionIndex, argsArrayDeclaration.variable)

            statements
        }
        Junit4 -> error("Parameterized tests are not supported for JUnit4")
    }

    /**
     * Sets an element of arguments array in parameterized test,
     * if test framework represents arguments as array.
     */
    private fun setArgumentsArrayElement(array: CgVariable, index: Int, value: CgExpression): CgStatement =
        if (array.type == objectClassId) {
            java.lang.reflect.Array::class.id[setArrayElement](array, index, value)
        } else {
            CgAssignment(array.at(index), value)
        }

    /**
     * Creates annotations for data provider method in parameterized tests
     * depending on test framework.
     */
    private fun createDataProviderAnnotations(dataProviderMethodName: String?): MutableList<CgAnnotation> =
        when (testFramework) {
            Junit5 -> mutableListOf()
            TestNg -> mutableListOf(
                annotation(
                    testFramework.methodSourceAnnotationId,
                    listOf("name" to CgLiteral(stringClassId, dataProviderMethodName))
                ),
            )
            Junit4 -> error("Parameterized tests are not supported for JUnit4")
        }

    /**
     * Creates declaration of argList collection in parameterized tests.
     */
    private fun createArgList(length: Int): CgDeclaration = when (testFramework) {
        Junit5 -> {
            val constructorCall = CgConstructorCall(argListClassId().findConstructor().asExecutableConstructor(), emptyList())
            CgDeclaration(argListClassId(), "argList", constructorCall)
        }
        TestNg -> {
            val allocateArrayCall = CgAllocateArray(argListClassId(), Array<Any>::class.java.id, length)
            CgDeclaration(argListClassId(), "argList", allocateArrayCall)
        }
        Junit4 -> error("Parameterized tests are not supported for JUnit4")
    }

    /**
     * Creates a [ClassId] for arguments collection.
     */
    private fun argListClassId(): ClassId = when (testFramework) {
        Junit5 -> builtInClass("java.util.ArrayList<${JUNIT5_PARAMETERIZED_PACKAGE}.provider.Arguments>")
        TestNg -> builtInClass(Array<Array<Any?>?>::class.java.name)
//            BuiltinClassId(
//            name = Array<Array<Any?>?>::class.java.name,
//            simpleName = when (codegenLanguage) {
//                CodegenLanguage.JAVA -> "Object[][]"
//                CodegenLanguage.KOTLIN -> "Array<Array<Any?>?>"
//            },
//            canonicalName = Array<Array<Any?>?>::class.java.canonicalName,
//            packageName = Array<Array<Any?>?>::class.java.packageName,
//        )
        Junit4 -> error("Parameterized tests are not supported for JUnit4")
    }


    /**
     * A [MethodId] to add an item into [ArrayList].
     */
    private fun addToListMethodId(): MethodId = ArrayList::class.id.findMethod(
        name = "add",
        returnType = booleanClassId,
        arguments = listOf(Object::class.id),
    )

    /**
     * A [MethodId] to call JUnit Arguments method.
     */
    private fun argumentsMethodId(): MethodId {
        val argumentsClassId = builtInClass("org.junit.jupiter.params.provider.Arguments")

        return argumentsClassId.newBuiltinMethod(
            name = "arguments",
            returnType = argumentsClassId,
            arguments = listOf(Object::class.id),
        )
    }

    private fun containsFailureExecution(testSet: UtMethodTestSet) =
        testSet.executions.any { it.result is UtExecutionFailure }

    /**
     * A [ClassId] for Class<Throwable>.
     */
    private fun throwableClassId(): ClassId = builtInClass("java.lang.Class<Throwable>")

    private fun collectParameterizedTestAnnotations(dataProviderMethodName: String?): Set<CgAnnotation> =
        when (testFramework) {
            Junit5 -> setOf(
                annotation(testFramework.parameterizedTestAnnotationId),
                annotation(testFramework.methodSourceAnnotationId, dataProviderMethodName),
            )
            TestNg -> setOf(
                annotation(
                    testFramework.parameterizedTestAnnotationId,
                    listOf("dataProvider" to CgLiteral(stringClassId, dataProviderMethodName))
                ),
            )
            Junit4 -> error("Parameterized tests are not supported for JUnit4")
        }

    private fun testMethod(
        methodName: String,
        displayName: String?,
        params: List<CgParameterDeclaration> = emptyList(),
        parameterized: Boolean = false,
        dataProviderMethodName: String? = null,
        body: () -> Unit,
    ): CgTestMethod {
        collectedTestMethodAnnotations += if (parameterized) {
            collectParameterizedTestAnnotations(dataProviderMethodName)
        } else {
            setOf(annotation(testFramework.testAnnotationId))
        }
        displayName?.let { testFrameworkManager.addDisplayName(it) }

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

        val testMethod = buildTestMethod {
            name = methodName
            parameters = params
            statements = block(body)
            // Exceptions and annotations assignment must run after everything else is set up
            exceptions += collectedExceptions
            annotations += collectedTestMethodAnnotations
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

    fun errorMethod(method: UtMethod<*>, errors: Map<String, Int>): CgRegion<CgMethod> {
        val name = nameGenerator.errorMethodNameFor(method)
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
        return CgSimpleRegion("Errors report for ${method.callable.name}", listOf(errorTestMethod))
    }

    private fun getJvmReportDocumentation(jvmReportPath: String): String {
        val pureJvmReportPath = jvmReportPath.substringAfter("# ")

        // \n is here because IntellijIdea cannot process other separators
        return PathUtil.toHtmlLinkTag(
            PathUtil.replaceSeparator(pureJvmReportPath),
            fileName = "JVM crash report"
        ) + "\n"
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
            is MethodExecutableId -> invoke
            is ConstructorExecutableId -> newInstance
        }
        if (executableId == executableToWrap) {
            this.wrapReflectiveCall()
        } else {
            +this
        }
    }
}