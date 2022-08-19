package org.utbot.intellij.plugin.models

import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.jetbrains.kotlin.idea.core.getPackage
import org.utbot.framework.util.ConflictTriggers
import org.utbot.intellij.plugin.ui.utils.jdkVersion

class GenerateTestsModel(
    project: Project,
    srcModule: Module,
    potentialTestModules: List<Module>,
    var srcClasses: Set<PsiClass>,
    var selectedMethods: Set<MemberInfo>?,
    var timeout:Long,
    var generateWarningsForStaticMocking: Boolean = false,
    var fuzzingValue: Double = 0.05
): BaseTestsModel(
    project,
    srcModule,
    potentialTestModules
) {
    override var testPackageName: String? = null
    override var testSourceRoot: VirtualFile? = null
    lateinit var testFramework: TestFramework
    lateinit var mockStrategy: MockStrategyApi
    var mockFramework: MockFramework? = null
    lateinit var staticsMocking: StaticsMocking
    lateinit var parametrizedTestSource: ParametrizedTestSource
    override lateinit var codegenLanguage: CodegenLanguage
    lateinit var runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour
    lateinit var hangingTestsTimeout: HangingTestsTimeout
    lateinit var forceStaticMocking: ForceStaticMocking
    lateinit var chosenClassesToMockAlways: Set<ClassId>

    val conflictTriggers: ConflictTriggers = ConflictTriggers()

    val isMultiPackage: Boolean by lazy {
        srcClasses.map { it.packageName }.distinct().size != 1
    }
    var runGeneratedTestsWithCoverage : Boolean = false

    val jdkVersion: JavaSdkVersion?
        get() = try {
            testModule.jdkVersion()
        } catch (e: IllegalStateException) {
            // Just ignore it here, notification will be shown in org.utbot.intellij.plugin.ui.utils.ModuleUtilsKt.jdkVersionBy
            null
        }
}

val PsiClass.packageName: String get() = this.containingFile.containingDirectory.getPackage()?.qualifiedName ?: ""