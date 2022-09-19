package org.utbot.framework.codegen

import org.utbot.framework.DEFAULT_CONCRETE_EXECUTION_TIMEOUT_IN_CHILD_PROCESS_MS
import org.utbot.framework.codegen.model.constructor.builtin.mockitoClassId
import org.utbot.framework.codegen.model.constructor.builtin.ongoingStubbingClassId
import org.utbot.framework.codegen.model.constructor.util.argumentsClassId
import org.utbot.framework.codegen.model.tree.CgClassId
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodeGenerationSettingBox
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.isolateCommandLineArgumentsToArgumentFile
import org.utbot.framework.plugin.api.util.booleanArrayClassId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.builtinMethodId
import org.utbot.framework.plugin.api.util.builtinStaticMethodId
import org.utbot.framework.plugin.api.util.byteArrayClassId
import org.utbot.framework.plugin.api.util.charArrayClassId
import org.utbot.framework.plugin.api.util.doubleArrayClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatArrayClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intArrayClassId
import org.utbot.framework.plugin.api.util.longArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.shortArrayClassId
import org.utbot.framework.plugin.api.util.voidClassId
import java.io.File
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.voidWrapperClassId

data class TestClassFile(val packageName: String, val imports: List<Import>, val testClass: String)

sealed class Import(internal val order: Int) : Comparable<Import> {
    abstract val qualifiedName: String

    override fun compareTo(other: Import) = importComparator.compare(this, other)
}

private val importComparator = compareBy<Import> { it.order }.thenBy { it.qualifiedName }

data class StaticImport(val qualifierClass: String, val memberName: String) : Import(1) {
    override val qualifiedName: String = "$qualifierClass.$memberName"
}

data class RegularImport(val packageName: String, val className: String) : Import(2) {
    override val qualifiedName: String
        get() = if (packageName.isNotEmpty()) "$packageName.$className" else className

    // TODO: check without equals() and hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegularImport

        if (packageName != other.packageName) return false
        if (className != other.className) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + className.hashCode()
        return result
    }
}

private const val TEST_NG_PACKAGE = "org.testng"
private const val JUNIT4_PACKAGE = "org.junit"
private const val JUNIT5_PACKAGE = "org.junit.jupiter.api"
const val JUNIT5_PARAMETERIZED_PACKAGE = "org.junit.jupiter.params"

//JUnit5 imports
private const val TEST_NG_ASSERTIONS = "org.testng.Assert"
private const val TEST_NG_ARRAYS_ASSERTIONS = "org.testng.internal.junit.ArrayAsserts"
private const val JUNIT5_ASSERTIONS = "org.junit.jupiter.api.Assertions"
private const val JUNIT4_ASSERTIONS = "org.junit.Assert"

fun junitByVersion(version: Int): TestFramework =
    when (version) {
        4 -> Junit4
        5 -> Junit5
        else -> error("Expected JUnit version 4 or 5, but got: $version")
    }

fun testFrameworkByName(testFramework: String): TestFramework =
    when (testFramework) {
        "junit4" -> Junit4
        "junit5" -> Junit5
        "testng" -> TestNg
        else -> error("Unexpected test framework name: $testFramework")
    }

/**
 * This feature allows to enable additional mockito-core settings required for static mocking.
 * It is implemented via adding special file "MockMaker" into test project resources.
 */
sealed class StaticsMocking(
    var isConfigured: Boolean = false,
    override val id: String,
    override val displayName: String,
    override val description: String = "Use static methods mocking"
) : CodeGenerationSettingItem {
    override fun toString(): String = id

    // Get is mandatory because of the initialization order of the inheritors.
    // Otherwise, in some cases we could get an incorrect value
    companion object : CodeGenerationSettingBox {
        override val defaultItem: StaticsMocking
            get() = MockitoStaticMocking
        override val allItems: List<StaticsMocking>
            get() = listOf(NoStaticMocking, MockitoStaticMocking)
    }
}

object NoStaticMocking : StaticsMocking(
    id = "No static mocking",
    displayName = "No static mocking",
    description = "Do not use additional settings to mock static fields"
)

object MockitoStaticMocking : StaticsMocking(id = "Mockito static mocking", displayName = "Mockito static mocking") {

    val mockedStaticClassId = BuiltinClassId(
        name = "org.mockito.MockedStatic",
        canonicalName = "org.mockito.MockedStatic",
        simpleName = "MockedStatic"
    )

    val mockedConstructionClassId = BuiltinClassId(
        name = "org.mockito.MockedConstruction",
        canonicalName = "org.mockito.MockedConstruction",
        simpleName = "MockedConstruction"
    )

    val mockStaticMethodId = builtinStaticMethodId(
        classId = mockitoClassId,
        name = "mockStatic",
        returnType = mockedStaticClassId,
        arguments = arrayOf(objectClassId)
    )

    val mockConstructionMethodId = builtinStaticMethodId(
        classId = mockitoClassId,
        name = "mockConstruction",
        returnType = mockedConstructionClassId,
        // actually second argument is lambda
        arguments = arrayOf(objectClassId, objectClassId)
    )

    val mockedStaticWhen = builtinMethodId(
        classId = mockedStaticClassId,
        name = "when",
        returnType = ongoingStubbingClassId,
        // argument type is actually a functional interface
        arguments = arrayOf(objectClassId)
    )

    fun mockedStaticWhen(nullable: Boolean): MethodId = builtinMethodId(
        classId = mockedStaticClassId,
        name = "when",
        returnType = CgClassId(ongoingStubbingClassId, isNullable = nullable),
        // argument type is actually a functional interface
        arguments = arrayOf(objectClassId)
    )
}

sealed class TestFramework(
    override val id: String,
    override val displayName: String,
    override val description: String = "Use $displayName as test framework",
) : CodeGenerationSettingItem {
    var isParametrizedTestsConfigured = false
    var isInstalled: Boolean = false
    abstract val mainPackage: String
    abstract val assertionsClass: ClassId
    abstract val arraysAssertionsClass: ClassId
    abstract val testAnnotation: String
    abstract val testAnnotationId: ClassId
    abstract val testAnnotationFqn: String
    abstract val parameterizedTestAnnotation: String
    abstract val parameterizedTestAnnotationId: ClassId
    abstract val parameterizedTestAnnotationFqn: String
    abstract val methodSourceAnnotation: String
    abstract val methodSourceAnnotationId: ClassId
    abstract val methodSourceAnnotationFqn: String
    abstract val nestedClassesShouldBeStatic: Boolean
    abstract val argListClassId: ClassId

    val assertEquals by lazy { assertionId("assertEquals", objectClassId, objectClassId) }

    val assertFloatEquals by lazy { assertionId("assertEquals", floatClassId, floatClassId, floatClassId) }

    val assertDoubleEquals by lazy { assertionId("assertEquals", doubleClassId, doubleClassId, doubleClassId) }

    val assertArrayEquals by lazy { arrayAssertionId("assertArrayEquals", Array<Any>::class.id, Array<Any>::class.id) }

    open val assertBooleanArrayEquals by lazy { assertionId("assertArrayEquals", booleanArrayClassId, booleanArrayClassId) }

    val assertByteArrayEquals by lazy { arrayAssertionId("assertArrayEquals", byteArrayClassId, byteArrayClassId) }

    val assertCharArrayEquals by lazy { arrayAssertionId("assertArrayEquals", charArrayClassId, charArrayClassId) }

    val assertShortArrayEquals by lazy { arrayAssertionId("assertArrayEquals", shortArrayClassId, shortArrayClassId) }

    val assertIntArrayEquals by lazy { arrayAssertionId("assertArrayEquals", intArrayClassId, intArrayClassId) }

    val assertLongArrayEquals by lazy { arrayAssertionId("assertArrayEquals", longArrayClassId, longArrayClassId) }

    val assertFloatArrayEquals by lazy { arrayAssertionId("assertArrayEquals", floatArrayClassId, floatArrayClassId, floatClassId) }

    val assertDoubleArrayEquals by lazy { arrayAssertionId("assertArrayEquals", doubleArrayClassId, doubleArrayClassId, doubleClassId) }

    val assertNull by lazy { assertionId("assertNull", objectClassId) }

    val assertNotNull by lazy { assertionId("assertNotNull", objectClassId) }

    val assertFalse by lazy { assertionId("assertFalse", booleanClassId) }

    val assertTrue by lazy { assertionId("assertTrue", booleanClassId) }

    val assertNotEquals by lazy { assertionId("assertNotEquals", objectClassId, objectClassId) }

    protected fun assertionId(name: String, vararg params: ClassId): MethodId =
        builtinStaticMethodId(assertionsClass, name, voidClassId, *params)
    private fun arrayAssertionId(name: String, vararg params: ClassId): MethodId =
            builtinStaticMethodId(arraysAssertionsClass, name, voidClassId, *params)

    abstract fun getRunTestsCommand(
        executionInvoke: String,
        classPath: String,
        classesNames: List<String>,
        buildDirectory: String,
        additionalArguments: List<String>
    ): List<String>

    override fun toString() = id

    // Get is mandatory because of the initialization order of the inheritors.
    // Otherwise, in some cases we could get an incorrect value, i.e. allItems = [null, JUnit5, TestNg]
    companion object : CodeGenerationSettingBox {
        override val defaultItem: TestFramework get() = Junit5
        override val allItems: List<TestFramework> get() = listOf(Junit4, Junit5, TestNg)
        val parametrizedDefaultItem: TestFramework get() = Junit5
    }
}

object TestNg : TestFramework(id = "TestNG",displayName = "TestNG") {
    override val mainPackage: String = TEST_NG_PACKAGE
    override val testAnnotation: String = "@$mainPackage.Test"
    override val testAnnotationFqn: String = "$mainPackage.Test"

    override val parameterizedTestAnnotation: String = "@$mainPackage.Test"
    override val parameterizedTestAnnotationFqn: String = "@$mainPackage.Test"
    override val methodSourceAnnotation: String = "@$mainPackage.DataProvider"
    override val methodSourceAnnotationFqn: String = "@$mainPackage.DataProvider"

    internal const val testXmlName: String = "testng.xml"

    override val assertionsClass: ClassId = BuiltinClassId(
        name = TEST_NG_ASSERTIONS,
        canonicalName = TEST_NG_ASSERTIONS,
        simpleName = "Assert"
    )

    override val arraysAssertionsClass: ClassId = BuiltinClassId(
        name = TEST_NG_ARRAYS_ASSERTIONS,
        canonicalName = TEST_NG_ARRAYS_ASSERTIONS,
        simpleName = "ArrayAsserts"
    )

    override val assertBooleanArrayEquals by lazy { assertionId("assertEquals", booleanArrayClassId, booleanArrayClassId) }

    val throwingRunnableClassId = BuiltinClassId(
        name = "${assertionsClass.name}\$ThrowingRunnable",
        canonicalName = "${assertionsClass.canonicalName}.ThrowingRunnable",
        simpleName = "ThrowingRunnable"
    )

    val assertThrows = builtinStaticMethodId(
        classId = assertionsClass,
        name = "assertThrows",
        // TODO: actually the return type is 'T extends java.lang.Throwable'
        returnType = java.lang.Throwable::class.id,
        arguments = arrayOf(
            Class::class.id,
            throwingRunnableClassId
        )
    )

    override val testAnnotationId: ClassId = BuiltinClassId(
        name = "$mainPackage.annotations.Test",
        canonicalName = "$mainPackage.annotations.Test",
        simpleName = "Test"
    )

    override val parameterizedTestAnnotationId: ClassId = BuiltinClassId(
        name = "$mainPackage.annotations.Test",
        canonicalName = "$mainPackage.annotations.Test",
        simpleName = "Test"
    )

    override val methodSourceAnnotationId: ClassId = BuiltinClassId(
        name = "$mainPackage.annotations.DataProvider",
        canonicalName = "$mainPackage.annotations.DataProvider",
        simpleName = "DataProvider"
    )

    override val nestedClassesShouldBeStatic = true

    override val argListClassId: ClassId
        get() {
            val outerArrayId = Array<Array<Any?>?>::class.id
            val innerArrayId = BuiltinClassId(
                name = objectArrayClassId.name,
                simpleName = objectArrayClassId.simpleName,
                canonicalName = objectArrayClassId.canonicalName,
                packageName = objectArrayClassId.packageName,
                elementClassId = objectClassId,
                typeParameters = TypeParameters(listOf(objectClassId))
            )

            return BuiltinClassId(
                name = outerArrayId.name,
                simpleName = outerArrayId.simpleName,
                canonicalName = outerArrayId.canonicalName,
                packageName = outerArrayId.packageName,
                elementClassId = innerArrayId,
                typeParameters = TypeParameters(listOf(innerArrayId))
            )
        }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getRunTestsCommand(
        executionInvoke: String,
        classPath: String,
        classesNames: List<String>,
        buildDirectory: String,
        additionalArguments: List<String>
    ): List<String> {
        // TestNg requires a specific xml to run with
        writeXmlFileForTestSuite(buildDirectory, classesNames)

        return buildList {
            add(executionInvoke)
            addAll(additionalArguments)
            add("$mainPackage.TestNG")
            add("$buildDirectory${File.separator}$testXmlName")
        }
    }

    private fun writeXmlFileForTestSuite(buildDirectory: String, testsNames: List<String>) {
        val packages = testsNames.map { testName ->
            constructPackageLine(testName.extractPackage())
        }

        File(buildDirectory + File.separator + testXmlName).writeText(constructTestNgXml(packages))
    }

    private fun String.extractPackage(): String = split(".").dropLast(1).joinToString(".")

    private fun constructPackageLine(pkg: String): String = "<package name=\"$pkg\"/>"

    private fun constructTestNgXml(packages: List<String>): String =
        """
    <!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd" >  
    <suite name="SuiteAll" verbose="1">
        <test name="TestAll">  
            <packages>
                ${packages.joinToString(separator = "\t\t\t\n")}
            </packages>  
        </test>     
    </suite>
    """.trimIndent()
}

object Junit4 : TestFramework(id = "JUnit4",displayName = "JUnit4") {
    private val parametrizedTestsNotSupportedError: Nothing
        get() = error("Parametrized tests are not supported for JUnit4")

    override val mainPackage: String = JUNIT4_PACKAGE
    override val testAnnotation = "@$mainPackage.Test"
    override val testAnnotationFqn: String = "$mainPackage.Test"

    override val parameterizedTestAnnotation
        get() = parametrizedTestsNotSupportedError
    override val parameterizedTestAnnotationFqn
        get() = parametrizedTestsNotSupportedError
    override val methodSourceAnnotation
        get() = parametrizedTestsNotSupportedError
    override val methodSourceAnnotationFqn
        get() = parametrizedTestsNotSupportedError

    override val testAnnotationId = BuiltinClassId(
        name = "$JUNIT4_PACKAGE.Test",
        canonicalName = "$JUNIT4_PACKAGE.Test",
        simpleName = "Test"
    )

    override val parameterizedTestAnnotationId = voidClassId
    override val methodSourceAnnotationId = voidClassId

    val runWithAnnotationClassId = BuiltinClassId(
        name = "$JUNIT4_PACKAGE.runner.RunWith",
        canonicalName = "$JUNIT4_PACKAGE.runner.RunWith",
        simpleName = "RunWith"
    )

    override val assertionsClass = BuiltinClassId(
        name = JUNIT4_ASSERTIONS,
        canonicalName = JUNIT4_ASSERTIONS,
        simpleName = "Assert"
    )
    override val arraysAssertionsClass = assertionsClass

    val ignoreAnnotationClassId = with("$JUNIT4_PACKAGE.Ignore") {
        BuiltinClassId(
            name = this,
            canonicalName = this,
            simpleName = "Ignore"
        )
    }

    val enclosedClassId = BuiltinClassId(
        name = "org.junit.experimental.runners.Enclosed",
        canonicalName = "org.junit.experimental.runners.Enclosed",
        simpleName = "Enclosed"
    )

    override val nestedClassesShouldBeStatic = true

    override val argListClassId: ClassId
        get() = parametrizedTestsNotSupportedError

    @OptIn(ExperimentalStdlibApi::class)
    override fun getRunTestsCommand(
        executionInvoke: String,
        classPath: String,
        classesNames: List<String>,
        buildDirectory: String,
        additionalArguments: List<String>
    ): List<String> = buildList {
        add(executionInvoke)
        addAll(additionalArguments)
        add("$mainPackage.runner.JUnitCore")
        addAll(classesNames)
    }
}

object Junit5 : TestFramework(id = "JUnit5", displayName = "JUnit5") {
    override val mainPackage: String = JUNIT5_PACKAGE
    override val testAnnotation = "@$mainPackage.Test"
    override val testAnnotationFqn: String = "$mainPackage.Test"
    override val parameterizedTestAnnotation = "$JUNIT5_PARAMETERIZED_PACKAGE.ParameterizedTest"
    override val parameterizedTestAnnotationFqn: String = "$JUNIT5_PARAMETERIZED_PACKAGE.ParameterizedTest"
    override val methodSourceAnnotation: String = "$JUNIT5_PARAMETERIZED_PACKAGE.provider.MethodSource"
    override val methodSourceAnnotationFqn: String = "$JUNIT5_PARAMETERIZED_PACKAGE.provider.MethodSource"

    val executableClassId = BuiltinClassId(
        name = "$JUNIT5_PACKAGE.function.Executable",
        canonicalName = "$JUNIT5_PACKAGE.function.Executable",
        simpleName = "Executable"
    )

    val timeoutClassId = BuiltinClassId(
        name = "$JUNIT5_PACKAGE.Timeout",
        canonicalName = "$JUNIT5_PACKAGE.Timeout",
        simpleName = "Timeout"
    )

    val timeunitClassId = BuiltinClassId(
        name = "TimeUnit",
        canonicalName = "java.util.concurrent.TimeUnit",
        simpleName = "TimeUnit"
    )

    val durationClassId = BuiltinClassId(
        name = "Duration",
        canonicalName = "java.time.Duration",
        simpleName = "Duration"
    )

    val ofMillis = builtinStaticMethodId(
        classId = durationClassId,
        name = "ofMillis",
        returnType = durationClassId,
        arguments = arrayOf(longClassId)
    )

    val nestedTestClassAnnotationId = BuiltinClassId(
        name = "$JUNIT5_PACKAGE.Nested",
        canonicalName = "$JUNIT5_PACKAGE.Nested",
        simpleName = "Nested"
    )

    override val testAnnotationId = BuiltinClassId(
        name = "$JUNIT5_PACKAGE.Test",
        canonicalName = "$JUNIT5_PACKAGE.Test",
        simpleName = "Test"
    )

    override val parameterizedTestAnnotationId = BuiltinClassId(
        name = "$JUNIT5_PARAMETERIZED_PACKAGE.ParameterizedTest",
        canonicalName = "$JUNIT5_PARAMETERIZED_PACKAGE.ParameterizedTest",
        simpleName = "ParameterizedTest"
    )

    override val methodSourceAnnotationId: ClassId = BuiltinClassId(
        name = "$JUNIT5_PARAMETERIZED_PACKAGE.provider.MethodSource",
        canonicalName = "$JUNIT5_PARAMETERIZED_PACKAGE.provider.MethodSource",
        simpleName = "MethodSource"
    )

    override val assertionsClass = BuiltinClassId(
        name = JUNIT5_ASSERTIONS,
        canonicalName = JUNIT5_ASSERTIONS,
        simpleName = "Assertions"
    )

    override val arraysAssertionsClass = assertionsClass

    val assertThrows = builtinStaticMethodId(
        classId = assertionsClass,
        name = "assertThrows",
        // TODO: actually the return type is 'T extends java.lang.Throwable'
        returnType = java.lang.Throwable::class.id,
        arguments = arrayOf(
            Class::class.id,
            executableClassId
        )
    )

    val assertTimeoutPreemptively = builtinStaticMethodId(
        classId = assertionsClass,
        name = "assertTimeoutPreemptively",
        returnType = voidWrapperClassId,
        arguments = arrayOf(
            durationClassId,
            executableClassId
        )
    )

    val displayNameClassId = BuiltinClassId(
        name = "$JUNIT5_PACKAGE.DisplayName",
        canonicalName = "$JUNIT5_PACKAGE.DisplayName",
        simpleName = "DisplayName"
    )

    val disabledAnnotationClassId = with("$JUNIT5_PACKAGE.Disabled") {
        BuiltinClassId(
            name = this,
            canonicalName = this,
            simpleName = "Disabled"
        )
    }

    override val nestedClassesShouldBeStatic = false

    override val argListClassId: ClassId
        get() {
            val arrayListId = java.util.ArrayList::class.id
            return BuiltinClassId(
                name = arrayListId.name,
                simpleName = arrayListId.simpleName,
                canonicalName = arrayListId.canonicalName,
                packageName = arrayListId.packageName,
                typeParameters = TypeParameters(listOf(argumentsClassId))
            )
        }

    private const val junitVersion = "1.9.0" // TODO read it from gradle.properties
    private const val platformJarName: String = "junit-platform-console-standalone-$junitVersion.jar"

    @OptIn(ExperimentalStdlibApi::class)
    override fun getRunTestsCommand(
        executionInvoke: String,
        classPath: String,
        classesNames: List<String>,
        buildDirectory: String,
        additionalArguments: List<String>
    ): List<String> = buildList {
        add(executionInvoke)
        addAll(additionalArguments)
        add("-jar")
        add(classPath.split(File.pathSeparator).single { platformJarName in it })
        add(isolateCommandLineArgumentsToArgumentFile(listOf("-cp", classPath).plus(classesNames.map { "-c=$it" })))
    }
}

enum class RuntimeExceptionTestsBehaviour(
    override val id: String,
    override val displayName: String,
    override val description: String
) : CodeGenerationSettingItem {
    PASS(
        id = "Passing",
        displayName = "Pass",
        description = "Tests that produce Runtime exceptions should pass (by inserting throwable assertion)"
    ),
    FAIL(
        id = "Failing",
        displayName = "Fail",
        description = "Tests that produce Runtime exceptions should fail" +
                "(WARNING!: failing tests may appear in testing class)"
    );

    override fun toString(): String = id

    // Get is mandatory because of the initialization order of the inheritors.
    // Otherwise, in some cases we could get an incorrect value
    companion object : CodeGenerationSettingBox {
        override val defaultItem: RuntimeExceptionTestsBehaviour get() = FAIL
        override val allItems: List<RuntimeExceptionTestsBehaviour> = values().toList()
    }
}

data class HangingTestsTimeout(val timeoutMs: Long) {
    constructor() : this(DEFAULT_TIMEOUT_MS)

    companion object {
        const val DEFAULT_TIMEOUT_MS = DEFAULT_CONCRETE_EXECUTION_TIMEOUT_IN_CHILD_PROCESS_MS
        const val MIN_TIMEOUT_MS = 100L
        const val MAX_TIMEOUT_MS = 1_000_000L
    }
}

enum class ForceStaticMocking(
    override val id: String,
    override val displayName: String,
    override val description: String,
    val warningMessage: List<String>,
) : CodeGenerationSettingItem {
    FORCE(
        id = "Force static mocking",
        displayName = "Force static mocking",
        description = "Use mocks for static methods and constructors invocations even if static mocking is disabled" +
                "(WARNING!: can add imports from missing dependencies)",
        warningMessage = listOf(
            """WARNING!!! Automatically used "${StaticsMocking.defaultItem}" framework for mocking statics""",
            "because execution encountered flaky methods",
            "To change this behaviour edit [Settings -> UtBot -> Force static mocking]"
        )
    ),
    DO_NOT_FORCE(
        id = "Do not force static mocking",
        displayName = "Do not force static mocking",
        description = "Do not force static mocking if static mocking setting is disabled" +
                "(WARNING!: flaky tests can appear)",
        warningMessage = listOf(
            "Warning!!! This test can be flaky because execution encountered flaky methods,",
            """but no "static mocking" was selected"""
        )
    );

    override fun toString(): String = id

    // Get is mandatory because of the initialization order of the inheritors.
    // Otherwise, in some cases we could get an incorrect value
    companion object : CodeGenerationSettingBox {
        override val defaultItem: ForceStaticMocking get() = FORCE
        override val allItems: List<ForceStaticMocking> = values().toList()
    }
}

enum class ParametrizedTestSource(
    override val id: String,
    override val displayName: String,
    override val description: String = "Use $displayName for parametrized tests"
) : CodeGenerationSettingItem {
    DO_NOT_PARAMETRIZE(
        id = "Not parametrized",
        displayName = "Not parametrized",
        description = "Do not generate parametrized tests"
    ),
    PARAMETRIZE(
        id = "Parametrized",
        displayName = "Parametrized",
        description = "Generate parametrized tests"
    );

    override fun toString(): String = id

    // Get is mandatory because of the initialization order of the inheritors.
    // Otherwise, in some cases we could get an incorrect value
    companion object : CodeGenerationSettingBox {
        override val defaultItem: ParametrizedTestSource = DO_NOT_PARAMETRIZE
        override val allItems: List<ParametrizedTestSource> = values().toList()
    }
}
