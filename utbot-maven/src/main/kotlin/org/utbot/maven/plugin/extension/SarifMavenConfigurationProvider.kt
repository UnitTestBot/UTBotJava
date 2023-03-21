package org.utbot.maven.plugin.extension

import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.sarif.SarifExtensionProvider
import org.utbot.maven.plugin.GenerateTestsAndSarifReportMojo
import java.io.File

/**
 * Provides fields needed to create a SARIF report in a convenient form.
 */
class SarifMavenConfigurationProvider(
    private val generateTestsAndSarifReportMojo: GenerateTestsAndSarifReportMojo
) : SarifExtensionProvider {

    override val targetClasses: List<String>
        get() = generateTestsAndSarifReportMojo.targetClasses

    override val projectRoot: File
        get() = generateTestsAndSarifReportMojo.projectRoot

    override val generatedTestsRelativeRoot: String
        get() = generateTestsAndSarifReportMojo.generatedTestsRelativeRoot

    override val sarifReportsRelativeRoot: String
        get() = generateTestsAndSarifReportMojo.sarifReportsRelativeRoot

    override val markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean
        get() = generateTestsAndSarifReportMojo.markGeneratedTestsDirectoryAsTestSourcesRoot

    override val testPrivateMethods: Boolean
        get() = generateTestsAndSarifReportMojo.testPrivateMethods

    override val projectType: ProjectType
        get() = projectTypeParse(generateTestsAndSarifReportMojo.projectType)

    override val testFramework: TestFramework
        get() = testFrameworkParse(generateTestsAndSarifReportMojo.testFramework)

    override val mockFramework: MockFramework
        get() = mockFrameworkParse(generateTestsAndSarifReportMojo.mockFramework)

    override val generationTimeout: Long
        get() = generationTimeoutParse(generateTestsAndSarifReportMojo.generationTimeout)

    override val codegenLanguage: CodegenLanguage
        get() = codegenLanguageParse(generateTestsAndSarifReportMojo.codegenLanguage)

    override val mockStrategy: MockStrategyApi
        get() = mockStrategyParse(generateTestsAndSarifReportMojo.mockStrategy)

    override val staticsMocking: StaticsMocking
        get() = staticsMockingParse(generateTestsAndSarifReportMojo.staticsMocking)

    override val forceStaticMocking: ForceStaticMocking
        get() = forceStaticMockingParse(generateTestsAndSarifReportMojo.forceStaticMocking)

    override val classesToMockAlways: Set<ClassId>
        get() = classesToMockAlwaysParse(generateTestsAndSarifReportMojo.classesToMockAlways)
}