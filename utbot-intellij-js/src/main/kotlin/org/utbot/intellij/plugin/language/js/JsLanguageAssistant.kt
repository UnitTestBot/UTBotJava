package org.utbot.intellij.plugin.language.js

import com.intellij.lang.Language
import com.intellij.lang.ecmascript6.psi.ES6Class
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.utbot.intellij.plugin.language.agnostic.LanguageAssistant
import settings.JsTestGenerationSettings.dummyClassName

object JsLanguageAssistant : LanguageAssistant() {

    private const val jsId = "ECMAScript 6"
    val jsLanguage: Language = Language.findLanguageByID(jsId) ?: error("JavaScript language wasn't found")

    private data class PsiTargets(
        val methods: Set<JSMemberInfo>,
        val focusedMethod: JSMemberInfo?,
        val module: Module,
        val containingFilePath: String,
        val editor: Editor?,
        val file: JSFile
    )

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (methods, focusedMethod, module, containingFilePath, editor, file) = getPsiTargets(e) ?: return
        JsDialogProcessor.createDialogAndGenerateTests(
            project = project,
            srcModule = module,
            fileMethods = methods,
            focusedMethod = focusedMethod,
            containingFilePath = containingFilePath,
            editor = editor,
            file = file,
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): PsiTargets? {
        e.project ?: return null
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE) as? JSFile ?: return null
        val element = if (editor != null) {
            findPsiElement(file, editor) ?: return null
        } else {
            e.getData(CommonDataKeys.PSI_ELEMENT) ?: return null
        }
        val module = element.module ?: return null
        val virtualFile = (e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null).path
        val focusedMethod = getContainingMethod(element)
        containingClass(element)?.let {
            val methods = it.functions
            val memberInfos = generateMemberInfo(e.project!!, methods.toList(), it)
            val focusedMethodMI = memberInfos.find { member ->
                member.member?.name == focusedMethod?.name
            }
            return PsiTargets(
                methods = memberInfos,
                focusedMethod = focusedMethodMI,
                module = module,
                containingFilePath = virtualFile,
                editor = editor,
                file = file,
            )
        }
        var memberInfos = generateMemberInfo(e.project!!, file.statements.filterIsInstance<JSFunction>())
        var focusedMethodMI = memberInfos.find { member ->
            member.member?.name == focusedMethod?.name
        }
        // TODO: generate tests for all classes, not only the first one
        //  (currently not possible since breaks JsTestGenerator routine)
        if (memberInfos.isEmpty()) {
            val classes = file.statements.filterIsInstance<ES6Class>()
            if (classes.isEmpty()) return null

            memberInfos = generateMemberInfo(
                e.project!!,
                emptyList(),
                classes.first()
            )
            if (memberInfos.isEmpty()) return null

            focusedMethodMI = memberInfos.first()
        }
        return PsiTargets(
            methods = memberInfos,
            focusedMethod = focusedMethodMI,
            module = module,
            containingFilePath = virtualFile,
            editor = editor,
            file = file,
        )
    }

    private fun getContainingMethod(element: PsiElement): JSFunction? {
        if (element is JSFunction)
            return element

        val parent = element.parent ?: return null
        return getContainingMethod(parent)
    }

    private fun findPsiElement(file: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset
        var element = file.findElementAt(offset)
        if (element == null && offset == file.textLength) {
            element = file.findElementAt(offset - 1)
        }
        return element
    }

    private fun containingClass(element: PsiElement) =
        PsiTreeUtil.getParentOfType(element, ES6Class::class.java, false)

    private fun buildClassStringFromMethods(methods: List<JSFunction>): String {
        var strBuilder = "\n"
        val filteredMethods = methods.filterNot { method -> method.name == "constructor" }
        filteredMethods.forEach {
            strBuilder += it.text.replace("function ", "")
        }
        // Creating a class with a random name. It won't affect user's code since it is created in abstract PsiFile.
        return "class $dummyClassName {$strBuilder}"
    }

    /*
        Small hack: generating a string source code of an "impossible" class in order to
        generate a PsiFile with it, then extract ES6Class from it, then extract MemberInfos.
        Created for top-level functions that don't have a parent class.
     */
    private fun generateMemberInfo(
        project: Project,
        methods: List<JSFunction>,
        jsClass: JSClass? = null
    ): Set<JSMemberInfo> {
        jsClass?.let {
            val res = mutableListOf<JSMemberInfo>()
            JSMemberInfo.extractClassMembers(it, res) { member ->
                member is JSFunction
            }
            return res.toSet()
        }
        val strClazz = buildClassStringFromMethods(methods)
        val abstractPsiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(jsLanguage, strClazz)
        val clazz = PsiTreeUtil.getChildOfType(abstractPsiFile, JSClass::class.java)
        val res = mutableListOf<JSMemberInfo>()
        JSMemberInfo.extractClassMembers(clazz!!, res) { true }
        return res.toSet()
    }
}
