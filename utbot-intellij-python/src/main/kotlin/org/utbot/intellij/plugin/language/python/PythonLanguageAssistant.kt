package org.utbot.intellij.plugin.language.python

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.utbot.framework.plugin.api.util.LockFile
import org.utbot.intellij.plugin.language.agnostic.LanguageAssistant

object PythonLanguageAssistant : LanguageAssistant() {

    private const val pythonID = "Python"
    val language: Language = Language.findLanguageByID(pythonID) ?: error("Language wasn't found")

    data class Targets(
        val pyClasses: Set<PyClass>,
        val pyFunctions: Set<PyFunction>,
        val focusedClass: PyClass?,
        val focusedFunction: PyFunction?,
        val editor: Editor?
    ) {
        override fun toString(): String {
            return "Targets($pyClasses, $pyFunctions)"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val targets = getPsiTargets(e) ?: return

        PythonDialogProcessor.createDialogAndGenerateTests(
            project,
            targets.pyClasses + targets.pyFunctions,
            targets.focusedFunction ?: targets.focusedClass,
            targets.editor,
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !LockFile.isLocked() && getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): Targets? {
        val project = e.project ?: return null
        val editor = e.getData(CommonDataKeys.EDITOR)

        val resultFunctions = mutableSetOf<PyFunction>()
        val resultClasses = mutableSetOf<PyClass>()
        val focusedFunction: PyFunction?
        var focusedClass: PyClass? = null

        if (editor != null) {
            val file = e.getData(CommonDataKeys.PSI_FILE) as? PyFile ?: return null
            val element = findPsiElement(file, editor) ?: return null

            val rootFunctions = file.topLevelFunctions.filter { fineFunction(it) }
            val rootClasses = file.topLevelClasses.filter { fineClass(it) }

            val containingClass = getContainingElement<PyClass>(element) { fineClass(it) }
            val containingFunction: PyFunction? =
                if (containingClass == null)
                    getContainingElement(element) { it.parent is PsiFile && fineFunction(it) }
                else
                    getContainingElement(element) { func ->
                        val ancestors = getAncestors(func)
                        ancestors.dropLast(1).all { it !is PyFunction } &&
                                ancestors.count { it is PyClass } == 1 && fineFunction(func)
                    }

            if (rootClasses.isEmpty()) {
                return if (rootFunctions.isEmpty()) {
                    null
                } else {
                    resultFunctions.addAll(rootFunctions)
                    focusedFunction = containingFunction
                    Targets(resultClasses, resultFunctions, null, focusedFunction, editor)
                }
            } else {
                if (containingClass == null) {
                    resultClasses.addAll(rootClasses)
                    resultFunctions.addAll(rootFunctions)
                    focusedFunction = containingFunction
                } else {
                    resultFunctions.addAll(containingClass.methods.filter { fineFunction(it) })
                    focusedClass = containingClass
                    focusedFunction = containingFunction
                }
                return Targets(resultClasses, resultFunctions, focusedClass, focusedFunction, editor)
            }
        } else {
            val element = e.getData(CommonDataKeys.PSI_ELEMENT)
            if (element is PsiFileSystemItem) {
                e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.let {
                    val (classes, functions) = getAllElements(project, it.toList())
                    resultFunctions.addAll(functions)
                    resultClasses.addAll(classes)
                }
            } else {
                val someSelection = e.getData(PlatformDataKeys.PSI_ELEMENT_ARRAY)?: return null
                someSelection.forEach {
                    when(it) {
                        is PsiFileSystemItem -> {
                            val (classes, functions) = getAllElements(project, listOf(it.virtualFile))
                            resultFunctions += functions
                            resultClasses += classes
                        }
                    }
                }
            }
            if (resultClasses.isNotEmpty() || resultFunctions.isNotEmpty()) {
                return Targets(resultClasses, resultFunctions, null, null, null)
            }
        }
        return null
    }

    // this method is copy-paste from GenerateTestsActions.kt
    private fun findPsiElement(file: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset
        var element = file.findElementAt(offset)
        if (element == null && offset == file.textLength) {
            element = file.findElementAt(offset - 1)
        }

        return element
    }

    private fun getAllElements(project: Project, virtualFiles: Collection<VirtualFile>): Pair<Set<PyClass>, Set<PyFunction>> {
        val psiFiles = virtualFiles.mapNotNull { it.toPsiFile(project) }
        val psiDirectories = virtualFiles.mapNotNull { it.toPsiDirectory(project) }

        val classes = psiFiles.flatMap { getClassesFromFile(it) }.toMutableSet()
        val functions = psiFiles.flatMap { getFunctionsFromFile(it) }.toMutableSet()

        psiDirectories.forEach {
            classes.addAll(getAllClasses(it))
            functions.addAll(getAllFunctions(it))
        }

        return classes to functions
    }

    private fun getAllFunctions(directory: PsiDirectory): Set<PyFunction> {
        val allFunctions = directory.files.flatMap { getFunctionsFromFile(it) }.toMutableSet()
        directory.subdirectories.forEach {
            allFunctions.addAll(getAllFunctions(it))
        }
        return allFunctions
    }

    private fun getAllClasses(directory: PsiDirectory): Set<PyClass> {
        val allClasses = directory.files.flatMap { getClassesFromFile(it) }.toMutableSet()
        directory.subdirectories.forEach {
            allClasses.addAll(getAllClasses(it))
        }
        return allClasses
    }

    private fun getFunctionsFromFile(psiFile: PsiFile): List<PyFunction> {
        return PsiTreeUtil.getChildrenOfTypeAsList(psiFile, PyFunction::class.java)
            .map { it as PyFunction }
            .filter { fineFunction(it) }
    }

    private fun getClassesFromFile(psiFile: PsiFile): List<PyClass> {
        return PsiTreeUtil.getChildrenOfTypeAsList(psiFile, PyClass::class.java)
            .map { it as PyClass }
            .filter { fineClass(it) }
    }
}