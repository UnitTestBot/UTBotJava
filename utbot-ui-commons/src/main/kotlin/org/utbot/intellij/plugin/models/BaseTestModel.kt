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
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.idea.util.sourceRoot
import org.utbot.common.PathUtil.fileExtension
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.intellij.plugin.ui.utils.ITestSourceRoot
import org.utbot.intellij.plugin.ui.utils.getResourcesPaths
import org.utbot.intellij.plugin.ui.utils.getSortedTestRoots
import org.utbot.intellij.plugin.ui.utils.isBuildWithGradle
import org.utbot.intellij.plugin.ui.utils.suitableTestSourceRoots
import java.io.IOException
import java.io.StringReader
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import kotlin.streams.asSequence


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
    open lateinit var projectType: ProjectType

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
     * Finds @SpringBootApplication classes in Spring application.
     *
     * @see [getSortedAnnotatedClasses]
     */
    fun getSortedSpringBootApplicationClasses(): Set<String> =
        getSortedAnnotatedClasses("org.springframework.boot.autoconfigure.SpringBootApplication")

    /**
     * Finds @TestConfiguration and @Configuration classes in Spring application.
     *
     * @see [getSortedAnnotatedClasses]
     */
    fun getSortedSpringConfigurationClasses(): Set<String> =
        getSortedAnnotatedClasses("org.springframework.boot.test.context.TestConfiguration") +
                getSortedAnnotatedClasses("org.springframework.context.annotation.Configuration")

    /**
     * Finds classes annotated with given annotation in [srcModule] and [potentialTestModules].
     *
     * Sorting order:
     *   - classes from test source roots (in the order provided by [getSortedTestRoots])
     *   - classes from production source roots
     */
    private fun getSortedAnnotatedClasses(annotationFqn: String): Set<String> {
        val searchScope = potentialTestModules.fold(GlobalSearchScope.moduleScope(srcModule)) { accScope, module ->
            accScope.union(GlobalSearchScope.moduleScope(module))
        }

        val annotationClass = JavaPsiFacade
            .getInstance(project)
            .findClass(annotationFqn, GlobalSearchScope.allScope(project)) ?: return emptySet()

        val testRootToIndex = getSortedTestRoots().withIndex().associate { (i, root) -> root.dir to i }

        return AnnotatedElementsSearch
            .searchPsiClasses(annotationClass, searchScope)
            .findAll()
            .sortedBy { testRootToIndex[it.containingFile.sourceRoot] ?: Int.MAX_VALUE }
            .mapNotNullTo(mutableSetOf()) { it.qualifiedName }
    }

    fun getSpringXMLConfigurationFiles(): Set<String> {
        val resourcesPaths =
            buildList {
                addAll(potentialTestModules)
                add(srcModule)
            }.distinct().flatMapTo(mutableSetOf()) { it.getResourcesPaths() }
        val xmlFilePaths = resourcesPaths.flatMapTo(mutableListOf()) { path ->
            Files.walk(path)
                .asSequence()
                .filter { it.fileExtension == ".xml" }
        }

        val builder = customizeXmlBuilder()
        return xmlFilePaths.mapNotNullTo(mutableSetOf()) { path ->
            try {
                val doc = builder.parse(path.toFile())

                val hasBeanTagName = doc.documentElement.tagName == "beans"
                val hasAttribute = doc.documentElement.getAttribute("xmlns") == "http://www.springframework.org/schema/beans"
                when {
                    hasBeanTagName && hasAttribute -> path.toString()
                    else -> null
                }
            } catch (e: Exception) {
                // `DocumentBuilder.parse` is an unpredictable operation, may have some side effects, we suppress them.
                null
            }
        }
    }

    /**
     * Creates "safe" xml builder instance.
     *
     * Using standard `DocumentBuilderFactory.newInstance()` may lead to some problems like
     * https://stackoverflow.com/questions/343383/unable-to-parse-xml-file-using-documentbuilder.
     *
     * We try to solve it in accordance with top-rated recommendation here
     * https://stackoverflow.com/questions/155101/make-documentbuilder-parse-ignore-dtd-references.
     */
    private fun customizeXmlBuilder(): DocumentBuilder {
        val builderFactory = DocumentBuilderFactory.newInstance()
        builderFactory.isNamespaceAware = true

        // See documentation https://xerces.apache.org/xerces2-j/features.html
        builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

        return builderFactory.newDocumentBuilder()
    }

    fun updateSourceRootHistory(path: String) {
        sourceRootHistory.apply {
            remove(path)//Remove existing entry if any
            add(path)//Add the most recent entry to the end to be brought first at sorting, see org.utbot.intellij.plugin.ui.utils.RootUtilsKt.getSortedTestRoots
            while (size > HISTORY_LIMIT) removeFirst()
        }
    }
}
