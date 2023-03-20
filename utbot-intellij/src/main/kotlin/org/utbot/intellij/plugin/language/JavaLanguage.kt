package org.utbot.intellij.plugin.language

import com.intellij.openapi.actionSystem.ActionPlaces
import org.utbot.intellij.plugin.generator.UtTestsDialogProcessor
import org.utbot.intellij.plugin.ui.utils.PsiElementHandler
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.runReadAction
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
import org.jetbrains.kotlin.idea.util.module
import org.utbot.intellij.plugin.util.extractFirstLevelMembers
import org.utbot.intellij.plugin.util.isVisible
import java.util.*
import org.jetbrains.kotlin.j2k.getContainingClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.utils.addIfNotNull
import org.utbot.framework.plugin.api.util.LockFile
import org.utbot.intellij.plugin.models.packageName
import org.utbot.intellij.plugin.ui.InvalidClassNotifier
import org.utbot.intellij.plugin.language.agnostic.LanguageAssistant
import org.utbot.intellij.plugin.util.findSdkVersionOrNull

object JvmLanguageAssistant : LanguageAssistant() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val (srcClasses, focusedMethods, extractMembersFromSrcClasses) = getPsiTargets(e) ?: return
        val validatedSrcClasses = validateSrcClasses(srcClasses) ?: return

        UtTestsDialogProcessor.createDialogAndGenerateTests(project, validatedSrcClasses, extractMembersFromSrcClasses, focusedMethods)
    }

    override fun update(e: AnActionEvent) {
        if (LockFile.isLocked()) {
            e.presentation.isEnabled = false
            return
        }
        if (e.place == ActionPlaces.POPUP) {
            e.presentation.text = "Tests with UnitTestBot..."
        }
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): Triple<Set<PsiClass>, Set<MemberInfo>, Boolean>? {
        val project = e.project ?: return null
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            //The action is being called from editor
            val file = e.getData(CommonDataKeys.PSI_FILE) ?: return null
            val element = findPsiElement(file, editor) ?: return null

            val psiElementHandler = PsiElementHandler.makePsiElementHandler(file)

            if (psiElementHandler.isCreateTestActionAvailable(element)) {
                val srcClass = psiElementHandler.containingClass(element) ?: return null
                val srcSourceRoot = srcClass.getSourceRoot() ?: return null
                val srcMembers = srcClass.extractFirstLevelMembers(false)
                val focusedMethod = focusedMethodOrNull(element, srcMembers, psiElementHandler)

                val module = ModuleUtil.findModuleForFile(srcSourceRoot, project) ?: return null
                val matchingRoot = ModuleRootManager.getInstance(module).contentEntries
                    .flatMap { entry -> entry.sourceFolders.toList() }
                    .firstOrNull { folder -> folder.file == srcSourceRoot }
                if (srcMembers.isEmpty() || matchingRoot == null || matchingRoot.rootType.isForTests) {
                    return null
                }

                return Triple(setOf(srcClass), if (focusedMethod != null) setOf(focusedMethod) else emptySet(), true)
            }
        } else {
            // The action is being called from 'Project' tool window
            val srcClasses = mutableSetOf<PsiClass>()
            val selectedMethods = mutableSetOf<MemberInfo>()
            var extractMembersFromSrcClasses = false
            val element = e.getData(CommonDataKeys.PSI_ELEMENT)
            if (element is PsiFileSystemItem) {
                e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.let {
                    srcClasses += getAllClasses(project, it)
                }
            } else if (element is PsiElement){
                val file = element.containingFile ?: return null
                val psiElementHandler = PsiElementHandler.makePsiElementHandler(file)

                if (psiElementHandler.isCreateTestActionAvailable(element)) {
                    psiElementHandler.containingClass(element)?.let {
                        srcClasses += setOf(it)
                        extractMembersFromSrcClasses = true
                        val memberInfoList = runReadAction<List<MemberInfo>> {
                            it.extractFirstLevelMembers(false)
                        }
                        if (memberInfoList.isNullOrEmpty())
                            return null
                    }

                    if (element is PsiMethod) {
                        selectedMethods.add(MemberInfo(element))
                    }
                }
            } else {
                val someSelection = e.getData(PlatformDataKeys.PSI_ELEMENT_ARRAY)?: return null
                someSelection.forEach {
                    when(it) {
                        is PsiFileSystemItem  -> srcClasses += getAllClasses(project, arrayOf(it.virtualFile))
                        is PsiClass -> srcClasses.add(it)
                        is KtClass -> srcClasses += getClassesFromFile(it.containingKtFile)
                        is PsiElement -> {
                            srcClasses.addIfNotNull(it.getContainingClass())
                            if (it is PsiMethod) {
                                selectedMethods.add(MemberInfo(it))
                                extractMembersFromSrcClasses = true
                            }
                        }
                    }
                }
            }

            if (srcClasses.size > 1) {
                extractMembersFromSrcClasses = false
            }
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

            return Triple(srcClasses.toSet(), selectedMethods.toSet(), extractMembersFromSrcClasses)
        }
        return null
    }

    /**
     * Validates that a set of source classes matches some requirements from [isInvalid].
     * If no one of them matches, shows a warning about the first mismatch reason.
     */
    private fun validateSrcClasses(srcClasses: Set<PsiClass>): Set<PsiClass>? {
        val filteredClasses = srcClasses
            .filterNot { it.isInvalid(withWarnings = false) }
            .toSet()

        if (filteredClasses.isEmpty()) {
            srcClasses.first().isInvalid(withWarnings = true)
            return null
        }

        return filteredClasses
    }

    private fun PsiClass.isInvalid(withWarnings: Boolean): Boolean {
        if (this.module?.let { findSdkVersionOrNull(it) } == null) {
            if (withWarnings) InvalidClassNotifier.notify("class out of module or with undefined SDK")
            return true
        }

        val isInvisible = !this.isVisible
        if (isInvisible) {
            if (withWarnings) InvalidClassNotifier.notify("private or protected class ${this.name}")
            return true
        }

        val packageIsIncorrect = this.packageName.split(".").firstOrNull() == "java"
        if (packageIsIncorrect) {
            if (withWarnings) InvalidClassNotifier.notify("class ${this.name} located in java.* package")
            return true
        }

        return false
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