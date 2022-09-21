package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.Junit4
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.codegen.model.constructor.TestClassContext
import org.utbot.framework.codegen.model.constructor.builtin.forName
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.constructor.util.addToListMethodId
import org.utbot.framework.codegen.model.constructor.util.argumentsClassId
import org.utbot.framework.codegen.model.constructor.util.argumentsMethodId
import org.utbot.framework.codegen.model.constructor.util.classCgClassId
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.codegen.model.constructor.util.setArgumentsArrayElement
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgAnnotation
import org.utbot.framework.codegen.model.tree.CgEnumConstantAccess
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgGetKotlinClass
import org.utbot.framework.codegen.model.tree.CgLiteral
import org.utbot.framework.codegen.model.tree.CgMethod
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.tree.CgMultipleArgsAnnotation
import org.utbot.framework.codegen.model.tree.CgNamedAnnotationArgument
import org.utbot.framework.codegen.model.tree.CgSingleArgAnnotation
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.util.classLiteralAnnotationArgument
import org.utbot.framework.codegen.model.util.isAccessibleFrom
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.codegen.model.util.stringLiteral
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ConstructorId
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
internal abstract class TestFrameworkManager(val context: CgContext)
    : CgContextOwner by context,
        CgCallableAccessManager by CgComponents.getCallableAccessManagerBy(context) {

    val assertions = context.testFramework.assertionsClass

    val assertEquals = context.testFramework.assertEquals
    val assertFloatEquals = context.testFramework.assertFloatEquals
    val assertDoubleEquals = context.testFramework.assertDoubleEquals

    val assertNull = context.testFramework.assertNull
    val assertNotNull = context.testFramework.assertNotNull
    val assertTrue = context.testFramework.assertTrue
    val assertFalse = context.testFramework.assertFalse

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

    protected val statementConstructor = CgComponents.getStatementConstructorBy(context)

    abstract val annotationForNestedClasses: CgAnnotation?

    abstract val annotationForOuterClasses: CgAnnotation?

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

    // Exception expectation differs between test frameworks
    // JUnit4 requires to add a specific argument to the test method annotation
    // JUnit5 requires using method assertThrows()
    // TestNg allows both approaches, we use similar to JUnit5
    abstract fun expectException(exception: ClassId, block: () -> Unit)

    /**
     * Creates annotations for data provider method in parameterized tests
     */
    abstract fun createDataProviderAnnotations(dataProviderMethodName: String): MutableList<CgAnnotation>

    /**
     * Creates declaration of argList collection in parameterized tests.
     */
    abstract fun createArgList(length: Int): CgVariable

    abstract fun collectParameterizedTestAnnotations(dataProviderMethodName: String?): Set<CgAnnotation>

    abstract fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int)

    open fun expectTimeout(timeoutMs: Long, block: () -> Unit) {}

    open fun setTestExecutionTimeout(timeoutMs: Long) {
        val timeout = CgNamedAnnotationArgument(
            name = timeoutArgumentName,
            value = timeoutMs.resolve()
        )
        val testAnnotation = collectedMethodAnnotations.singleOrNull { it.classId == testFramework.testAnnotationId }

        if (testAnnotation is CgMultipleArgsAnnotation) {
            testAnnotation.arguments += timeout
        } else {
            collectedMethodAnnotations += CgMultipleArgsAnnotation(
                testFramework.testAnnotationId,
                mutableListOf(timeout)
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
        collectedMethodAnnotations += CgSingleArgAnnotation(Junit5.displayNameClassId, stringLiteral(name))
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

    override val annotationForNestedClasses: CgAnnotation?
        get() = null

    override val annotationForOuterClasses: CgAnnotation?
        get() = null

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

    override fun createDataProviderAnnotations(dataProviderMethodName: String) =
        mutableListOf(
            statementConstructor.annotation(
                testFramework.methodSourceAnnotationId,
                listOf("name" to stringLiteral(dataProviderMethodName))
            ),
        )

    override fun createArgList(length: Int) =
        statementConstructor.newVar(testFramework.argListClassId, "argList") {
            CgAllocateArray(testFramework.argListClassId, Array<Any>::class.java.id, length)
        }

    override fun collectParameterizedTestAnnotations(dataProviderMethodName: String?) = setOf(
        statementConstructor.annotation(
            testFramework.parameterizedTestAnnotationId,
            listOf("dataProvider" to CgLiteral(stringClassId, dataProviderMethodName))
        )
    )

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
            collectedMethodAnnotations += CgMultipleArgsAnnotation(
                testFramework.testAnnotationId,
                mutableListOf(descriptionArgument)
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
            collectedMethodAnnotations += CgMultipleArgsAnnotation(
                testFramework.testAnnotationId,
                mutableListOf(disabledAnnotationArgument, descriptionTestAnnotationArgument)
            )
        }
    }
}

internal class Junit4Manager(context: CgContext) : TestFrameworkManager(context) {
    private val parametrizedTestsNotSupportedError: Nothing
        get() = error("Parametrized tests are not supported for JUnit4")

    override val dataProviderMethodsHolder: TestClassContext
        get() = parametrizedTestsNotSupportedError

    override val annotationForNestedClasses: CgAnnotation?
        get() = null

    override val annotationForOuterClasses: CgAnnotation
        get() {
            require(testFramework is Junit4) { "According to settings, JUnit4 was expected, but got: $testFramework" }
            return statementConstructor.annotation(
                testFramework.runWithAnnotationClassId,
                testFramework.enclosedClassId.let {
                    when (codegenLanguage) {
                        CodegenLanguage.JAVA   -> CgGetJavaClass(it)
                        CodegenLanguage.KOTLIN -> CgGetKotlinClass(it)
                    }
                }
            )
        }

    override fun expectException(exception: ClassId, block: () -> Unit) {
        require(testFramework is Junit4) { "According to settings, JUnit4 was expected, but got: $testFramework" }

        require(exception.isAccessibleFrom(testClassPackageName)) {
            "Exception $exception is not accessible from package $testClassPackageName"
        }

        val expected = CgNamedAnnotationArgument(
            name = "expected",
            value = classLiteralAnnotationArgument(exception, codegenLanguage)
        )
        val testAnnotation = collectedMethodAnnotations.singleOrNull { it.classId == testFramework.testAnnotationId }
        if (testAnnotation is CgMultipleArgsAnnotation) {
            testAnnotation.arguments += expected
        } else {
            collectedMethodAnnotations += CgMultipleArgsAnnotation(testFramework.testAnnotationId, mutableListOf(expected))
        }
        block()
    }

    override fun createDataProviderAnnotations(dataProviderMethodName: String) =
        parametrizedTestsNotSupportedError

    override fun createArgList(length: Int) =
        parametrizedTestsNotSupportedError

    override fun collectParameterizedTestAnnotations(dataProviderMethodName: String?) =
        parametrizedTestsNotSupportedError

    override fun passArgumentsToArgsVariable(argsVariable: CgVariable, argsArray: CgVariable, executionIndex: Int) =
        parametrizedTestsNotSupportedError

    override fun addTestDescription(description: String) = Unit

    override fun disableTestMethod(reason: String) {
        require(testFramework is Junit4) { "According to settings, JUnit4 was expected, but got: $testFramework" }

        collectedMethodAnnotations += CgMultipleArgsAnnotation(
            testFramework.ignoreAnnotationClassId,
            mutableListOf(
                CgNamedAnnotationArgument(
                    name = "value",
                    value = reason.resolve()
                )
            )
        )
    }
}

internal class Junit5Manager(context: CgContext) : TestFrameworkManager(context) {
    override val dataProviderMethodsHolder: TestClassContext
        get() = outerMostTestClassContext

    override val annotationForNestedClasses: CgAnnotation
        get() {
            require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }

            return statementConstructor.annotation(testFramework.nestedTestClassAnnotationId)
        }

    override val annotationForOuterClasses: CgAnnotation?
        get() = null

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

    override fun createDataProviderAnnotations(dataProviderMethodName: String) = mutableListOf<CgAnnotation>()

    override fun createArgList(length: Int) =
        statementConstructor.newVar(testFramework.argListClassId, "argList") {
            val constructor = ConstructorId(testFramework.argListClassId, emptyList())
            constructor.invoke()
        }

    override fun collectParameterizedTestAnnotations(dataProviderMethodName: String?) = setOf(
        statementConstructor.annotation(testFramework.parameterizedTestAnnotationId),
        statementConstructor.annotation(
            testFramework.methodSourceAnnotationId,
            "${outerMostTestClass.canonicalName}#$dataProviderMethodName"
        )
    )

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
        collectedMethodAnnotations += statementConstructor.annotation(testFramework.displayNameClassId, name)
    }

    override fun setTestExecutionTimeout(timeoutMs: Long) {
        require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }

        val timeoutAnnotationArguments = mutableListOf<CgNamedAnnotationArgument>()
        timeoutAnnotationArguments += CgNamedAnnotationArgument(
            name = "value",
            value = timeoutMs.resolve()
        )

        val milliseconds = TimeUnit.MILLISECONDS
        timeoutAnnotationArguments += CgNamedAnnotationArgument(
            name = "unit",
            value = CgEnumConstantAccess(testFramework.timeunitClassId, milliseconds.name)
        )
        importIfNeeded(testFramework.timeunitClassId)

        collectedMethodAnnotations += CgMultipleArgsAnnotation(
            Junit5.timeoutClassId,
            timeoutAnnotationArguments
        )
    }

    override fun addTestDescription(description: String) = addDisplayName(description)

    override fun disableTestMethod(reason: String) {
        require(testFramework is Junit5) { "According to settings, JUnit5 was expected, but got: $testFramework" }

        collectedMethodAnnotations += CgMultipleArgsAnnotation(
            testFramework.disabledAnnotationClassId,
            mutableListOf(
                CgNamedAnnotationArgument(
                    name = "value",
                    value = reason.resolve()
                )
            )
        )
    }
}
