package org.utbot.intellij.plugin.generator

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class IntentionHelper(val project: Project, private val editor: Editor, private val testFile: PsiFile) {
    fun applyIntentions() {
        while (true) {
            val actions =
                DumbService.getInstance(project).runReadActionInSmartMode(Computable<Map<IntentionAction, String>> {
                    val daemonProgressIndicator = DaemonProgressIndicator()
                    Disposer.register(project, daemonProgressIndicator)//check it
                    val list = ProgressManager.getInstance().runProcess(Computable<List<HighlightInfo>> {
                        try {
                            DaemonCodeAnalyzerEx.getInstanceEx(project).runMainPasses(
                                testFile,
                                editor.document,
                                daemonProgressIndicator
                            )
                        } catch (e: Exception) {
                            emptyList()// 'Cannot obtain read-action' rare case
                        }
                    }, daemonProgressIndicator)
                    val actions = mutableMapOf<IntentionAction, String>()
                    list.forEach { info ->
                        val quickFixActionRanges = info.quickFixActionRanges
                        if (!quickFixActionRanges.isNullOrEmpty()) {
                            val toList =
                                quickFixActionRanges.map { pair: com.intellij.openapi.util.Pair<HighlightInfo.IntentionActionDescriptor, TextRange> -> pair.first.action }
                                    .toList()
                            toList.forEach { intentionAction -> actions[intentionAction] = intentionAction.familyName }
                        }
                    }
                    actions
                })
            if (actions.isEmpty()) break

            var someWereApplied = false
            actions.forEach {
                if (it.value.isApplicable()) {
                    someWereApplied = true
                    if (it.key.startInWriteAction()) {
                        WriteCommandAction.runWriteCommandAction(project) {
                            editor.document.isInBulkUpdate = true
                            it.key.invoke(project, editor, testFile)
                            editor.document.isInBulkUpdate = false
                        }
                    } else {
                        editor.document.isInBulkUpdate = true
                        it.key.invoke(project, editor, testFile)
                        editor.document.isInBulkUpdate = false
                    }
                }
            }
            if (!someWereApplied) break
        }
    }

    private fun String.isApplicable(): Boolean {
        if (this.startsWith("Change type of actual to ")) return true
        if (this == "Replace 'switch' with 'if'") return true // SetsTest
        if (this == "Replace with allMatch()") return true
        if (this == "Remove redundant cast(s)") return true // SetsTest
        if (this == "Collapse 'catch' blocks") return true // MapsTest
        if (this == "Replace lambda with method reference") return true // MockRandomExamplesTest
        if (this == "Inline variable") return true // ServiceWithFieldTest
        if (this == "Optimize imports") return true
        if (this.startsWith("Replace 'if else' with '&&'")) return true
        if (this.startsWith("Merge with 'case")) return true // CodegenExampleTest
        // if (this.equals("Simplify assertion")) return true // RecursiveTypeTest
        // if (this.familyName.startsWith("Try to generify ")) return true
        return false
        // "Generify File" shows TypeCookDialog to update JavaRefactoringSettings.getInstance() and then call invokeRefactoring
        // We may do the same without dialog interaction
        // "Collapse into loop" for duplicate lines like collection.add(...) comes from background later
        // We may implement it in codegen by ourselves
    }
}