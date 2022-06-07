package org.utbot.gradle.plugin.extension

import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import java.io.File

interface SarifExtensionProvider {
    val targetClasses: List<String>
    val projectRoot: File
    val generatedTestsRelativeRoot: String
    val sarifReportsRelativeRoot: String
    val markGeneratedTestsDirectoryAsTestSourcesRoot: Boolean
    val testFramework: TestFramework
    val mockFramework: MockFramework
    val generationTimeout: Long
    val codegenLanguage: CodegenLanguage
    val mockStrategy: MockStrategyApi
    val staticsMocking: StaticsMocking
    val forceStaticMocking: ForceStaticMocking
    val classesToMockAlways: Set<ClassId>
}