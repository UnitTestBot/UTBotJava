package org.utbot.intellij.plugin.models

import com.intellij.openapi.components.service
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.jetbrains.kotlin.psi.KtFile
import org.utbot.framework.plugin.api.JavaDocCommentStyle
import org.utbot.framework.util.ConflictTriggers
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.intellij.plugin.ui.utils.jdkVersion

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

    fun setSourceRootAndFindTestModule(newTestSourceRoot: VirtualFile?) {
        requireNotNull(newTestSourceRoot)
        testSourceRoot = newTestSourceRoot
        var target = newTestSourceRoot
        while(target != null && target is FakeVirtualFile) {
            target = target.parent
        }
        if (target == null) {
            error("Could not find module for $newTestSourceRoot")
        }

        testModule = ModuleUtil.findModuleForFile(target, project)
            ?: error("Could not find module for $newTestSourceRoot")
    }

//    val codegenLanguage = project.service<Settings>().codegenLanguage

    var testPackageName: String? = null
    lateinit var testFramework: TestFramework
    lateinit var mockStrategy: MockStrategyApi
    lateinit var mockFramework: MockFramework
    lateinit var staticsMocking: StaticsMocking
    lateinit var parametrizedTestSource: ParametrizedTestSource
    lateinit var runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour
    lateinit var hangingTestsTimeout: HangingTestsTimeout
    lateinit var forceStaticMocking: ForceStaticMocking
    lateinit var chosenClassesToMockAlways: Set<ClassId>
    lateinit var commentStyle: JavaDocCommentStyle

    val conflictTriggers: ConflictTriggers = ConflictTriggers()

    var runGeneratedTestsWithCoverage : Boolean = false
    var enableSummariesGeneration : Boolean = true

    val jdkVersion: JavaSdkVersion?
        get() = try {
            testModule.jdkVersion()
        } catch (e: IllegalStateException) {
            // Just ignore it here, notification will be shown in org.utbot.intellij.plugin.ui.utils.ModuleUtilsKt.jdkVersionBy
            null
        }
}

val PsiClass.packageName: String
    get() {
        return when (val currentFile = containingFile) {
            is PsiJavaFile -> currentFile.packageName
            is KtFile -> currentFile.packageFqName.asString()
            else -> error("Can't find package name for $this: it should be located either in Java or Kt file")
        }
    }