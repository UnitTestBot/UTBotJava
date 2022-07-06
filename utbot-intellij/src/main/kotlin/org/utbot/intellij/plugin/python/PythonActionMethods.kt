package org.utbot.intellij.plugin.python

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.*
import com.jetbrains.python.refactoring.classes.PyMemberInfoStorage
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo
import org.jetbrains.kotlin.idea.util.module

object PythonActionMethods {
    const val pythonID = "Python"

    fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (functions, focusedFunction, module) = getPsiTargets(e) ?: return

         PythonDialogProcessor.createDialogAndGenerateTests(
             project,
             module,
             functions,
             focusedFunction
         )
    }

    fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): Triple<Set<PyMemberInfo<PyElement>>, PyFunction?, Module>? {
        val project = e.project ?: return null
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val file = e.getData(CommonDataKeys.PSI_FILE) as? PyFile ?: return null
        val element = findPsiElement(file, editor) ?: return null

        val containingFunction = getContainingFunction(element)
        val containingClass = getContainingClass(element)

        if (containingClass == null) {
            val functions = file.topLevelFunctions
            if (functions.isEmpty())
                return null

            // val focusedFunction = if (functions.contains(containingFunction)) containingFunction else null
            return Triple(pyFunctionsToPyMemberInfo(project, functions), containingFunction, findSrcModule(functions) { it.module })
        }

        val infos = PyMemberInfoStorage(containingClass).getClassMemberInfos(containingClass).filter { it.member is PyFunction }
        return Triple(infos.toSet(), containingFunction, findSrcModule(infos) { (it.member as? PyFunction)?.module })
    }

    private fun getContainingFunction(element: PsiElement): PyFunction? {
        if (element is PyFunction)
            return element

        val parent = element.parent ?: return null
        return getContainingFunction(parent)
    }

    private fun getContainingClass(element: PsiElement): PyClass? {
        if (element is PyClass)
            return element

        val parent = element.parent ?: return null
        return getContainingClass(parent)
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

    private fun <T> findSrcModule(fileMethods: Collection<T>, get_module: (T) -> Module?): Module {
        val srcModules = fileMethods.mapNotNull(get_module).distinct()
        return when (srcModules.size) {
            0 -> error("Module for source classes not found")
            1 -> srcModules.first()
            else -> error("Can not generate tests for classes from different modules")
        }
    }

    private fun pyFunctionsToPyMemberInfo(project: Project, functions: List<PyFunction>): Set<PyMemberInfo<PyElement>> {
        val generator = PyElementGenerator.getInstance(project)
        val newClass = generator.createFromText(
            LanguageLevel.getDefault(),
            PyClass::class.java,
            "class A:\npass"
        )
        functions.forEach {
            newClass.add(it)
        }
        val storage = PyMemberInfoStorage(newClass)
        val infos = storage.getClassMemberInfos(newClass)
        return infos.toSet()
    }
}