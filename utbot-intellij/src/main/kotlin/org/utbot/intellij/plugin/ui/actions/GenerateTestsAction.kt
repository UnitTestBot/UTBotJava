package org.utbot.intellij.plugin.ui.actions

import org.utbot.intellij.plugin.generator.UtTestsDialogProcessor
import org.utbot.intellij.plugin.ui.utils.PsiElementHandler
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.utbot.intellij.plugin.util.extractFirstLevelMembers
import java.util.*

class GenerateTestsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (srcClasses, focusedMethod, extractMembersFromSrcClasses) = getPsiTargets(e) ?: return
        UtTestsDialogProcessor.createDialogAndGenerateTests(project, srcClasses, extractMembersFromSrcClasses, focusedMethod)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): Triple<Set<PsiClass>, MemberInfo?, Boolean>? {
        val project = e.project ?: return null
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            //The action is being called from editor
            val file = e.getData(CommonDataKeys.PSI_FILE) ?: return null
            val element = findPsiElement(file, editor) ?: return null

            val psiElementHandler = PsiElementHandler.makePsiElementHandler(file)

            if (psiElementHandler.isCreateTestActionAvailable(element)) {
                val srcClass = psiElementHandler.containingClass(element) ?: return null
                if (srcClass.isInterface) return null
                val srcSourceRoot = srcClass.getSourceRoot() ?: return null
                val srcMembers = srcClass.extractFirstLevelMembers(false)
                val focusedMethod = focusedMethodOrNull(element, srcMembers, psiElementHandler)

                val module = ModuleUtil.findModuleForFile(srcSourceRoot, project) ?: return null
                val matchingRoot = ModuleRootManager.getInstance(module).contentEntries
                    .flatMap { entry -> entry.sourceFolders.toList() }
                    .singleOrNull { folder -> folder.file == srcSourceRoot }
                if (srcMembers.isEmpty() || matchingRoot == null || matchingRoot.rootType.isForTests) {
                    return null
                }

                return Triple(setOf(srcClass), focusedMethod, true)
            }
        } else {
            // The action is being called from 'Project' tool window 
            val srcClasses = mutableSetOf<PsiClass>()
            var selectedMethod: MemberInfo? = null
            var extractMembersFromSrcClasses = false
            val element = e.getData(CommonDataKeys.PSI_ELEMENT) ?: return null
            if (element is PsiFileSystemItem) {
                e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.let {
                    srcClasses += getAllClasses(project, it)
                }
            } else {
                val file = element.containingFile ?: return null
                val psiElementHandler = PsiElementHandler.makePsiElementHandler(file)

                if (psiElementHandler.isCreateTestActionAvailable(element)) {
                    psiElementHandler.containingClass(element)?.let {
                        srcClasses += setOf(it)
                        extractMembersFromSrcClasses = true
                        if (it.extractFirstLevelMembers(false).isEmpty())
                            return null
                    }

                    if (element is PsiMethod) {
                        selectedMethod = MemberInfo(element)
                    }
                }
            }
            srcClasses.removeIf { it.isInterface }
            var commonSourceRoot = null as VirtualFile?
            for (srcClass in srcClasses) {
                if (commonSourceRoot == null) {
                    commonSourceRoot = srcClass.getSourceRoot()?: return null
                } else if (commonSourceRoot != srcClass.getSourceRoot()) return null
            }
            if (commonSourceRoot == null) return null
            val module = ModuleUtil.findModuleForFile(commonSourceRoot, project)?: return null

             if (!Arrays.stream(ModuleRootManager.getInstance(module).contentEntries)
                     .flatMap { entry -> Arrays.stream(entry.sourceFolders) }
                     .filter { folder -> !folder.rootType.isForTests && folder.file == commonSourceRoot}
                     .findAny().isPresent ) return null

            return Triple(srcClasses.toSet(), selectedMethod, extractMembersFromSrcClasses)
        }
        return null
    }

    private fun PsiElement?.getSourceRoot() : VirtualFile? {
        val project = this?.project?: return null
        val virtualFile = this.containingFile?.originalFile?.virtualFile?: return null
        return ProjectFileIndex.getInstance(project).getSourceRootForFile(virtualFile)
    }

    private fun findPsiElement(file: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset
        var element = file.findElementAt(offset)
        if (element == null && offset == file.textLength) {
            element = file.findElementAt(offset - 1)
        }

        return element
    }

    private fun focusedMethodOrNull(element: PsiElement, methods: List<MemberInfo>, psiElementHandler: PsiElementHandler): MemberInfo? {
        // getParentOfType might return element which does not correspond to the standard Psi hierarchy.
        // Thus, make transition to the Psi if it is required.
        val currentMethod = PsiTreeUtil.getParentOfType(element, psiElementHandler.methodClass)
                ?.let { psiElementHandler.toPsi(it, PsiMethod::class.java) }

        return methods.singleOrNull { it.member == currentMethod }
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