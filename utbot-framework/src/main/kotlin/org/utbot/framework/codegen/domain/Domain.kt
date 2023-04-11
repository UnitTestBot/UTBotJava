package org.utbot.framework.codegen.domain

import org.utbot.framework.DEFAULT_EXECUTION_TIMEOUT_IN_INSTRUMENTED_PROCESS_MS
import org.utbot.framework.codegen.domain.builtin.mockitoClassId
import org.utbot.framework.codegen.domain.builtin.ongoingStubbingClassId
import org.utbot.framework.codegen.domain.models.CgClassId
import org.utbot.framework.codegen.tree.argumentsClassId
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodeGenerationSettingBox
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.idOrNull
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
import org.utbot.framework.plugin.api.util.voidWrapperClassId

data class TestClassFile(val packageName: String, val imports: List<Import>, val testClass: String)

abstract class Import(val order: Int) : Comparable<Import> {
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
        canonicalName = "org.mockito.MockedStatic",
        simpleName = "MockedStatic"
    )

    val mockedConstructionClassId = BuiltinClassId(
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

abstract class TestFramework(
    override val id: String,
    override val displayName: String,
    override val description: String = "Use $displayName as test framework",
) : CodeGenerationSettingItem {
    var isParametrizedTestsConfigured = false
    var isInstalled: Boolean = false
    abstract val mainPackage: String
    abstract val assertionsClass: ClassId
    abstract val arraysAssertionsClass: ClassId
    abstract val kotlinFailureAssertionsClass: ClassId
    abstract val testAnnotation: String
    abstract val testAnnotationId: ClassId
    abstract val testAnnotationFqn: String
    abstract val beforeMethod: String
    abstract val beforeMethodId: ClassId
    abstract val beforeMethodFqn: String
    abstract val afterMethod: String
    abstract val afterMethodId: ClassId
    abstract val afterMethodFqn: String
    abstract val parameterizedTestAnnotation: String
    abstract val parameterizedTestAnnotationId: ClassId
    abstract val parameterizedTestAnnotationFqn: String
    abstract val methodSourceAnnotation: String
    abstract val methodSourceAnnotationId: ClassId
    abstract val methodSourceAnnotationFqn: String
    abstract val nestedClassesShouldBeStatic: Boolean
    abstract val argListClassId: ClassId

    open val testSuperClass: ClassId? = null

    open val assertEquals by lazy { assertionId("assertEquals", objectClassId, objectClassId) }

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

    val fail by lazy { assertionId("fail", objectClassId) }

    val kotlinFail by lazy { kotlinFailAssertionId("fail", objectClassId) }

    protected open fun assertionId(name: String, vararg params: ClassId): MethodId =
        builtinStaticMethodId(assertionsClass, name, voidClassId, *params)

    private fun arrayAssertionId(name: String, vararg params: ClassId): MethodId =
        builtinStaticMethodId(arraysAssertionsClass, name, voidClassId, *params)

    private fun kotlinFailAssertionId(name: String, vararg params: ClassId): MethodId =
        builtinStaticMethodId(kotlinFailureAssertionsClass, name, voidClassId, *params)

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
    override val testAnnotationId: ClassId = BuiltinClassId(
        canonicalName = "$mainPackage.annotations.Test",
        simpleName = "Test"
    )

    override val beforeMethod = "@${mainPackage}.BeforeMethod"
    override val beforeMethodFqn = "${mainPackage}.BeforeMethod"
    override val beforeMethodId = BuiltinClassId(
        canonicalName = "${mainPackage}.BeforeMethod",
        simpleName = "BeforeMethod"
    )

    override val afterMethod = "@${mainPackage}.AfterMethod"
    override val afterMethodFqn = "${mainPackage}.AfterMethod"
    override val afterMethodId = BuiltinClassId(
        canonicalName = "${mainPackage}.AfterMethod",
        simpleName = "AfterMethod"
    )

    override val parameterizedTestAnnotation: String = "@$mainPackage.Test"
    override val parameterizedTestAnnotationFqn: String = "@$mainPackage.Test"
    override val methodSourceAnnotation: String = "@$mainPackage.DataProvider"
    override val methodSourceAnnotationFqn: String = "@$mainPackage.DataProvider"

    internal const val testXmlName: String = "testng.xml"

    override val assertionsClass: ClassId = BuiltinClassId(
        canonicalName = TEST_NG_ASSERTIONS,
        simpleName = "Assert"
    )

    override val arraysAssertionsClass: ClassId = BuiltinClassId(
        canonicalName = TEST_NG_ARRAYS_ASSERTIONS,
        simpleName = "ArrayAsserts"
    )

    override val kotlinFailureAssertionsClass = assertionsClass

    override val assertBooleanArrayEquals by lazy { assertionId("assertEquals", booleanArrayClassId, booleanArrayClassId) }

    val throwingRunnableClassId = BuiltinClassId(
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

    override val parameterizedTestAnnotationId: ClassId = BuiltinClassId(
        canonicalName = "$mainPackage.annotations.Test",
        simpleName = "Test",
    )

    override val methodSourceAnnotationId: ClassId = BuiltinClassId(
        canonicalName = "$mainPackage.annotations.DataProvider",
        simpleName = "DataProvider"
    )

    override val nestedClassesShouldBeStatic = true

    override val argListClassId: ClassId
        get() = Array<Array<Any?>?>::class.id

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

object Junit4 : TestFramework(id = "JUnit4",displayName = "JUnit 4") {
    private val parametrizedTestsNotSupportedError: Nothing
        get() = error("Parametrized tests are not supported for JUnit 4")

    override val mainPackage: String = JUNIT4_PACKAGE

    override val testAnnotation = "@$mainPackage.Test"
    override val testAnnotationFqn: String = "$mainPackage.Test"
    override val testAnnotationId = BuiltinClassId(
        canonicalName = "$mainPackage.Test",
        simpleName = "Test"
    )

    override val beforeMethod = "@$mainPackage.Before"
    override val beforeMethodFqn = "$mainPackage.Before"
    override val beforeMethodId = BuiltinClassId(
        canonicalName = "$mainPackage.Before",
        simpleName = "Before"
    )

    override val afterMethod = "@$mainPackage.After"
    override val afterMethodFqn = "$mainPackage.After"
    override val afterMethodId = BuiltinClassId(
        canonicalName = "$mainPackage.After",
        simpleName = "After"
    )

    override val parameterizedTestAnnotation
        get() = parametrizedTestsNotSupportedError
    override val parameterizedTestAnnotationFqn
        get() = parametrizedTestsNotSupportedError
    override val methodSourceAnnotation
        get() = parametrizedTestsNotSupportedError
    override val methodSourceAnnotationFqn
        get() = parametrizedTestsNotSupportedError

    override val parameterizedTestAnnotationId = voidClassId
    override val methodSourceAnnotationId = voidClassId

    val runWithAnnotationClassId = BuiltinClassId(
        canonicalName = "$JUNIT4_PACKAGE.runner.RunWith",
        simpleName = "RunWith"
    )

    override val assertionsClass = BuiltinClassId(
        canonicalName = JUNIT4_ASSERTIONS,
        simpleName = "Assert"
    )
    override val arraysAssertionsClass = assertionsClass
    override val kotlinFailureAssertionsClass = assertionsClass

    val ignoreAnnotationClassId = with("$JUNIT4_PACKAGE.Ignore") {
        BuiltinClassId(
            canonicalName = this,
            simpleName = "Ignore"
        )
    }

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

object Junit5 : TestFramework(id = "JUnit5", displayName = "JUnit 5") {
    override val mainPackage: String = JUNIT5_PACKAGE

    override val testAnnotation = "@$mainPackage.Test"
    override val testAnnotationFqn: String = "$mainPackage.Test"
    override val testAnnotationId = BuiltinClassId(
        canonicalName = "$JUNIT5_PACKAGE.Test",
        simpleName = "Test"
    )

    override val beforeMethod = "@${mainPackage}.BeforeEach"
    override val beforeMethodFqn = "${mainPackage}.BeforeEach"
    override val beforeMethodId = BuiltinClassId(
        canonicalName = "${mainPackage}.BeforeEach",
        simpleName = "BeforeEach"
    )

    override val afterMethod = "@${mainPackage}.AfterEach"
    override val afterMethodFqn = "${mainPackage}.AfterEach"
    override val afterMethodId = BuiltinClassId(
        canonicalName = "${mainPackage}.AfterEach",
        simpleName = "AfterEach"
    )

    override val parameterizedTestAnnotation = "$JUNIT5_PARAMETERIZED_PACKAGE.ParameterizedTest"
    override val parameterizedTestAnnotationFqn: String = "$JUNIT5_PARAMETERIZED_PACKAGE.ParameterizedTest"
    override val methodSourceAnnotation: String = "$JUNIT5_PARAMETERIZED_PACKAGE.provider.MethodSource"
    override val methodSourceAnnotationFqn: String = "$JUNIT5_PARAMETERIZED_PACKAGE.provider.MethodSource"

    val executableClassId = BuiltinClassId(
        canonicalName = "$JUNIT5_PACKAGE.function.Executable",
        simpleName = "Executable"
    )

    val timeoutClassId = BuiltinClassId(
        canonicalName = "$JUNIT5_PACKAGE.Timeout",
        simpleName = "Timeout"
    )

    val timeunitClassId = BuiltinClassId(
        canonicalName = "java.util.concurrent.TimeUnit",
        simpleName = "TimeUnit"
    )

    val durationClassId = BuiltinClassId(
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
        canonicalName = "$JUNIT5_PACKAGE.Nested",
        simpleName = "Nested"
    )

    override val parameterizedTestAnnotationId = BuiltinClassId(
        canonicalName = "$JUNIT5_PARAMETERIZED_PACKAGE.ParameterizedTest",
        simpleName = "ParameterizedTest"
    )

    override val methodSourceAnnotationId: ClassId = BuiltinClassId(
        canonicalName = "$JUNIT5_PARAMETERIZED_PACKAGE.provider.MethodSource",
        simpleName = "MethodSource"
    )

    override val assertionsClass = BuiltinClassId(
        canonicalName = JUNIT5_ASSERTIONS,
        simpleName = "Assertions"
    )

    override val arraysAssertionsClass = assertionsClass

    override val kotlinFailureAssertionsClass = BuiltinClassId(
        canonicalName = "org.junit.jupiter.api",
        simpleName = "Assertions"
    )

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
        canonicalName = "$JUNIT5_PACKAGE.DisplayName",
        simpleName = "DisplayName"
    )

    val disabledAnnotationClassId = with("$JUNIT5_PACKAGE.Disabled") {
        BuiltinClassId(
            canonicalName = this,
            simpleName = "Disabled"
        )
    }

    override val nestedClassesShouldBeStatic = false

    override val argListClassId: ClassId
        get() {
            val arrayListId = java.util.ArrayList::class.id
            return BuiltinClassId(
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
        const val DEFAULT_TIMEOUT_MS = DEFAULT_EXECUTION_TIMEOUT_IN_INSTRUMENTED_PROCESS_MS
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

enum class ProjectType {
    /**
     * Standard JVM project without DI frameworks
     */
    PureJvm,

    /**
     * Spring or Spring Boot project
     */
    Spring,

    /**
     * Python project
     */
    Python,

    /**
     * JavaScript project
     */
    JavaScript,
}

sealed class TypeReplacementApproach {
    /**
     * Do not replace interfaces and abstract classes with concrete implementors.
     * Use mocking instead of it.
     */
    object DoNotReplace : TypeReplacementApproach()

    /**
     * Try to replace interfaces and abstract classes with concrete implementors
     * obtained from bean definitions.
     * If it is impossible, use mocking.
     *
     * Currently used in Spring applications only.
     */
    class ReplaceIfPossible(val config: String) : TypeReplacementApproach()
}

abstract class DependencyInjectionFramework(
    override val id: String,
    override val displayName: String,
    override val description: String = "Use $displayName as dependency injection framework",
) : CodeGenerationSettingItem {
    var isInstalled = false

    companion object : CodeGenerationSettingBox {
        override val defaultItem: DependencyInjectionFramework get() = SpringBoot
        override val allItems: List<DependencyInjectionFramework> get() = listOf(SpringBoot, SpringBeans)
    }
}

object SpringBeans : DependencyInjectionFramework(
    id = "spring-beans",
    displayName = "Spring Beans"
)

object SpringBoot : DependencyInjectionFramework(
    id = "spring-boot",
    displayName = "Spring Boot"
)

/**
 * Extended id of [UtModel], unique for whole test set.
 *
 * Allows distinguishing models from different executions and test sets,
 * even if they have the same value of `UtModel.id` that is allowed.
 */
data class ModelId private constructor(
    private val id: Int?,
    private val executionId: Int,
    private val testSetId: Int,
) {
    companion object {
        fun create(model: UtModel, executionId: Int = -1, testSetId: Int = -1) = ModelId(model.idOrNull(), executionId, testSetId)
    }
}

