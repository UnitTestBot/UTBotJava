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
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.jetbrains.kotlin.idea.core.getPackage
import org.utbot.framework.util.ConflictTriggers

data class GenerateTestsModel(
    val project: Project,
    val srcModule: Module,
    val testModule: Module,
    val jdkVersion: JavaSdkVersion,
    var srcClasses: Set<PsiClass>,
    var selectedMethods: Set<MemberInfo>?,
    var timeout:Long,
    var generateWarningsForStaticMocking: Boolean = false,
) {
    var testSourceRoot: VirtualFile? = null
    var testPackageName: String? = null
    lateinit var testFramework: TestFramework
    lateinit var mockStrategy: MockStrategyApi
    var mockFramework: MockFramework? = null
    lateinit var staticsMocking: StaticsMocking
    lateinit var parametrizedTestSource: ParametrizedTestSource
    lateinit var codegenLanguage: CodegenLanguage
    lateinit var runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour
    lateinit var hangingTestsTimeout: HangingTestsTimeout
    lateinit var forceStaticMocking: ForceStaticMocking
    lateinit var chosenClassesToMockAlways: Set<ClassId>

    val conflictTriggers: ConflictTriggers = ConflictTriggers()

    val isMultiPackage: Boolean by lazy {
        srcClasses.map { it.packageName }.distinct().size != 1
    }
}

val PsiClass.packageName: String get() = this.containingFile.containingDirectory.getPackage()?.qualifiedName ?: ""