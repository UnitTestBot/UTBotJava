package org.utbot.gradle.plugin.extension

import org.gradle.api.Project
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.gradle.plugin.extension.SarifConfigurationParser.classesToMockAlwaysParse
import org.utbot.gradle.plugin.extension.SarifConfigurationParser.codegenLanguageParse
import org.utbot.gradle.plugin.extension.SarifConfigurationParser.forceStaticMockingParse
import org.utbot.gradle.plugin.extension.SarifConfigurationParser.generationTimeoutParse
import org.utbot.gradle.plugin.extension.SarifConfigurationParser.mockFrameworkParse
import org.utbot.gradle.plugin.extension.SarifConfigurationParser.mockStrategyParse
import org.utbot.gradle.plugin.extension.SarifConfigurationParser.staticsMockingParse
import org.utbot.gradle.plugin.extension.SarifConfigurationParser.testFrameworkParse
import java.io.File

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
        get() = classesToMockAlwaysParse(
            extension.classesToMockAlways.getOrElse(listOf())
        )
}
