package org.utbot.sarif.extension

import org.utbot.common.PathUtil.toPath
import org.utbot.engine.Mocker
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.Junit4
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.MockitoStaticMocking
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import java.io.File
import org.gradle.api.Project

/**
 * Provides all [SarifGradleExtension] fields in a convenient form:
 * Defines default values and a transform function for these fields.
 */
class SarifGradleExtensionProvider(
    private val project: Project,
    private val extension: SarifGradleExtension
) {

    /**
     * Classes for which the SARIF report will be created.
     */
    val targetClasses: List<String>
        get() = extension.targetClasses
            .getOrElse(listOf())

    /**
     * Absolute path to the root of the relative paths in the SARIF report.
     */
    val projectRoot: File
        get() = extension.projectRoot.orNull
            ?.toPath()?.toFile()
            ?: project.projectDir

    /**
     * Relative path to the root of the generated tests.
     */
    val generatedTestsRelativeRoot: String
        get() = extension.generatedTestsRelativeRoot.orNull
            ?: "build/generated/test"

    /**
     * Relative path to the root of the SARIF reports.
     */
    val sarifReportsRelativeRoot: String
        get() = extension.sarifReportsRelativeRoot.orNull
            ?: "build/generated/sarif"

    /**
     * Mark the directory with generated tests as `test sources root` or not.
     */
    val markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean
        get() = extension.markGeneratedTestsDirectoryAsTestSourcesRoot.orNull
            ?: true

    val testFramework: TestFramework
        get() = extension.testFramework
            .map(::testFrameworkParse)
            .getOrElse(TestFramework.defaultItem)

    val mockFramework: MockFramework
        get() = extension.mockFramework
            .map(::mockFrameworkParse)
            .getOrElse(MockFramework.defaultItem)

    /**
     * Maximum tests generation time for one class (in milliseconds).
     */
    val generationTimeout: Long
        get() = extension.generationTimeout
            .map(::generationTimeoutParse)
            .getOrElse(60 * 1000L) // 60 seconds

    val codegenLanguage: CodegenLanguage
        get() = extension.codegenLanguage
            .map(::codegenLanguageParse)
            .getOrElse(CodegenLanguage.defaultItem)

    val mockStrategy: MockStrategyApi
        get() = extension.mockStrategy
            .map(::mockStrategyParse)
            .getOrElse(MockStrategyApi.defaultItem)

    val staticsMocking: StaticsMocking
        get() = extension.staticsMocking
            .map(::staticsMockingParse)
            .getOrElse(StaticsMocking.defaultItem)

    val forceStaticMocking: ForceStaticMocking
        get() = extension.forceStaticMocking
            .map(::forceStaticMockingParse)
            .getOrElse(ForceStaticMocking.defaultItem)

    /**
     * Contains user-specified classes and `Mocker.defaultSuperClassesToMockAlwaysNames`.
     */
    val classesToMockAlways: Set<ClassId>
        get() {
            val defaultClasses = Mocker.defaultSuperClassesToMockAlwaysNames
            val specifiedClasses = extension.classesToMockAlways.getOrElse(listOf())
            return (defaultClasses + specifiedClasses).map { className ->
                ClassId(className)
            }.toSet()
        }

    // transform functions

    private fun testFrameworkParse(testFramework: String): TestFramework =
        when (testFramework.toLowerCase()) {
            "junit4" -> Junit4
            "junit5" -> Junit5
            "testng" -> TestNg
            else -> error("Parameter testFramework == '$testFramework', but it can take only 'junit4', 'junit5' or 'testng'")
        }

    private fun mockFrameworkParse(mockFramework: String): MockFramework =
        when (mockFramework.toLowerCase()) {
            "mockito" -> MockFramework.MOCKITO
            else -> error("Parameter mockFramework == '$mockFramework', but it can take only 'mockito'")
        }

    private fun generationTimeoutParse(generationTimeout: Long): Long {
        if (generationTimeout < 0)
            error("Parameter generationTimeout == $generationTimeout, but it should be non-negative")
        return generationTimeout
    }

    private fun codegenLanguageParse(codegenLanguage: String): CodegenLanguage =
        when (codegenLanguage.toLowerCase()) {
            "java" -> CodegenLanguage.JAVA
            "kotlin" -> CodegenLanguage.KOTLIN
            else -> error("Parameter codegenLanguage == '$codegenLanguage', but it can take only 'java' or 'kotlin'")
        }

    private fun mockStrategyParse(mockStrategy: String): MockStrategyApi =
        when (mockStrategy.toLowerCase()) {
            "do-not-mock" -> MockStrategyApi.NO_MOCKS
            "package-based" -> MockStrategyApi.OTHER_PACKAGES
            "all-except-cut" -> MockStrategyApi.OTHER_CLASSES
            else -> error("Parameter mockStrategy == '$mockStrategy', but it can take only 'do-not-mock', 'package-based' or 'all-except-cut'")
        }

    private fun staticsMockingParse(staticsMocking: String): StaticsMocking =
        when (staticsMocking.toLowerCase()) {
            "do-not-mock-statics" -> NoStaticMocking
            "mock-statics" -> MockitoStaticMocking
            else -> error("Parameter staticsMocking == '$staticsMocking', but it can take only 'do-not-mock-statics' or 'mock-statics'")
        }

    private fun forceStaticMockingParse(forceStaticMocking: String): ForceStaticMocking =
        when (forceStaticMocking.toLowerCase()) {
            "force" -> ForceStaticMocking.FORCE
            "do-not-force" -> ForceStaticMocking.DO_NOT_FORCE
            else -> error("Parameter forceStaticMocking == '$forceStaticMocking', but it can take only 'force' or 'do-not-force'")
        }
}
