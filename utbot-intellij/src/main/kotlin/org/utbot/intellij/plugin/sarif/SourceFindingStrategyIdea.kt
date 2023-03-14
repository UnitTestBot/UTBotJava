package org.utbot.intellij.plugin.sarif

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.common.PathUtil.safeRelativize
import org.utbot.common.PathUtil.toPath
import org.utbot.sarif.SourceFindingStrategy
import java.io.File

/**
 * The search strategy based on the information available to the PsiClass.
 */
class SourceFindingStrategyIdea(testClass: PsiClass) : SourceFindingStrategy() {

    /**
     * Returns the relative path (against `project.basePath`) to the file with generated tests.
     */
    override val testsRelativePath: String
        get() = safeRelativize(project.basePath, testsFilePath)
            ?: testsFilePath.toPath().fileName.toString()

    /**
     * Returns the relative path (against `project.basePath`) to the source file containing the class [classFqn].
     */
    override fun getSourceRelativePath(classFqn: String, extension: String?): String {
        val psiClass = findPsiClass(classFqn)
        val absolutePath = psiClass?.containingFile?.virtualFile?.path
        val relativePath = safeRelativize(project.basePath, absolutePath)
        val defaultRelativePath = classFqnToPath(classFqn) + (extension ?: defaultExtension)
        return relativePath ?: defaultRelativePath
    }

    /**
     * Finds the source file containing the class [classFqn].
     * Returns null if the file does not exist.
     */
    override fun getSourceFile(classFqn: String, extension: String?): File? {
        val psiClass = findPsiClass(classFqn)
        val sourceCodeFile = psiClass?.containingFile?.virtualFile?.path?.let(::File)
        return if (sourceCodeFile?.exists() == true) sourceCodeFile else null
    }

    // internal

    private val project = testClass.project

    private val testsFilePath = testClass.containingFile.virtualFile.path

    /**
     * The file extension to be used in [getSourceRelativePath] if the source file
     * was not found by the class qualified name and the `extension` parameter is null.
     */
    private val defaultExtension = "." + (testClass.containingFile.virtualFile.extension ?: "java")

    /**
     * Returns PsiClass by given [classFqn].
     */
    private fun findPsiClass(classFqn: String): PsiClass? {
        val psiFacade = JavaPsiFacade.getInstance(project)
        val psiClass = psiFacade.findClass(classFqn, GlobalSearchScope.allScope(project))
        if (psiClass != null)
            return psiClass

        // If for some reason `psiClass` was not found by the `findClass` method
        val packageName = classFqn.substringBeforeLast('.')
        val shortClassName = classFqn.substringAfterLast('.')
        val neededPackage = psiFacade.findPackage(packageName)
        return neededPackage?.classes?.firstOrNull { it.name == shortClassName }
    }
}