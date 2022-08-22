package org.utbot.framework.plugin.sarif

import org.utbot.engine.Mocker
import org.utbot.framework.codegen.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import java.io.File

/**
 * Provides fields needed to create a SARIF report.
 * Defines transform function for these fields.
 */
interface SarifExtensionProvider {

    /**
    * Classes for which the SARIF report will be created.
    */
    val targetClasses: List<String>

    /**
     * Absolute path to the root of the relative paths in the SARIF report.
     */
    val projectRoot: File

    /**
     * Relative path to the root of the generated tests.
     */
    val generatedTestsRelativeRoot: String

    /**
     * Relative path to the root of the SARIF reports.
     */
    val sarifReportsRelativeRoot: String

    /**
     * Mark the directory with generated tests as `test sources root` or not.
     */
    val markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean

    /**
     * Generate tests for private methods or not.
     */
    val testPrivateMethods: Boolean

    val testFramework: TestFramework

    val mockFramework: MockFramework

    /**
     * Maximum tests generation time for one class (in milliseconds).
     */
    val generationTimeout: Long

    val codegenLanguage: CodegenLanguage

    val mockStrategy: MockStrategyApi

    val staticsMocking: StaticsMocking

    val forceStaticMocking: ForceStaticMocking

    /**
     * Classes to force mocking theirs static methods and constructors.
     * Contains user-specified classes and `Mocker.defaultSuperClassesToMockAlwaysNames`.
     */
    val classesToMockAlways: Set<ClassId>

    // transform functions

    fun testFrameworkParse(testFramework: String): TestFramework =
        when (testFramework.toLowerCase()) {
            "junit4" -> Junit4
            "junit5" -> Junit5
            "testng" -> TestNg
            else -> error("Parameter testFramework == '$testFramework', but it can take only 'junit4', 'junit5' or 'testng'")
        }

    fun mockFrameworkParse(mockFramework: String): MockFramework =
        when (mockFramework.toLowerCase()) {
            "mockito" -> MockFramework.MOCKITO
            else -> error("Parameter mockFramework == '$mockFramework', but it can take only 'mockito'")
        }

    fun generationTimeoutParse(generationTimeout: Long): Long {
        if (generationTimeout < 0)
            error("Parameter generationTimeout == $generationTimeout, but it should be non-negative")
        return generationTimeout
    }

    fun codegenLanguageParse(codegenLanguage: String): CodegenLanguage =
        when (codegenLanguage.toLowerCase()) {
            "java" -> CodegenLanguage.JAVA
            "kotlin" -> CodegenLanguage.KOTLIN
            else -> error("Parameter codegenLanguage == '$codegenLanguage', but it can take only 'java' or 'kotlin'")
        }

    fun mockStrategyParse(mockStrategy: String): MockStrategyApi =
        when (mockStrategy.toLowerCase()) {
            "no-mocks" -> MockStrategyApi.NO_MOCKS
            "other-packages" -> MockStrategyApi.OTHER_PACKAGES
            "other-classes" -> MockStrategyApi.OTHER_CLASSES
            else -> error("Parameter mockStrategy == '$mockStrategy', but it can take only 'no-mocks', 'other-packages' or 'other-classes'")
        }

    fun staticsMockingParse(staticsMocking: String): StaticsMocking =
        when (staticsMocking.toLowerCase()) {
            "do-not-mock-statics" -> NoStaticMocking
            "mock-statics" -> MockitoStaticMocking
            else -> error("Parameter staticsMocking == '$staticsMocking', but it can take only 'do-not-mock-statics' or 'mock-statics'")
        }

    fun forceStaticMockingParse(forceStaticMocking: String): ForceStaticMocking =
        when (forceStaticMocking.toLowerCase()) {
            "force" -> ForceStaticMocking.FORCE
            "do-not-force" -> ForceStaticMocking.DO_NOT_FORCE
            else -> error("Parameter forceStaticMocking == '$forceStaticMocking', but it can take only 'force' or 'do-not-force'")
        }

    fun classesToMockAlwaysParse(specifiedClasses: List<String>): Set<ClassId> =
        (Mocker.defaultSuperClassesToMockAlwaysNames + specifiedClasses).map { className ->
            ClassId(className)
        }.toSet()
}