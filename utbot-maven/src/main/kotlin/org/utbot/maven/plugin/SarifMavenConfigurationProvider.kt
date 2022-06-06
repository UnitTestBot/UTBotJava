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
import java.io.File

class SarifMavenConfigurationProvider(
    private val createSarifReportMojo: CreateSarifReportMojo
) {

    val targetClasses: List<String>
        get() = createSarifReportMojo.targetClasses

    val projectRoot: File
        get() = createSarifReportMojo.projectRoot

    val generatedTestsRelativeRoot: File
        get() = createSarifReportMojo.generatedTestsRelativeRoot

    val sarifReportsRelativeRoot: File
        get() = createSarifReportMojo.sarifReportsRelativeRoot

    val markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean
        get() = createSarifReportMojo.markGeneratedTestsDirectoryAsTestSourcesRoot

    val testFramework: TestFramework
        get() = testFrameworkParse(createSarifReportMojo.testFramework)

    val mockFramework: MockFramework
        get() = mockFrameworkParse(createSarifReportMojo.mockFramework)

    val generationTimeout: Long
        get() = generationTimeoutParse(createSarifReportMojo.generationTimeout)

    val codegenLanguage: CodegenLanguage
        get() = codegenLanguageParse(createSarifReportMojo.codegenLanguage)

    val mockStrategy: MockStrategyApi
        get() = mockStrategyParse(createSarifReportMojo.mockStrategy)

    val staticsMocking: StaticsMocking
        get() = staticsMockingParse(createSarifReportMojo.staticsMocking)

    val forceStaticMocking: ForceStaticMocking
        get() = forceStaticMockingParse(createSarifReportMojo.forceStaticMocking)

    val classesToMockAlways: Set<ClassId>
        get() = classesToMockAlwaysParse(createSarifReportMojo.classesToMockAlways)
}