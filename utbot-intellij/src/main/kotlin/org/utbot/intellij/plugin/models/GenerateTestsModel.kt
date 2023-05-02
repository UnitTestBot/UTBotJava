package org.utbot.intellij.plugin.models

import com.intellij.openapi.components.service
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.jetbrains.kotlin.psi.KtFile
import org.utbot.framework.SummariesGenerationType
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.TypeReplacementApproach
import org.utbot.framework.plugin.api.JavaDocCommentStyle
import org.utbot.framework.util.ConflictTriggers
import org.utbot.intellij.plugin.settings.Settings

class GenerateTestsModel(
    project: Project,
    srcModule: Module,
    potentialTestModules: List<Module>,
    srcClasses: Set<PsiClass>,
    val extractMembersFromSrcClasses: Boolean,
    var selectedMembers: Set<MemberInfo>,
    var timeout: Long,
    var generateWarningsForStaticMocking: Boolean = false,
    var fuzzingValue: Double = 0.05
): BaseTestsModel(
    project,
    srcModule,
    potentialTestModules,
    srcClasses
) {
    override var sourceRootHistory = project.service<Settings>().sourceRootHistory
    override var codegenLanguage = project.service<Settings>().codegenLanguage

    lateinit var testFramework: TestFramework
    lateinit var mockStrategy: MockStrategyApi
    lateinit var mockFramework: MockFramework
    lateinit var staticsMocking: StaticsMocking
    lateinit var parametrizedTestSource: ParametrizedTestSource
    lateinit var runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour
    lateinit var hangingTestsTimeout: HangingTestsTimeout
    var runInspectionAfterTestGeneration: Boolean = true
    lateinit var forceStaticMocking: ForceStaticMocking
    lateinit var chosenClassesToMockAlways: Set<ClassId>
    lateinit var commentStyle: JavaDocCommentStyle

    lateinit var typeReplacementApproach: TypeReplacementApproach
    lateinit var profileNames: String

    val conflictTriggers: ConflictTriggers = ConflictTriggers()

    var runGeneratedTestsWithCoverage : Boolean = false
    var summariesGenerationType : SummariesGenerationType = UtSettings.summaryGenerationType
}

val PsiClass.packageName: String
    get() {
        return when (val currentFile = containingFile) {
            is PsiJavaFile -> currentFile.packageName
            is KtFile -> currentFile.packageFqName.asString()
            else -> error("Can't find package name for $this: it should be located either in Java or Kt file")
        }
    }