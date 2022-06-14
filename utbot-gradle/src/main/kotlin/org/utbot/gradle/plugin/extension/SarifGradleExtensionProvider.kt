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
import org.utbot.framework.plugin.sarif.SarifExtensionProvider
import java.io.File

/**
 * Provides all [SarifGradleExtension] fields in a convenient form:
 * Defines default values and a transform function for these fields.
 */
class SarifGradleExtensionProvider(
    private val project: Project,
    private val extension: SarifGradleExtension
) : SarifExtensionProvider {

    override val targetClasses: List<String>
        get() = extension.targetClasses
            .getOrElse(listOf())

    override val projectRoot: File
        get() = extension.projectRoot.orNull
            ?.toPath()?.toFile()
            ?: project.projectDir

    override val generatedTestsRelativeRoot: String
        get() = extension.generatedTestsRelativeRoot.orNull
            ?: "build/generated/test"

    override val sarifReportsRelativeRoot: String
        get() = extension.sarifReportsRelativeRoot.orNull
            ?: "build/generated/sarif"

    override val markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean
        get() = extension.markGeneratedTestsDirectoryAsTestSourcesRoot.orNull
            ?: true

    override val testFramework: TestFramework
        get() = extension.testFramework
            .map(::testFrameworkParse)
            .getOrElse(TestFramework.defaultItem)

    override val mockFramework: MockFramework
        get() = extension.mockFramework
            .map(::mockFrameworkParse)
            .getOrElse(MockFramework.defaultItem)

    override val generationTimeout: Long
        get() = extension.generationTimeout
            .map(::generationTimeoutParse)
            .getOrElse(60 * 1000L) // 60 seconds

    override val codegenLanguage: CodegenLanguage
        get() = extension.codegenLanguage
            .map(::codegenLanguageParse)
            .getOrElse(CodegenLanguage.defaultItem)

    override val mockStrategy: MockStrategyApi
        get() = extension.mockStrategy
            .map(::mockStrategyParse)
            .getOrElse(MockStrategyApi.defaultItem)

    override val staticsMocking: StaticsMocking
        get() = extension.staticsMocking
            .map(::staticsMockingParse)
            .getOrElse(StaticsMocking.defaultItem)

    override val forceStaticMocking: ForceStaticMocking
        get() = extension.forceStaticMocking
            .map(::forceStaticMockingParse)
            .getOrElse(ForceStaticMocking.defaultItem)

    override val classesToMockAlways: Set<ClassId>
        get() = classesToMockAlwaysParse(
            extension.classesToMockAlways.getOrElse(listOf())
        )
}
