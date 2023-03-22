package org.utbot.gradle.plugin.extension

import org.gradle.api.Project
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.sarif.SarifExtensionProvider
import java.io.File

/**
 * Provides all [SarifGradleExtension] fields in a convenient form:
 * Defines default values and a transform function for these fields.
 * Takes the fields from the [taskParameters] if they are available there,
 * otherwise takes them from the [extension].
 */
class SarifGradleExtensionProvider(
    private val project: Project,
    private val extension: SarifGradleExtension,
    var taskParameters: Map<String, String> = mapOf()
) : SarifExtensionProvider {

    override val targetClasses: List<String>
        get() = taskParameters["targetClasses"]?.transformKeywordAll()?.parseToList()
            ?: extension.targetClasses.orNull
            ?: listOf()

    override val projectRoot: File
        get() = (taskParameters["projectRoot"] ?: extension.projectRoot.orNull)
            ?.toPath()?.toFile()
            ?: project.projectDir

    override val generatedTestsRelativeRoot: String
        get() = taskParameters["generatedTestsRelativeRoot"]
            ?: extension.generatedTestsRelativeRoot.orNull
            ?: "build/generated/test"

    override val sarifReportsRelativeRoot: String
        get() = taskParameters["sarifReportsRelativeRoot"]
            ?: extension.sarifReportsRelativeRoot.orNull
            ?: "build/generated/sarif"

    // We don't get this field from `taskParameters` because marking the directory
    // as a test source root is possible while the gradle project is reloading,
    // but `taskParameters` become available only when the user runs the gradle task
    // `generateTestsAndSarifReport` (that is, after a reloading).
    override val markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean
        get() = extension.markGeneratedTestsDirectoryAsTestSourcesRoot.orNull
            ?: true

    override val testPrivateMethods: Boolean
        get() = taskParameters["testPrivateMethods"]?.let { it == "true"}
            ?: extension.testPrivateMethods.orNull
            ?: false

    override val projectType: ProjectType
        get() = (taskParameters["projectType"] ?: extension.projectType.orNull)
            ?.let(::projectTypeParse)
            ?: ProjectType.PureJvm

    override val testFramework: TestFramework
        get() = (taskParameters["testFramework"] ?: extension.testFramework.orNull)
            ?.let(::testFrameworkParse)
            ?: TestFramework.defaultItem

    override val mockFramework: MockFramework
        get() = (taskParameters["mockFramework"] ?: extension.mockFramework.orNull)
            ?.let(::mockFrameworkParse)
            ?: MockFramework.defaultItem

    override val generationTimeout: Long
        get() = (taskParameters["generationTimeout"]?.toLongOrNull() ?: extension.generationTimeout.orNull)
            ?.let(::generationTimeoutParse)
            ?: (60 * 1000L) // 60 seconds

    override val codegenLanguage: CodegenLanguage
        get() = (taskParameters["codegenLanguage"] ?: extension.codegenLanguage.orNull)
            ?.let(::codegenLanguageParse)
            ?: CodegenLanguage.defaultItem

    override val mockStrategy: MockStrategyApi
        get() = (taskParameters["mockStrategy"] ?: extension.mockStrategy.orNull)
            ?.let(::mockStrategyParse)
            ?: MockStrategyApi.defaultItem

    override val staticsMocking: StaticsMocking
        get() = (taskParameters["staticsMocking"] ?: extension.staticsMocking.orNull)
            ?.let(::staticsMockingParse)
            ?: StaticsMocking.defaultItem

    override val forceStaticMocking: ForceStaticMocking
        get() = (taskParameters["forceStaticMocking"] ?: extension.forceStaticMocking.orNull)
            ?.let(::forceStaticMockingParse)
            ?: ForceStaticMocking.defaultItem

    override val classesToMockAlways: Set<ClassId>
        get() = classesToMockAlwaysParse(
            specifiedClasses = taskParameters["classesToMockAlways"]?.parseToList()
                ?: extension.classesToMockAlways.orNull
                ?: listOf()
        )

    /**
     * SARIF report file containing static analysis information about all [targetClasses].
     */
    val mergedSarifReportFileName: String?
        get() = taskParameters["mergedSarifReportFileName"]

    // internal

    /**
     * Keyword "all" is the same as "[]" for [targetClasses], but more user-friendly.
     */
    private fun String.transformKeywordAll(): String =
        if (this == "all") "[]" else this

    /**
     * Example: "[A, B, C]" -> ["A", "B", "C"].
     */
    private fun String.parseToList() =
        this.removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim() }
            .filter { it != "" }
}
