package org.utbot.maven.plugin

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
import org.utbot.gradle.plugin.extension.SarifExtensionProvider
import java.io.File

class SarifMavenConfigurationProvider(
    private val createSarifReportMojo: CreateSarifReportMojo
) : SarifExtensionProvider {

    override val targetClasses: List<String>
        get() = createSarifReportMojo.targetClasses

    override val projectRoot: File
        get() = createSarifReportMojo.projectRoot

    override val generatedTestsRelativeRoot: String
        get() = createSarifReportMojo.generatedTestsRelativeRoot

    override val sarifReportsRelativeRoot: String
        get() = createSarifReportMojo.sarifReportsRelativeRoot

    override val markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean
        get() = createSarifReportMojo.markGeneratedTestsDirectoryAsTestSourcesRoot

    override val testFramework: TestFramework
        get() = testFrameworkParse(createSarifReportMojo.testFramework)

    override val mockFramework: MockFramework
        get() = mockFrameworkParse(createSarifReportMojo.mockFramework)

    override val generationTimeout: Long
        get() = generationTimeoutParse(createSarifReportMojo.generationTimeout)

    override val codegenLanguage: CodegenLanguage
        get() = codegenLanguageParse(createSarifReportMojo.codegenLanguage)

    override val mockStrategy: MockStrategyApi
        get() = mockStrategyParse(createSarifReportMojo.mockStrategy)

    override val staticsMocking: StaticsMocking
        get() = staticsMockingParse(createSarifReportMojo.staticsMocking)

    override val forceStaticMocking: ForceStaticMocking
        get() = forceStaticMockingParse(createSarifReportMojo.forceStaticMocking)

    override val classesToMockAlways: Set<ClassId>
        get() = classesToMockAlwaysParse(createSarifReportMojo.classesToMockAlways)
}