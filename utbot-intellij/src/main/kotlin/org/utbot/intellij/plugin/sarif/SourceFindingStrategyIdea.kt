package org.utbot.intellij.plugin.sarif

import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.common.PathUtil.safeRelativize
import org.utbot.common.PathUtil.toPath
import org.utbot.sarif.SourceFindingStrategy
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.search.allScope

/**
 * The search strategy based on the information available to the PsiClass
 */
class SourceFindingStrategyIdea(testClass: PsiClass) : SourceFindingStrategy() {

    /**
     * Returns the relative path (against `project.basePath`) to the file with generated tests
     */
    override val testsRelativePath: String
        get() = safeRelativize(project.basePath, testsFilePath)
            ?: testsFilePath.toPath().fileName.toString()

    /**
     * Returns the relative path (against `project.basePath`) to the source file containing the class [classFqn]
     */
    override fun getSourceRelativePath(classFqn: String, extension: String?): String =
        JavaPsiFacade.getInstance(project)
            .findClass(classFqn, project.allScope())?.let { psiClass ->
                safeRelativize(project.basePath, psiClass.containingFile.virtualFile.path)
            } ?: (classFqnToPath(classFqn) + (extension ?: defaultExtension))

    // internal

    private val project = testClass.project

    private val testsFilePath = testClass.containingFile.virtualFile.path

    /**
     * The file extension to be used in [getSourceRelativePath] if the source file
     * was not found by the class qualified name and the `extension` parameter is null
     */
    private val defaultExtension = "." + (testClass.containingFile.virtualFile.extension ?: "java")
}