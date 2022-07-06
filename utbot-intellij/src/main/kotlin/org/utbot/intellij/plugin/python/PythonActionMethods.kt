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

    data class Targets(
        val functions: Set<PyMemberInfo<PyElement>>,
        val focusedFunction: PyFunction?,
        val module: Module,
        val files: Set<PyFile>
    )

    fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (functions, focusedFunction, module, files) = getPsiTargets(e) ?: return

         PythonDialogProcessor.createDialogAndGenerateTests(
             project,
             module,
             functions,
             focusedFunction,
             files
         )
    }

    fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): Targets? {
        val project = e.project ?: return null
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val file = e.getData(CommonDataKeys.PSI_FILE) as? PyFile ?: return null
        val element = findPsiElement(file, editor) ?: return null


        val containingFunction = IterationUtils.getContainingElement<PyFunction>(element)
        val containingClass = IterationUtils.getContainingElement<PyClass>(element)

        if (containingClass == null) {
            val functions = file.topLevelFunctions
            if (functions.isEmpty())
                return null

            val focusedFunction = if (functions.contains(containingFunction)) containingFunction else null
            return Targets(
                pyFunctionsToPyMemberInfo(project, functions),
                focusedFunction,
                findSrcModule(functions) { it.module },
                setOf(file)
            )
        }

        val infos = PyMemberInfoStorage(containingClass).getClassMemberInfos(containingClass).filter { it.member is PyFunction }
        if (infos.isEmpty())
            return null

        val focusedFunction = if (infos.any {it.member.name == containingFunction?.name}) containingFunction else null
        return Targets(
            infos.toSet(),
            focusedFunction,
            findSrcModule(infos) { (it.member as? PyFunction)?.module },
            setOf(file)
        )
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

    private fun <T> findSrcModule(
        fileMethods: Collection<T>,
        getElementModule: (T) -> Module?,
    ): Module {
        val srcModules = fileMethods.mapNotNull(getElementModule).distinct()
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
            "class __ivtdjvrdkgbmpmsclaro__:\npass"
        )
        functions.forEach {
            newClass.add(it)
        }
        val storage = PyMemberInfoStorage(newClass)
        val infos = storage.getClassMemberInfos(newClass)
        return infos.toSet()
    }
}