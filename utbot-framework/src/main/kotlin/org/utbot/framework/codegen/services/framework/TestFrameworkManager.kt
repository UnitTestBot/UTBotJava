package org.utbot.framework.codegen.services.framework

import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.TestNg
import org.utbot.framework.codegen.domain.context.TestClassContext
import org.utbot.framework.codegen.domain.builtin.forName
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.*
import org.utbot.framework.codegen.domain.models.AnnotationTarget.*
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.tree.CgComponents.getCallableAccessManagerBy
import org.utbot.framework.codegen.tree.CgComponents.getStatementConstructorBy
import org.utbot.framework.codegen.tree.addToListMethodId
import org.utbot.framework.codegen.tree.argumentsClassId
import org.utbot.framework.codegen.tree.argumentsMethodId
import org.utbot.framework.codegen.tree.classCgClassId
import org.utbot.framework.codegen.tree.importIfNeeded
import org.utbot.framework.codegen.tree.setArgumentsArrayElement
import org.utbot.framework.codegen.util.isAccessibleFrom
import org.utbot.framework.codegen.util.resolve
import org.utbot.framework.codegen.util.stringLiteral
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.util.SpringModelUtils.extendWithClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.runWithClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springExtensionClassId
import org.utbot.framework.plugin.api.util.booleanArrayClassId
import org.utbot.framework.plugin.api.util.byteArrayClassId
import org.utbot.framework.plugin.api.util.charArrayClassId
import org.utbot.framework.plugin.api.util.doubleArrayClassId
import org.utbot.framework.plugin.api.util.floatArrayClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intArrayClassId
import org.utbot.framework.plugin.api.util.longArrayClassId
import org.utbot.framework.plugin.api.util.shortArrayClassId
import org.utbot.framework.plugin.api.util.stringClassId
import java.util.concurrent.TimeUnit

@Suppress("MemberVisibilityCanBePrivate")
abstract class TestFrameworkManager(val context: CgContext)
    : CgContextOwner by context,
        CgCallableAccessManager by getCallableAccessManagerBy(context) {

    val assertions = context.testFramework.assertionsClass

    val assertEquals = context.testFramework.assertEquals
    val assertFloatEquals = context.testFramework.assertFloatEquals
    val assertDoubleEquals = context.testFramework.assertDoubleEquals

    val assertNull = context.testFramework.assertNull
    val assertNotNull = context.testFramework.assertNotNull
    val assertTrue = context.testFramework.assertTrue
    val assertFalse = context.testFramework.assertFalse

    val fail = context.testFramework.fail
    val kotlinFail = context.testFramework.kotlinFail

    val assertArrayEquals = context.testFramework.assertArrayEquals
    val assertBooleanArrayEquals = context.testFramework.assertBooleanArrayEquals
    val assertByteArrayEquals = context.testFramework.assertByteArrayEquals
    val assertCharArrayEquals = context.testFramework.assertCharArrayEquals
    val assertShortArrayEquals = context.testFramework.assertShortArrayEquals
    val assertIntArrayEquals = context.testFramework.assertIntArrayEquals
    val assertLongArrayEquals = context.testFramework.assertLongArrayEquals
    val assertFloatArrayEquals = context.testFramework.assertFloatArrayEquals
    val assertDoubleArrayEquals = context.testFramework.assertDoubleArrayEquals

    // Points to the class, into which data provider methods in parametrized tests should be put (current or outermost).
    // It is needed, because data provider methods are static and thus may not be put into inner classes, e.g. in JUnit5
    // all data providers should be placed in the outermost class.
    protected abstract val dataProviderMethodsHolder: TestClassContext

    protected val statementConstructor = getStatementConstructorBy(context)

    abstract fun addAnnotationForNestedClasses()

    /**
     * Determines whether appearance of expected exception in test method breaks current test execution or not.
     */
    abstract val isExpectedExceptionExecutionBreaking: Boolean

    protected open val timeoutArgumentName: String = "timeout"

    open fun assertEquals(expected: CgValue, actual: CgValue) {
        +assertions[assertEquals](expected, actual)
    }

    open fun assertFloatEquals(expected: CgExpression, actual: CgExpression, delta: Any) {
        +assertions[assertFloatEquals](expected, actual, delta)
    }

    open fun assertDoubleEquals(expected: CgExpression, actual: CgExpression, delta: Any) {
        +assertions[assertDoubleEquals](expected, actual, delta)
    }

    open fun getArrayEqualsAssertion(arrayType: ClassId, expected: CgExpression, actual: CgExpression): CgMethodCall =
        when (arrayType) {
            booleanArrayClassId -> assertions[assertBooleanArrayEquals](expected, actual)
            byteArrayClassId -> assertions[assertByteArrayEquals](expected, actual)
            charArrayClassId -> assertions[assertCharArrayEquals](expected, actual)
            shortArrayClassId -> assertions[assertShortArrayEquals](expected, actual)
            intArrayClassId -> assertions[assertIntArrayEquals](expected, actual)
            longArrayClassId -> assertions[assertLongArrayEquals](expected, actual)
            floatArrayClassId -> assertions[assertFloatArrayEquals](expected, actual)
            doubleArrayClassId -> assertions[assertDoubleArrayEquals](expected, actual)
            else -> assertions[assertArrayEquals](expected, actual)
        }

    fun assertArrayEquals(arrayType: ClassId, expected: CgExpression, actual: CgExpression) {
        +getArrayEqualsAssertion(arrayType, expected, actual)
    }

    open fun getDeepEqualsAssertion(expected: CgExpression, actual: CgExpression): CgMethodCall {
        requiredUtilMethods += setOf(
            utilMethodProvider.deepEqualsMethodId,
            utilMethodProvider.arraysDeepEqualsMethodId,
            utilMethodProvider.iterablesDeepEqualsMethodId,
            utilMethodProvider.streamsDeepEqualsMethodId,
            utilMethodProvider.mapsDeepEqualsMethodId,
            utilMethodProvider.hasCustomEqualsMethodId
        )
        // TODO we cannot use common assertEquals because of using custom deepEquals
        //  For this reason we have to use assertTrue here
        //  Unfortunately, if test with assertTrue fails, it gives non informative message false != true
        //  Thus, we need to provide custom message to assertTrue showing compared objects correctly
        //  SAT-1345
        return assertions[assertTrue](utilsClassId[deepEquals](expected, actual))
    }

    @Suppress("unused")
    fun assertDeepEquals(expected: CgExpression, actual: CgExpression) {
        +getDeepEqualsAssertion(expected, actual)
    }

    open fun getFloatArrayEqualsAssertion(expected: CgExpression, actual: CgExpression, delta: Any): CgMethodCall =
        assertions[assertFloatArrayEquals](expected, actual, delta)

    fun assertFloatArrayEquals(expected: CgExpression, actual: CgExpression, delta: Any) {
        +getFloatArrayEqualsAssertion(expected, actual, delta)
    }

    open fun getDoubleArrayEqualsAssertion(expected: CgExpression, actual: CgExpression, delta: Any): CgMethodCall =
        assertions[assertDoubleArrayEquals](expected, actual, delta)

    fun assertDoubleArrayEquals(expected: CgExpression, actual: CgExpression, delta: Any) {
        +getDoubleArrayEqualsAssertion(expected, actual, delta)
    }

    fun assertNull(actual: CgExpression) {
        +assertions[assertNull](actual)
    }

    fun assertBoolean(expected: Boolean, actual: CgExpression) {
        if (expected) {
            +assertions[assertTrue](actual)
        } else {
            +assertions[assertFalse](actual)
        }
    }

    fun assertBoolean(actual: CgExpression) = assertBoolean(expected = true, actual)

    fun fail(actual: CgExpression) {
        // failure assertion may be implemented in different packages in Java and Kotlin
        // more details at https://stackoverflow.com/questions/52967039/junit-5-assertions-fail-can-not-infer-type-in-kotlin
        when (codegenLanguage) {
            CodegenLanguage.JAVA -> +assertions[fail](actual)
            CodegenLanguage.KOTLIN -> +assertions[kotlinFail](actual)
        }
    }

    // Exception expectation differs between test frameworks
    // JUnit4 requires to add a specific argument to the test method annotation
    // JUnit5 requires using method assertThrows()
    // TestNg allows both approaches, we use similar to JUnit5
    abstract fun expectException(exception: ClassId, block: () -> Unit)

    /**
     * Creates annotations for data provider method in parameterized tests
     */
    abstract fun addDataProviderAnnotations(dataProviderMethodName: String)

    /**
     * Creates declaration of argList collection in parameterized tests.
     */
    abstract fun createArgList(length: Int): CgVariable

    abstract fun addParameterizedTestAnnotations(dataProviderMethodName: String?)

    abstract fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int)

    /**
     * Most frameworks don't have special timeout assertion, so only tested
     * method call is generated, while timeout is set using
     * [timeout argument][timeoutArgumentName] of the test annotation
     *
     * @see setTestExecutionTimeout
     */
    open fun expectTimeout(timeoutMs: Long, block: () -> Unit): Unit = block()

    open fun setTestExecutionTimeout(timeoutMs: Long) {
        val timeout = CgNamedAnnotationArgument(
            name = timeoutArgumentName,
            value = timeoutMs.resolve()
        )
        val testAnnotation = collectedMethodAnnotations.singleOrNull { it.classId == testFramework.testAnnotationId }

        if (testAnnotation is CgMultipleArgsAnnotation) {
            testAnnotation.arguments += timeout
        } else {
            statementConstructor.addAnnotation(
                classId = testFramework.testAnnotationId,
                namedArguments = listOf(timeout),
                target = Method,
            )
        }
    }


    /**
     * Add a short test's description depending on the test framework type:
     */
    abstract fun addTestDescription(description: String)

    abstract fun disableTestMethod(reason: String)

    /**
     * Adds @DisplayName annotation.
     *
     * Should be used only with JUnit 5.
     * @see <a href="https://github.com/UnitTestBot/UTBotJava/issues/576">issue-576 on GitHub</a>
     */
    open fun addDisplayName(name: String) {
        statementConstructor.addAnnotation(Junit5.displayNameClassId, stringLiteral(name), Method)
    }

    protected fun ClassId.toExceptionClass(): CgExpression =
            if (isAccessibleFrom(testClassPackageName)) {
                CgGetJavaClass(this)
            } else {
                statementConstructor.newVar(classCgClassId) { Class::class.id[forName](name) }
            }

    fun addDataProvider(dataProvider: CgMethod) {
        dataProviderMethodsHolder.cgDataProviderMethods += dataProvider
    }
}

internal class TestNgManager(context: CgContext) : TestFrameworkManager(context) {
    override val dataProviderMethodsHolder: TestClassContext
        get() = currentTestClassContext

    override fun addAnnotationForNestedClasses() { }

    override val isExpectedExceptionExecutionBreaking: Boolean = false

    override val timeoutArgumentName: String = "timeOut"

    private val assertThrows: BuiltinMethodId
        get() {
            require(testFramework is TestNg) { "According to settings, TestNg was expected, but got: $testFramework" }

            return testFramework.assertThrows
        }

    override fun assertEquals(expected: CgValue, actual: CgValue) = super.assertEquals(actual, expected)

    override fun assertFloatEquals(expected: CgExpression, actual: CgExpression, delta: Any) =
            super.assertFloatEquals(actual, expected, delta)

    override fun assertDoubleEquals(expected: CgExpression, actual: CgExpression, delta: Any) =
            super.assertDoubleEquals(actual, expected, delta)

    override fun getArrayEqualsAssertion(arrayType: ClassId, expected: CgExpression, actual: CgExpression): CgMethodCall =
            super.getArrayEqualsAssertion(arrayType, actual, expected)

    override fun getDeepEqualsAssertion(expected: CgExpression, actual: CgExpression): CgMethodCall =
            super.getDeepEqualsAssertion(actual, expected)

    override fun getFloatArrayEqualsAssertion(expected: CgExpression, actual: CgExpression, delta: Any): CgMethodCall =
            super.getFloatArrayEqualsAssertion(actual, expected, delta)

    override fun getDoubleArrayEqualsAssertion(expected: CgExpression, actual: CgExpression, delta: Any): CgMethodCall =
            super.getDoubleArrayEqualsAssertion(actual, expected, delta)

    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is TestNg) { "According to settings, TestNg was expected, but got: $testFramework" }
        val lambda = statementConstructor.lambda(testFramework.throwingRunnableClassId) { block() }
        +assertions[assertThrows](exception.toExceptionClass(), lambda)
    }

    override fun addDataProviderAnnotations(dataProviderMethodName: String) {
        statementConstructor.addAnnotation(
            testFramework.methodSourceAnnotationId,
            listOf("name" to stringLiteral(dataProviderMethodName)),
            Method,
        )
    }

    override fun createArgList(length: Int) =
        statementConstructor.newVar(testFramework.argListClassId, "argList") {
            CgAllocateArray(testFramework.argListClassId, Array<Any>::class.java.id, length)
        }

    override fun addParameterizedTestAnnotations(dataProviderMethodName: String?) {
            statementConstructor.addAnnotation(
                testFramework.parameterizedTestAnnotationId,
                listOf("dataProvider" to CgLiteral(stringClassId, dataProviderMethodName)),
                Method,
            )
    }

    override fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int) =
        setArgumentsArrayElement(argsVariable, executionIndex, argsArray, statementConstructor)

    /**
     * Supplements TestNG @Test annotation with a description.
     * It looks like @Test(description="...")
     *
     * @see <a href="https://github.com/UnitTestBot/UTBotJava/issues/576">issue-576 on GitHub</a>
     */
    private fun addDescriptionAnnotation(description: String) {
        val testAnnotation =
            collectedMethodAnnotations.singleOrNull { it.classId == testFramework.testAnnotationId }

        val descriptionArgument = CgNamedAnnotationArgument("description", stringLiteral(description))
        if (testAnnotation is CgMultipleArgsAnnotation) {
            testAnnotation.arguments += descriptionArgument
        } else {
            statementConstructor.addAnnotation(
                classId = testFramework.testAnnotationId,
                namedArguments = listOf(descriptionArgument),
                target = Method,
            )
        }
    }

    override fun addTestDescription(description: String) = addDescriptionAnnotation(description)

    override fun disableTestMethod(reason: String) {
        require(testFramework is TestNg) { "According to settings, TestNg was expected, but got: $testFramework" }

        val disabledAnnotationArgument = CgNamedAnnotationArgument(
            name = "enabled",
            value = false.resolve()
        )

        val descriptionArgumentName = "description"
        val descriptionTestAnnotationArgument = CgNamedAnnotationArgument(
            name = descriptionArgumentName,
            value = reason.resolve()
        )

        val testAnnotation = collectedMethodAnnotations.singleOrNull { it.classId == testFramework.testAnnotationId }
        if (testAnnotation is CgMultipleArgsAnnotation) {
            testAnnotation.arguments += disabledAnnotationArgument

            val alreadyExistingDescriptionAnnotationArgument = testAnnotation.arguments.singleOrNull {
                it.name == descriptionArgumentName
            }

            // append new description to existing one
            if (alreadyExistingDescriptionAnnotationArgument != null) {
                val gluedDescription = with(alreadyExistingDescriptionAnnotationArgument) {
                    require(value is CgLiteral && value.value is String) {
                        "Expected description to be String literal but got ${value.type}"
                    }

                    listOf(value.value, reason).joinToString("; ").resolve()
                }

                testAnnotation.arguments.map {
                    if (it.name != descriptionArgumentName) return@map it

                    CgNamedAnnotationArgument(
                        name = descriptionArgumentName,
                        value = gluedDescription
                    )
                }
            } else {
                testAnnotation.arguments += descriptionTestAnnotationArgument
            }
        } else {
            statementConstructor.addAnnotation(
                classId = testFramework.testAnnotationId,
                namedArguments =listOf(disabledAnnotationArgument, descriptionTestAnnotationArgument),
                target = Method,
            )
        }
    }
}

internal class Junit4Manager(context: CgContext) : TestFrameworkManager(context) {
    private val parametrizedTestsNotSupportedError: Nothing
        get() = error("Parametrized tests are not supported for JUnit4")

    override val dataProviderMethodsHolder: TestClassContext
        get() = parametrizedTestsNotSupportedError

    override fun addAnnotationForNestedClasses() { }

    override val isExpectedExceptionExecutionBreaking: Boolean = true

    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Junit4) { "According to settings, JUnit4 was expected, but got: $testFramework" }

        require(exception.isAccessibleFrom(testClassPackageName)) {
            "Exception $exception is not accessible from package $testClassPackageName"
        }

        val expected = CgNamedAnnotationArgument(
            name = "expected",
            value = createGetClassExpression(exception, codegenLanguage)
        )
        val testAnnotation = collectedMethodAnnotations.singleOrNull { it.classId == testFramework.testAnnotationId }
        if (testAnnotation is CgMultipleArgsAnnotation) {
            testAnnotation.arguments += expected
        } else {
            statementConstructor.addAnnotation(
                classId = testFramework.testAnnotationId,
                namedArguments = listOf(expected),
                target = Method,
            )
        }
        block()
    }

    override fun addDataProviderAnnotations(dataProviderMethodName: String) =
        parametrizedTestsNotSupportedError

    override fun createArgList(length: Int) =
        parametrizedTestsNotSupportedError

    override fun addParameterizedTestAnnotations(dataProviderMethodName: String?) =
        parametrizedTestsNotSupportedError

    override fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int) =
        parametrizedTestsNotSupportedError

    override fun addTestDescription(description: String) = Unit

    override fun disableTestMethod(reason: String) {
        require(testFramework is Junit4) { "According to settings, JUnit4 was expected, but got: $testFramework" }

        val reasonArgument = CgNamedAnnotationArgument(name = "value", value = reason.resolve())
        statementConstructor.addAnnotation(
            classId = testFramework.ignoreAnnotationClassId,
            namedArguments = listOf(reasonArgument),
            target = Method,
        )
    }
}

internal class Junit5Manager(context: CgContext) : TestFrameworkManager(context) {
    override val dataProviderMethodsHolder: TestClassContext
        get() = outerMostTestClassContext

    override fun addAnnotationForNestedClasses() {
        require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }
        statementConstructor.addAnnotation(testFramework.nestedTestClassAnnotationId, Class)
    }

    override val isExpectedExceptionExecutionBreaking: Boolean = false

    private val assertThrows: BuiltinMethodId
        get() {
            require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }
            return testFramework.assertThrows
        }

    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }
        val lambda = statementConstructor.lambda(testFramework.executableClassId) { block() }
        +assertions[assertThrows](exception.toExceptionClass(), lambda)
    }

    override fun addDataProviderAnnotations(dataProviderMethodName: String) { }

    override fun createArgList(length: Int) =
        statementConstructor.newVar(testFramework.argListClassId, "argList") {
            val constructor = ConstructorId(testFramework.argListClassId, emptyList())
            constructor.invoke()
        }

    override fun addParameterizedTestAnnotations(dataProviderMethodName: String?) {
        statementConstructor.addAnnotation(testFramework.parameterizedTestAnnotationId, Method)
        statementConstructor.addAnnotation(
            testFramework.methodSourceAnnotationId,
            "${outerMostTestClass.canonicalName}#$dataProviderMethodName",
            Method,
        )
    }

    override fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int) {
        +argsVariable[addToListMethodId](
            argumentsClassId[argumentsMethodId](argsArray)
        )
    }


    override fun expectTimeout(timeoutMs : Long, block: () -> Unit) {
        require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }
        val lambda = statementConstructor.lambda(testFramework.executableClassId) { block() }
        importIfNeeded(testFramework.durationClassId)
        val duration = CgMethodCall(null, testFramework.ofMillis, listOf(timeoutMs.resolve()))
        +assertions[testFramework.assertTimeoutPreemptively](duration, lambda)
    }

    override fun addDisplayName(name: String) {
        require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }
        statementConstructor.addAnnotation(testFramework.displayNameClassId, name, Method)
    }

    override fun setTestExecutionTimeout(timeoutMs: Long) {
        require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }

        importIfNeeded(testFramework.timeunitClassId)

        statementConstructor.addAnnotation(
            classId = Junit5.timeoutClassId,
            namedArguments = listOf(
                CgNamedAnnotationArgument(
                    name = "value",
                    value = timeoutMs.resolve(),
                ),
                CgNamedAnnotationArgument(
                    name = "unit",
                    value = CgEnumConstantAccess(testFramework.timeunitClassId, TimeUnit.MILLISECONDS.name),
                ),
            ),
            target = Method,
        )
    }

    override fun addTestDescription(description: String) = addDisplayName(description)

    override fun disableTestMethod(reason: String) {
        require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }

        val reasonArgument = CgNamedAnnotationArgument(name = "value", value = reason.resolve())
        statementConstructor.addAnnotation(
            classId = testFramework.disabledAnnotationClassId,
            namedArguments = listOf(reasonArgument),
            target = Method,
        )
    }
}
