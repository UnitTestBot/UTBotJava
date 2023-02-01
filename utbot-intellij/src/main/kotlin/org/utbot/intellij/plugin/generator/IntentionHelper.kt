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
import com.intellij.psi.SmartPsiElementPointer
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.util.application.runReadAction

private val logger = KotlinLogging.logger {}
// The required part of IntelliJ API was changed to com.intellij.codeInsight.daemon.impl.MainPassesRunner that is available since 2022.1 only
class IntentionHelper(val project: Project, private val editor: Editor, private val testFile: SmartPsiElementPointer<PsiFile>) {
    fun applyIntentions() {
        val actions =
            DumbService.getInstance(project).runReadActionInSmartMode(Computable<Map<IntentionAction, String>> {
                val daemonProgressIndicator = DaemonProgressIndicator()
                Disposer.register(project) { daemonProgressIndicator.cancel() }//check it
                val list = ProgressManager.getInstance().runProcess(Computable<List<HighlightInfo>> inner@{
                    try {
                        val containingFile = testFile.containingFile ?: return@inner emptyList()
                        DaemonCodeAnalyzerEx.getInstanceEx(project).runMainPasses(
                            containingFile,
                            editor.document,
                            daemonProgressIndicator
                        )
                    } catch (e: Exception) {
                        logger.info { e }
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
        actions.forEach {
            if (runReadAction {
                    it.value.isApplicable() && it.key.isAvailable(
                        project,
                        editor,
                        testFile.containingFile
                    )
                }) {
                if (it.key.startInWriteAction()) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        invokeIntentionAction(it)
                    }
                } else {
                    runReadAction {
                        invokeIntentionAction(it)
                    }
                }
            }
        }
    }

    private fun invokeIntentionAction(it: Map.Entry<IntentionAction, String>) {
        it.key.invoke(project, editor, testFile.containingFile)
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