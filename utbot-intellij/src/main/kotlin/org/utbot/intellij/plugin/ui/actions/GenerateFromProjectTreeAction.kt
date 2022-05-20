package org.utbot.intellij.plugin.ui.actions

import org.utbot.intellij.plugin.ui.UtTestsDialogProcessor
import org.utbot.intellij.plugin.ui.utils.KotlinPsiElementHandler
import org.utbot.intellij.plugin.ui.utils.PsiElementHandler
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass

class GenerateFromProjectTreeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: error("No project found for action event $e")
        val selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        val srcClasses = mutableSetOf<PsiClass>()
        if (selectedElement != null) srcClasses += getAllClasses(selectedElement)
        if (selectedFiles != null) srcClasses += getAllClasses(project, selectedFiles)

        if (srcClasses.isEmpty()) {
            error("Tests generation can be performed only on a class, package or a set of classes from one package")
        }

        UtTestsDialogProcessor.createDialogAndGenerateTests(project, srcClasses, focusedMethod = null)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: error("No project found for action event $e")
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        val isPsiClass = psiElement is PsiClass || psiElement is KtClass
        val isPsiPackage = psiElement is PsiDirectory && (psiElement.getPackage()?.qualifiedName?.isNotEmpty() ?: false)
        val isPsiClassArray = virtualFiles != null && getAllClasses(project, virtualFiles).isNotEmpty()

        e.presentation.isEnabled = isPsiClass || isPsiPackage || isPsiClassArray
    }

    private fun getAllClasses(psiElement: PsiElement): Set<PsiClass> {
        return when (psiElement) {
            is KtClass -> setOf(KotlinPsiElementHandler().toPsi(psiElement, PsiClass::class.java))
            is PsiClass -> setOf(psiElement)
            is PsiDirectory -> getAllClasses(psiElement)
            else -> emptySet()
        }
    }

    private fun getAllClasses(directory: PsiDirectory): Set<PsiClass> {
        val allClasses = directory.files.flatMap { getClassesFromFile(it) }.toMutableSet()
        for (subDir in directory.subdirectories) allClasses += getAllClasses(subDir)
        return allClasses
    }

    private fun getAllClasses(project: Project, virtualFiles: Array<VirtualFile>): Set<PsiClass> {
        val psiFiles = virtualFiles.mapNotNull { it.toPsiFile(project) }
        val psiDirectories = virtualFiles.mapNotNull { it.toPsiDirectory(project) }
        val dirsArePackages = psiDirectories.all { it.getPackage()?.qualifiedName?.isNotEmpty() == true }

        if (!dirsArePackages) {
            return emptySet()
        }
        val allClasses = psiFiles.flatMap { getClassesFromFile(it) }.toMutableSet()
        for (psiDir in psiDirectories) allClasses += getAllClasses(psiDir)

        return allClasses
    }

    private fun getClassesFromFile(psiFile: PsiFile): List<PsiClass> {
        val psiElementHandler = PsiElementHandler.makePsiElementHandler(psiFile)
        return PsiTreeUtil.getChildrenOfTypeAsList(psiFile, psiElementHandler.classClass)
            .map { psiElementHandler.toPsi(it, PsiClass::class.java) }
    }
}
