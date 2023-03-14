package org.utbot.intellij.plugin.models

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.TestModuleProperties
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.idea.util.sourceRoot
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.intellij.plugin.ui.utils.ITestSourceRoot
import org.utbot.intellij.plugin.ui.utils.getSortedTestRoots
import org.utbot.intellij.plugin.ui.utils.isBuildWithGradle
import org.utbot.intellij.plugin.ui.utils.suitableTestSourceRoots

val PsiClass.packageName: String get() = this.containingFile.containingDirectory.getPackage()?.qualifiedName ?: ""
const val HISTORY_LIMIT = 10

open class BaseTestsModel(
    val project: Project,
    val srcModule: Module,
    val potentialTestModules: List<Module>,
    var srcClasses: Set<PsiClass> = emptySet(),
) {
    // GenerateTestsModel is supposed to be created with non-empty list of potentialTestModules.
    // Otherwise, the error window is supposed to be shown earlier.
    var testModule: Module = potentialTestModules.firstOrNull() ?: error("Empty list of test modules in model")

    var testSourceRoot: VirtualFile? = null
    var testPackageName: String? = null
    open var sourceRootHistory : MutableList<String> = mutableListOf()
    open lateinit var codegenLanguage: CodegenLanguage

    fun setSourceRootAndFindTestModule(newTestSourceRoot: VirtualFile?) {
        requireNotNull(newTestSourceRoot)
        testSourceRoot = newTestSourceRoot
        var target = newTestSourceRoot
        while (target != null && target is FakeVirtualFile) {
            target = target.parent
        }
        if (target == null) {
            error("Could not find module for $newTestSourceRoot")
        }

        testModule = ModuleUtil.findModuleForFile(target, project)
            ?: error("Could not find module for $newTestSourceRoot")
    }

    val isMultiPackage: Boolean by lazy {
        srcClasses.map { it.packageName }.distinct().size != 1
    }

    fun getAllTestSourceRoots() : MutableList<out ITestSourceRoot> {
        with(if (project.isBuildWithGradle) project.allModules() else potentialTestModules) {
            return this.flatMap { it.suitableTestSourceRoots().toList() }.toMutableList().distinct().toMutableList()
        }
    }

    fun getSortedTestRoots(): MutableList<ITestSourceRoot> = getSortedTestRoots(
        getAllTestSourceRoots(),
        sourceRootHistory,
        srcModule.rootManager.sourceRoots.map { file: VirtualFile -> file.toNioPath().toString() },
        codegenLanguage
    )

    /**
     * Searches configuration classes in Spring application.
     *
     * Classes are selected and sorted in the following order:
     *   - Classes marked with `@TestConfiguration` annotation
     *   - Classes marked with `@Configuration` annotation
     *      - firstly, from test source roots (in the order provided by [getSortedTestRoots])
     *      - after that, from source roots
     */
    fun getSortedSpringConfigurationClasses(): List<PsiClass> {
        val testRootToIndex = getSortedTestRoots().withIndex().associate { (i, root) -> root.dir to i }

        // Not using `srcModule.testModules(project)` here because it returns
        // test modules for dependent modules if no test roots are found in the source module itself.
        // We don't want to search configurations there because they seem useless.
        val testModules = ModuleManager.getInstance(project)
            .modules
            .filter { module -> TestModuleProperties.getInstance(module).productionModule == srcModule }

        val searchScope = testModules.fold(GlobalSearchScope.moduleScope(srcModule)) { accScope, module ->
            accScope.union(GlobalSearchScope.moduleScope(module))
        }

        val annotationClasses =  listOf(
            "org.springframework.boot.test.context.TestConfiguration",
            "org.springframework.context.annotation.Configuration"
        ).mapNotNull {
            JavaPsiFacade.getInstance(project).findClass(it, project.allScope())
        }

        return annotationClasses.flatMap { annotation ->
            AnnotatedElementsSearch
                .searchPsiClasses(annotation, searchScope)
                .findAll()
                .sortedBy { testRootToIndex[it.containingFile.sourceRoot] ?: Int.MAX_VALUE }
        }
    }

    fun updateSourceRootHistory(path: String) {
        sourceRootHistory.apply {
            remove(path)//Remove existing entry if any
            add(path)//Add the most recent entry to the end to be brought first at sorting, see org.utbot.intellij.plugin.ui.utils.RootUtilsKt.getSortedTestRoots
            while (size > HISTORY_LIMIT) removeFirst()
        }
    }
}
