package org.utbot.framework.codegen.tree

import mu.KotlinLogging
import org.utbot.common.WorkaroundReason
import org.utbot.common.isStatic
import org.utbot.common.workaround
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ArtificialError
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour.PASS
import org.utbot.framework.codegen.domain.builtin.closeMethodIdOrNull
import org.utbot.framework.codegen.domain.builtin.forName
import org.utbot.framework.codegen.domain.builtin.getClass
import org.utbot.framework.codegen.domain.builtin.getTargetException
import org.utbot.framework.codegen.domain.builtin.invoke
import org.utbot.framework.codegen.domain.builtin.newInstance
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.AnnotationTarget
import org.utbot.framework.codegen.domain.models.CgAllocateArray
import org.utbot.framework.codegen.domain.models.CgArrayElementAccess
import org.utbot.framework.codegen.domain.models.CgClassId
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgDocumentationComment
import org.utbot.framework.codegen.domain.models.CgEqualTo
import org.utbot.framework.codegen.domain.models.CgErrorTestMethod
import org.utbot.framework.codegen.domain.models.CgExecutableCall
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgFieldAccess
import org.utbot.framework.codegen.domain.models.CgGetJavaClass
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgMultilineComment
import org.utbot.framework.codegen.domain.models.CgNotNullAssertion
import org.utbot.framework.codegen.domain.models.CgParameterDeclaration
import org.utbot.framework.codegen.domain.models.CgParameterKind
import org.utbot.framework.codegen.domain.models.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.domain.models.CgRegion
import org.utbot.framework.codegen.domain.models.CgSimpleRegion
import org.utbot.framework.codegen.domain.models.CgSingleLineComment
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgStaticFieldAccess
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgTestMethodType
import org.utbot.framework.codegen.domain.models.CgTestMethodType.*
import org.utbot.framework.codegen.domain.models.CgTryCatch
import org.utbot.framework.codegen.domain.models.CgTypeCast
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.convertDocToCg
import org.utbot.framework.codegen.domain.models.toStatement
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.framework.TestFrameworkManager
import org.utbot.framework.codegen.tree.CgComponents.getCallableAccessManagerBy
import org.utbot.framework.codegen.tree.CgComponents.getCustomAssertConstructorBy
import org.utbot.framework.codegen.tree.CgComponents.getMockFrameworkManagerBy
import org.utbot.framework.codegen.tree.CgComponents.getNameGeneratorBy
import org.utbot.framework.codegen.tree.CgComponents.getStatementConstructorBy
import org.utbot.framework.codegen.tree.CgComponents.getTestFrameworkManagerBy
import org.utbot.framework.codegen.tree.CgComponents.getVariableConstructorBy
import org.utbot.framework.codegen.util.canBeReadFrom
import org.utbot.framework.codegen.util.canBeReadViaGetterFrom
import org.utbot.framework.codegen.util.canBeSetFrom
import org.utbot.framework.codegen.util.equalTo
import org.utbot.framework.codegen.util.escapeControlChars
import org.utbot.framework.codegen.util.getter
import org.utbot.framework.codegen.util.inc
import org.utbot.framework.codegen.util.length
import org.utbot.framework.codegen.util.lessThan
import org.utbot.framework.codegen.util.nullLiteral
import org.utbot.framework.codegen.util.resolve
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.InstrumentedProcessDeathException
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtConcreteExecutionFailure
import org.utbot.framework.plugin.api.UtCustomModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtModelWithCompositeOrigin
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.UtStreamConsumingFailure
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtTaintAnalysisFailure
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.isNotNull
import org.utbot.framework.plugin.api.isNull
import org.utbot.framework.plugin.api.onFailure
import org.utbot.framework.plugin.api.onSuccess
import org.utbot.framework.plugin.api.util.IndentUtil.TAB
import org.utbot.framework.plugin.api.util.allSuperTypes
import org.utbot.framework.plugin.api.util.baseStreamClassId
import org.utbot.framework.plugin.api.util.doubleArrayClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.doubleStreamClassId
import org.utbot.framework.plugin.api.util.doubleStreamToArrayMethodId
import org.utbot.framework.plugin.api.util.doubleWrapperClassId
import org.utbot.framework.plugin.api.util.executable
import org.utbot.framework.plugin.api.util.floatArrayClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.floatWrapperClassId
import org.utbot.framework.plugin.api.util.hasField
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.intStreamClassId
import org.utbot.framework.plugin.api.util.intStreamToArrayMethodId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isInaccessibleViaReflection
import org.utbot.framework.plugin.api.util.isInnerClassEnclosingClassReference
import org.utbot.framework.plugin.api.util.isIterableOrMap
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isPrimitiveArray
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.kClass
import org.utbot.framework.plugin.api.util.longStreamClassId
import org.utbot.framework.plugin.api.util.longStreamToArrayMethodId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.streamClassId
import org.utbot.framework.plugin.api.util.streamToArrayMethodId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.plugin.api.util.wrapIfPrimitive
import org.utbot.framework.util.isUnit
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType
import java.security.AccessControlException

private const val DEEP_EQUALS_MAX_DEPTH = 5 // TODO move it to plugin settings?

open class CgMethodConstructor(val context: CgContext) : CgContextOwner by context,
    CgCallableAccessManager by getCallableAccessManagerBy(context),
    CgStatementConstructor by getStatementConstructorBy(context) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    protected val nameGenerator = getNameGeneratorBy(context)
    protected val testFrameworkManager = getTestFrameworkManagerBy(context)

    protected val variableConstructor = getVariableConstructorBy(context)
    private val customAssertConstructor = getCustomAssertConstructorBy(context)
    private val mockFrameworkManager = getMockFrameworkManagerBy(context)

    private val floatDelta: Float = 1e-6f
    private val doubleDelta = 1e-6

    // a model for execution result (it is lateinit because execution can fail,
    // and we need it only on assertions generation stage
    lateinit var resultModel: UtModel

    lateinit var methodType: CgTestMethodType

    private val fieldsOfExecutionResults = mutableMapOf<Pair<FieldId, Int>, MutableList<UtModel>>()

    /**
     * Contains whether [UtStreamConsumingFailure] is in [CgMethodTestSet] for parametrized tests.
     * See [WorkaroundReason.CONSUME_DIRTY_STREAMS].
     */
    private var containsStreamConsumingFailureForParametrizedTests: Boolean = false

    protected fun setupInstrumentation() {
        val instrumentation = when (val execution = currentExecution) {
            is UtSymbolicExecution -> execution.instrumentation
            else -> return
        }
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
    protected fun rememberInitialStaticFields(statics: Map<FieldId, UtModel>) {
        val accessibleStaticFields = statics.accessibleFields()
        for ((field, _) in accessibleStaticFields) {
            val declaringClass = field.declaringClass
            val fieldAccessible = field.canBeReadFrom(context, declaringClass)

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

    protected fun substituteStaticFields(statics: Map<FieldId, UtModel>, isParametrized: Boolean = false) {
        val accessibleStaticFields = statics.accessibleFields()
        for ((field, model) in accessibleStaticFields) {
            val declaringClass = field.declaringClass
            val fieldAccessible = field.canBeSetFrom(context, declaringClass)

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

    protected fun recoverStaticFields() {
        for ((field, prevValue) in prevStaticFieldValues.accessibleFields()) {
            if (field.canBeSetFrom(context, field.declaringClass)) {
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
    protected open fun generateResultAssertions() {
        when (val executable = currentExecutableToCall) {
            is ConstructorId -> generateConstructorCall(executable, currentExecution!!)
            is BuiltinMethodId -> error("Unexpected BuiltinMethodId $executable while generating result assertions")
            is MethodId -> {
                emptyLineIfNeeded()
                val currentExecution = currentExecution!!
                val executionResult = currentExecution.result

                // build assertions
                executionResult
                    .onSuccess { resultModel ->
                        methodType = SUCCESSFUL

                        // TODO possible engine bug - void method return type and result model not UtVoidModel
                        if (resultModel.isUnit() || executable.returnType == voidClassId) {
                            +thisInstance[executable](*methodArguments.toTypedArray())
                        } else {
                            this.resultModel = resultModel
                            assertEquality(resultModel, actual, emptyLineIfNeeded = true)
                        }
                    }
                    .onFailure { exception -> processExecutionFailure(exception, executionResult) }
            }
            else -> {} // TODO: check this specific case
        }
    }

    private fun processExecutionFailure(exceptionFromAnalysis: Throwable, executionResult: UtExecutionResult) {
        val (methodInvocationBlock, expectedException) = constructExceptionProducingBlock(exceptionFromAnalysis, executionResult)

        when (methodType) {
            SUCCESSFUL -> error("Unexpected successful without exception method type for execution with exception $expectedException")
            PASSED_EXCEPTION -> {
                // TODO consider rendering message in a comment
                //  expectedException.message?.let { +comment(it.escapeControlChars()) }
                testFrameworkManager.expectException(expectedException::class.id) {
                    methodInvocationBlock()
                }
            }
            TIMEOUT -> {
                writeWarningAboutTimeoutExceeding()
                testFrameworkManager.expectTimeout(hangingTestsTimeout.timeoutMs) {
                    methodInvocationBlock()
                }
            }
            CRASH -> when (expectedException) {
                is InstrumentedProcessDeathException -> {
                    writeWarningAboutCrash()
                    methodInvocationBlock()
                }
                is AccessControlException -> {
                    // exception from sandbox
                    writeWarningAboutFailureTest(expectedException)
                }
                else -> error("Unexpected crash suite for failing execution with $expectedException exception")
            }
            ARTIFICIAL -> {
                methodInvocationBlock()

                val failureMessage = prepareArtificialFailureMessage(executionResult)
                testFrameworkManager.fail(failureMessage)
            }
            FAILING -> {
                writeWarningAboutFailureTest(expectedException)
                methodInvocationBlock()
            }
            PARAMETRIZED -> error("Unexpected $PARAMETRIZED method type for failing execution with $expectedException exception")
        }
    }

    // TODO: ISSUE-1546 introduce some kind of language-specific string interpolation in Codegen
    // and render arguments that cause overflow (but it requires engine enhancements)
    private fun prepareArtificialFailureMessage(executionResult: UtExecutionResult): CgLiteral {
        when (executionResult) {
            is UtOverflowFailure -> {
                val failureMessage = "Overflow detected in \'${currentExecutableToCall!!.name}\' call"
                return CgLiteral(stringClassId, failureMessage)
            }
            is UtTaintAnalysisFailure -> {
                return CgLiteral(stringClassId, executionResult.exception.message)
            }
            else -> error("$executionResult is not supported in artificial errors")
        }
    }

    private fun constructExceptionProducingBlock(
        exceptionFromAnalysis: Throwable,
        executionResult: UtExecutionResult
    ): Pair<() -> Unit, Throwable> {
        if (executionResult is UtStreamConsumingFailure) {
            return constructStreamConsumingBlock() to executionResult.rootCauseException
        }

        return {
            with(currentExecutableToCall) {
                when (this) {
                    is MethodId -> thisInstance[this](*methodArguments.toTypedArray()).intercepted()
                    is ConstructorId -> this(*methodArguments.toTypedArray()).intercepted()
                    else -> {} // TODO: check this specific case
                }
            }
        } to exceptionFromAnalysis
    }

    private fun constructStreamConsumingBlock(): () -> Unit {
        val executable = currentExecutableToCall

        require((executable is MethodId) && (executable.returnType isSubtypeOf baseStreamClassId)) {
            "Unexpected non-stream returning executable $executable"
        }

        val allSuperTypesOfReturn = executable.returnType.allSuperTypes().toSet()

        val streamConsumingMethodId = when {
            // The order is important since all streams implement BaseStream
            intStreamClassId in allSuperTypesOfReturn -> intStreamToArrayMethodId
            longStreamClassId in allSuperTypesOfReturn -> longStreamToArrayMethodId
            doubleStreamClassId in allSuperTypesOfReturn -> doubleStreamToArrayMethodId
            streamClassId in allSuperTypesOfReturn -> streamToArrayMethodId
            else -> {
                // BaseStream, use util method to consume it
                return { +utilsClassId[consumeBaseStream](actual) }
            }
        }

        return { +actual[streamConsumingMethodId]() }
    }

    protected open fun shouldTestPassWithException(execution: UtExecution, exception: Throwable): Boolean {
        if (exception is AccessControlException) return false
        // tests with timeout or crash should be processed differently
        if (exception is TimeoutException || exception is InstrumentedProcessDeathException) return false
        if (exception is ArtificialError) return false
        if (UtSettings.treatAssertAsErrorSuite && exception is AssertionError) return false

        val exceptionRequiresAssert = exception !is RuntimeException || runtimeExceptionTestsBehaviour == PASS
        val exceptionIsExplicit = execution.result is UtExplicitlyThrownException
        return exceptionRequiresAssert || exceptionIsExplicit
    }

    protected fun shouldTestPassWithTimeoutException(execution: UtExecution, exception: Throwable): Boolean {
        return execution.result is UtTimeoutException || exception is TimeoutException
    }

    protected fun writeWarningAboutTimeoutExceeding() {
        +CgMultilineComment(
            listOf(
                "This execution may take longer than the ${hangingTestsTimeout.timeoutMs} ms timeout",
                " and therefore fail due to exceeding the timeout."
            )
        )
    }

    protected fun writeWarningAboutFailureTest(exception: Throwable) {
        require(currentExecutableToCall is ExecutableId)
        val executableName = "${currentExecutableToCall!!.classId.name}.${currentExecutableToCall!!.name}"

        val warningLine = "This test fails because method [$executableName] produces [$exception]"
            .lines()
            .map { it.escapeControlChars() }
            .toMutableList()

        +CgMultilineComment(warningLine + collectNeededStackTraceLines(
            exception,
            executableToStartCollectingFrom = currentExecutableToCall!!
        ))
    }

    protected open fun collectNeededStackTraceLines(
        exception: Throwable,
        executableToStartCollectingFrom: ExecutableId
    ): List<String> {
        val executableName = "${executableToStartCollectingFrom.classId.name}.${executableToStartCollectingFrom.name}"

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
        if (!executableCallFound)
            logger.warn(exception) { "Failed to find executable call in stack trace" }

        return neededStackTraceLines.reversed()
    }

    protected fun writeWarningAboutCrash() {
        +CgSingleLineComment("This invocation possibly crashes JVM")
    }

    /**
     * Generates result assertions in parameterized tests for successful executions
     * and just runs the method if all executions are unsuccessful.
     */
    private fun generateAssertionsForParameterizedTest() {
        emptyLineIfNeeded()

        when (val executable = currentExecutableToCall) {
            is ConstructorId -> generateConstructorCall(executable, currentExecution!!)
            is MethodId -> {
                val executionResult = currentExecution!!.result

                executionResult
                    .onSuccess { resultModel ->
                        if (resultModel.isUnit()) {
                            +thisInstance[executable](*methodArguments.toTypedArray())
                        } else {
                            //"generic" expected variable is represented with a wrapper if
                            //actual result is primitive to support cases with exceptions.
                            this.resultModel = resultModel

                            val expectedVariable = currentMethodParameters[CgParameterKind.ExpectedResult]!!
                            val expectedExpression = CgNotNullAssertion(expectedVariable)

                            assertEquality(expectedExpression, actual)
                        }
                    }
                    .onFailure {
                        workaround(WorkaroundReason.CONSUME_DIRTY_STREAMS) {
                            if (containsStreamConsumingFailureForParametrizedTests) {
                                constructStreamConsumingBlock().invoke()
                            } else {
                                thisInstance[executable](*methodArguments.toTypedArray()).intercepted()
                            }
                        }
                    }
            }
            else -> {} // TODO: check this specific case
        }
    }

    /**
     * Generates assertions for field states.
     *
     * Note: not supported in parameterized tests.
     */
    protected fun generateFieldStateAssertions() {
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
    private fun assertStatesByPath(cache: FieldStateCache, path: Any) {
        emptyLineIfNeeded()
        val beforeVariable = cache.before[path]?.variable
        val (afterVariable, afterModel) = cache.after[path]!!

        // TODO: remove the following after the issue fix
        // We do not generate some assertions for Enums due to [https://github.com/UnitTestBot/UTBotJava/issues/1704].
        val beforeModel = cache.before[path]?.model
        if (beforeModel !is UtEnumConstantModel && afterModel is UtEnumConstantModel) {
            return
        }

        if (afterModel !is UtReferenceModel) {
            assertEquality(
                expected = afterModel,
                actual = afterVariable,
                expectedVariableName = "expected" + afterVariable.name.capitalize()
            )
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
        visitedModels: MutableSet<ModelWithField>,
        expectedModelField: FieldId? = null,
    ) {
        val modelWithField = ModelWithField(expectedModel, expectedModelField)
        if (modelWithField in visitedModels) return

        @Suppress("NAME_SHADOWING")
        var expected = expected
        if (expected == null) {
            require(!needExpectedDeclaration(expectedModel))
            expected = actual
        }

        visitedModels += modelWithField

        with(testFrameworkManager) {
            if (expectedModel.isMockModel()) {
                currentBlock += assertions[assertSame](expected, actual).toStatement()
                return
            }

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
                            baseName = nameGenerator.variableName("actualObject"),
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
                is UtCompositeModel -> assertDeepEqualsForComposite(
                    expected = expected,
                    actual = actual,
                    expectedModel = expectedModel,
                    depth = depth,
                    visitedModels = visitedModels
                )
                is UtCustomModel -> assertDeepEqualsForComposite(
                    expected = expected,
                    actual = actual,
                    expectedModel = expectedModel.origin
                        ?: error("Can't generate equals assertion for custom expected model without origin [$expectedModel]"),
                    depth = depth,
                    visitedModels = visitedModels
                )
                is UtLambdaModel -> Unit // we do not check equality of lambdas
                is UtVoidModel -> {
                    // Unit result is considered in generateResultAssertions method
                    error("Unexpected UtVoidModel in deep equals")
                }
                else -> {}
            }
        }
    }

    private fun TestFrameworkManager.assertDeepEqualsForComposite(
        expected: CgVariable,
        actual: CgVariable,
        expectedModel: UtCompositeModel,
        depth: Int,
        visitedModels: MutableSet<ModelWithField>
    ) {
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

        // We can use overridden equals if we have one, but not for mocks.
        if (expected.hasNotParametrizedCustomEquals() && !expectedModel.isMock) {
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

    private fun TestFrameworkManager.addArraysLengthAssertion(
        expected: CgVariable,
        actual: CgVariable,
    ): CgDeclaration {
        val cgGetLengthDeclaration = CgDeclaration(
            intClassId,
            nameGenerator.variableName("${expected.name}Size"),
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
                        baseName = nameGenerator.variableName("${expected.name}NestedElement"),
                        init = { CgArrayElementAccess(expected, i) }
                    )

                    val actualNestedElement = newVar(
                        baseType = actual.type.elementClassId!!,
                        baseName = nameGenerator.variableName("${actual.name}NestedElement"),
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
        visitedModels: MutableSet<ModelWithField>
    ) {
        // if field is static, it is represents itself in "before" and
        // "after" state: no need to assert its equality to itself.
        if (fieldId.isStatic) {
            return
        }

        // if model is already processed, so we don't want to add new statements
        val modelWithField = ModelWithField(fieldModel, fieldId)
        if (modelWithField in visitedModels) {
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
        visitedModels: MutableSet<ModelWithField>
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
            fieldId,
        )
        emptyLineIfNeeded()
    }

    private fun traverseFieldForParametrizedTest(
        fieldId: FieldId,
        fieldModel: UtModel,
        expected: CgVariable,
        actual: CgVariable,
        depth: Int,
        visitedModels: MutableSet<ModelWithField>
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
                        fieldId,
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
                fieldId,
            )
        }
        emptyLineIfNeeded()
    }

    private fun collectExecutionsResultFields() {
        for (model in successfulExecutionsModels) {
            when (model) {
                is UtCompositeModel -> collectExecutionsResultFieldsRecursively(model, 0)

                is UtModelWithCompositeOrigin -> model.origin?.let {
                    collectExecutionsResultFieldsRecursively(it, 0)
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
                else -> {}
            }
        }
    }

    private fun collectExecutionsResultFieldsRecursively(model: UtCompositeModel, depth: Int) {
        for ((fieldId, fieldModel) in model.fields) {
            collectExecutionsResultFieldsRecursively(fieldId, fieldModel, depth)
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
            is UtCompositeModel -> collectExecutionsResultFieldsRecursively(fieldModel, depth + 1)

            is UtModelWithCompositeOrigin -> fieldModel.origin?.let {
                collectExecutionsResultFieldsRecursively(it, depth + 1)
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
            else -> {}
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
        )
        error("")
    }

    private fun FieldId.getAccessExpression(variable: CgVariable): CgExpression =
        // Can directly access field only if it is declared in variable class (or in its ancestors)
        // and is accessible from current package
        if (variable.type.hasField(this) && canBeReadFrom(context, variable.type)) {
            if (jField.isStatic) CgStaticFieldAccess(this) else CgFieldAccess(variable, this)
        } else if (context.codegenLanguage == CodegenLanguage.JAVA &&
            !jField.isStatic && canBeReadViaGetterFrom(context)
        ) {
            variable[getter]()
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

    fun assertEquality(
        expected: UtModel,
        actual: CgVariable,
        expectedVariableName: String = "expected",
        emptyLineIfNeeded: Boolean = false,
    ) {
        val successfullyConstructedCustomAssert = expected is UtCustomModel &&
                customAssertConstructor.tryConstructCustomAssert(expected, actual)

        if (!successfullyConstructedCustomAssert) {
            val expectedVariable = variableConstructor.getOrCreateVariable(expected, expectedVariableName)
            if (emptyLineIfNeeded) emptyLineIfNeeded()
            assertEquality(expectedVariable, actual)
        }
    }

    open fun assertEquality(expected: CgValue, actual: CgVariable) {
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

    private fun generateConstructorCall(currentExecutableId: ConstructorId, currentExecution: UtExecution) {
        // we cannot generate any assertions for constructor testing
        // but we need to generate a constructor call
        val constructorCall = currentExecutableId as ConstructorId
        val executionResult = currentExecution.result

        executionResult
            .onSuccess {
                methodType = SUCCESSFUL

                require(!constructorCall.classId.isInner) {
                    "Inner class ${constructorCall.classId} constructor testing is not supported yet"
                }

                actual = newVar(constructorCall.classId, "actual") {
                    constructorCall(*methodArguments.toTypedArray())
                }
            }
            .onFailure { exception -> processExecutionFailure(exception, executionResult) }
    }

    // Class is required to verify, if current model has already been analyzed in deepEquals.
    // Using model without related field (if it is present) in comparison is incorrect,
    // for example, for [UtNullModel] as they are equal to each other..
    private data class ModelWithField(
        val fieldModel: UtModel,
        val relatedField: FieldId?,
    )

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
                        trueBranch = {
                            workaround(WorkaroundReason.CONSUME_DIRTY_STREAMS) {
                                if (containsStreamConsumingFailureForParametrizedTests) {
                                    constructStreamConsumingBlock().invoke()
                                } else {
                                    +testFrameworkManager.assertions[testFramework.assertNull](actual).toStatement()
                                }
                            }
                        },
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

    protected fun recordActualResult() {
        val executionResult = currentExecution!!.result

        executionResult.onSuccess { resultModel ->
            when (val executable = currentExecutableToCall) {
                is ConstructorId -> {
                    // there is nothing to generate for constructors
                    return
                }
                is BuiltinMethodId -> error("Unexpected BuiltinMethodId $executable while generating actual result")
                is MethodId -> {
                    // TODO possible engine bug - void method return type and result model not UtVoidModel
                    if (resultModel.isUnit() || executable.returnType == voidClassId) return

                    emptyLineIfNeeded()

                    actual = newVar(
                        CgClassId(resultModel.classId, isNullable = resultModel is UtNullModel),
                        "actual"
                    ) {
                        thisInstance[executable](*methodArguments.toTypedArray())
                    }
                }
                else -> {} // TODO: check this specific case
            }
        }.onFailure {
            processActualInvocationFailure(executionResult)
        }
    }

    private fun processActualInvocationFailure(executionResult: UtExecutionResult) {
        when (executionResult) {
            is UtStreamConsumingFailure -> processStreamConsumingException(executionResult.rootCauseException)
            else -> {} // Do nothing for now
        }
    }

    private fun processStreamConsumingException(innerException: Throwable) {
        val executable = currentExecutableToCall

        require((executable is MethodId) && (executable.returnType isSubtypeOf baseStreamClassId)) {
            "Unexpected exception $innerException during stream consuming in non-stream returning executable $executable"
        }

        emptyLineIfNeeded()

        actual = newVar(
            CgClassId(executable.returnType, isNullable = false),
            "actual"
        ) {
            thisInstance[executable](*methodArguments.toTypedArray())
        }
    }

    open fun createTestMethod(testSet: CgMethodTestSet, execution: UtExecution): CgTestMethod =
        withTestMethodScope(execution) {
            val testMethodName = nameGenerator.testMethodNameFor(testSet.executableUnderTest, execution.testMethodName)
            if (execution.testMethodName == null) {
                execution.testMethodName = testMethodName
            }
            // TODO: remove this line when SAT-1273 is completed
            execution.displayName = execution.displayName?.let { "${testSet.executableUnderTest.name}: $it" }
            testMethod(testMethodName, execution.displayName) {
                //Enum constants are static, but there is no need to store and recover value for them
                val statics = currentExecution!!.stateBefore
                    .statics
                    .filterNot { it.value is UtEnumConstantModel }
                    .filterNot { it.value.classId.outerClass?.isEnum == true }

                rememberInitialStaticFields(statics)
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
                        val name = paramNames[execution.executableToCall ?: testSet.executableUnderTest]?.get(index)
                        methodArguments += variableConstructor.getOrCreateVariable(param, name)
                    }

                    if (requiresFieldStateAssertions()) {
                        // we should generate field assertions only for successful tests
                        // that does not break the current test execution after invocation of method under test
                    }

                    recordActualResult()
                    generateResultAssertions()

                    if (requiresFieldStateAssertions()) {
                        // we should generate field assertions only for successful tests
                        // that does not break the current test execution after invocation of method under test
                        generateFieldStateAssertions()
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

                        // TODO this type parameter (required for Kotlin test) is unused until the proper implementation
                        //  of generics in code generation https://github.com/UnitTestBot/UTBotJava/issues/88
                        @Suppress("UNUSED_VARIABLE")
                        val typeParameter = when (val firstArg = (it.initializer as CgMethodCall).arguments.first()) {
                            is CgGetJavaClass -> firstArg.classId
                            is CgVariable -> firstArg.type
                            else -> error("Unexpected mocked resource declaration argument $firstArg")
                        }

                        +CgDeclaration(
                            it.variableType,
                            it.variableName,
                            initializer = nullLiteral(),
                            isMutable = true,
                        )
                    }
                    +tryWithMocksFinallyClosing
                }
            }
        }

    private fun requiresFieldStateAssertions(): Boolean =
        !methodType.isThrowing ||
                (methodType == PASSED_EXCEPTION && !testFrameworkManager.isExpectedExceptionExecutionBreaking)

    private val expectedResultVarName = "expectedResult"
    private val expectedErrorVarName = "expectedError"

    /**
     * Returns `null` if parameterized test method can't be created for [testSet]
     * (for example, when there are multiple distinct [CgMethodTestSet.executablesToCall]).
     */
    fun createParameterizedTestMethod(testSet: CgMethodTestSet, dataProviderMethodName: String): CgTestMethod? {
        //TODO: orientation on generic execution may be misleading, but what is the alternative?
        //may be a heuristic to select a model with minimal number of internal nulls should be used
        val genericExecution = chooseGenericExecution(testSet.executions)

        workaround(WorkaroundReason.CONSUME_DIRTY_STREAMS) {
            containsStreamConsumingFailureForParametrizedTests = testSet.executions.any {
                it.result is UtStreamConsumingFailure
            }
        }

        val statics = genericExecution.stateBefore.statics

        return withTestMethodScope(genericExecution) {
            val testName = nameGenerator.parameterizedTestMethodName(dataProviderMethodName)
            withNameScope {
                val testParameterDeclarations =
                    createParameterDeclarations(testSet, genericExecution) ?: return@withNameScope null

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
                    thisInstance = genericExecution.stateBefore.thisInstance?.let {
                        currentMethodParameters[CgParameterKind.ThisInstance]
                    }

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

    /**
     * Returns `null` if parameter declarations can't be created for [testSet]
     * (for example, when there are multiple distinct [CgMethodTestSet.executablesToCall]).
     */
    private fun createParameterDeclarations(
        testSet: CgMethodTestSet,
        genericExecution: UtExecution,
    ): List<CgParameterDeclaration>? {
        val executableToCall = testSet.executablesToCall.singleOrNull() ?: return null
        val executableUnderTestParameters = executableToCall.executable.parameters

        return mutableListOf<CgParameterDeclaration>().apply {
            // this instance
            genericExecution.stateBefore.thisInstance?.let {
                val type = wrapTypeIfRequired(it.classId)
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
                val argumentName = paramNames[executableToCall]?.get(index)
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

            val expectedResultClassId = wrapTypeIfRequired(testSet.getCommonResultTypeOrNull() ?: return null)
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
            val argumentName = paramNames[execution.executableToCall ?: testSet.executableUnderTest]?.get(paramIndex)
            arguments += variableConstructor.getOrCreateVariable(paramModel, argumentName)
        }

        val statics = execution.stateBefore.statics
        for ((field, model) in statics) {
            arguments += variableConstructor.getOrCreateVariable(model, field.name)
        }

        val method = currentExecutableToCall!!
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

    protected fun <R> withTestMethodScope(execution: UtExecution, block: () -> R): R {
        clearTestMethodScope()
        currentExecution = execution
        determineExecutionType()
        statesCache = EnvironmentFieldStateCache.emptyCacheFor(execution)
//        modelToUsageCountInMethod = countUsages(ignoreAssembleOrigin = true) { counter ->
//            execution.mapAllModels(counter)
//        }
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


    /**
     * Determines [CgTestMethodType] for current execution according to its success or failure.
     */
    private fun determineExecutionType() {
        val currentExecution = currentExecution!!

        currentExecution.result
            .onSuccess { methodType = SUCCESSFUL }
            .onFailure { exception ->
                methodType = when {
                    shouldTestPassWithException(currentExecution, exception) -> PASSED_EXCEPTION
                    shouldTestPassWithTimeoutException(currentExecution, exception) -> TIMEOUT
                    else -> when (exception) {
                        is ArtificialError -> ARTIFICIAL
                        is InstrumentedProcessDeathException -> CRASH
                        is AccessControlException -> CRASH // exception from sandbox
                        else -> FAILING
                    }
                }
            }
    }

    protected fun testMethod(
        methodName: String,
        displayName: String?,
        params: List<CgParameterDeclaration> = emptyList(),
        parameterized: Boolean = false,
        dataProviderMethodName: String? = null,
        body: () -> Unit,
    ): CgTestMethod {
        if (parameterized) {
            testFrameworkManager.addParameterizedTestAnnotations(dataProviderMethodName)
        } else {
            addAnnotation(testFramework.testAnnotationId, AnnotationTarget.Method)
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
            testFrameworkManager.addDataProviderAnnotations(dataProviderMethodName)
            exceptions += collectedExceptions
            annotations += collectedMethodAnnotations
        }
    }

    fun errorMethod(testSet: CgMethodTestSet, errors: Map<String, Int>): CgRegion<CgMethod> {
        val name = nameGenerator.errorMethodNameFor(testSet.executableUnderTest)
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
        return CgSimpleRegion("Errors report for ${testSet.executableUnderTest.name}", listOf(errorTestMethod))
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
    protected fun CgExecutableCall.intercepted() {
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