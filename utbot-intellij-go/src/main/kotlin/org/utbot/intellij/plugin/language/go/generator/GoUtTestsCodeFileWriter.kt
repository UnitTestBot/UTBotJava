package org.utbot.intellij.plugin.language.go.generator

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.IncorrectOperationException
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.utbot.go.api.GoUtFile
import org.utbot.intellij.plugin.language.go.models.GenerateGoTestsModel
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import java.nio.file.Paths

// This class is highly inspired by CodeGenerationController.
object GoUtTestsCodeFileWriter {

    private enum class Target { THREAD_POOL, READ_ACTION, WRITE_ACTION, EDT_LATER }

    fun createTestsFileWithGeneratedCode(
        model: GenerateGoTestsModel,
        sourceFile: GoUtFile,
        generatedTestsFileCode: String
    ) {
        val testsFileName = createTestsFileName(sourceFile)
        try {
            val sourcePsiFile = findPsiFile(model, sourceFile)
            val testsPsiFile = createTestsFileNearToSource(sourcePsiFile, testsFileName) ?: return
            WriteCommandAction.runWriteCommandAction(model.project, "Generate Go Tests with UtBot", null, {
                try {
                    writeGeneratedTestsFileCode(testsPsiFile, generatedTestsFileCode)
                } catch (e: IncorrectOperationException) {
                    showCreatingFileError(model.project, testsFileName)
                }
            })
        } catch (e: IncorrectOperationException) {
            showCreatingFileError(model.project, testsFileName)
        }
    }

    private fun findPsiFile(model: GenerateGoTestsModel, sourceFile: GoUtFile): PsiFile {
        val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(sourceFile.absolutePath))!!
        return PsiManager.getInstance(model.project).findFile(virtualFile)!!
    }

    private fun createTestsFileNearToSource(sourcePsiFile: PsiFile, testsFileName: String): PsiFile? {
        val testsFileNameWithExtension = "$testsFileName.go"
        val sourceFileDir = sourcePsiFile.containingDirectory

        runWriteAction { sourceFileDir.findFile(testsFileNameWithExtension)?.delete() }
        runWriteAction { sourceFileDir.createFile(testsFileNameWithExtension) }

        return sourceFileDir.findFile(testsFileNameWithExtension)
    }

    private fun createTestsFileName(sourceFile: GoUtFile) = sourceFile.fileNameWithoutExtension + "_go_ut_test"

    private fun writeGeneratedTestsFileCode(testFile: PsiFile, generatedTestsFileCode: String) {
        val editor = CodeInsightUtil.positionCursor(testFile.project, testFile, testFile)
        //TODO: Use PsiDocumentManager.getInstance(model.project).getDocument(file)
        // if we don't want to open _all_ new files with tests in editor one-by-one
        run(Target.THREAD_POOL) {
            run(Target.EDT_LATER) {
                run(Target.WRITE_ACTION) {
                    unblockDocument(testFile.project, editor.document)
                    // TODO: JIRA:1246 - display warnings if we rewrite the file
                    executeCommand(testFile.project, "Insert Generated Tests") {
                        editor.document.setText(generatedTestsFileCode)
                    }
                    unblockDocument(testFile.project, editor.document)
                }
            }
        }
    }

    private fun unblockDocument(project: Project, document: Document) {
        PsiDocumentManager.getInstance(project).apply {
            commitDocument(document)
            doPostponedOperationsAndUnblockDocument(document)
        }
    }

    private fun run(target: Target, runnable: Runnable) {
        when (target) {
            Target.THREAD_POOL -> AppExecutorUtil.getAppExecutorService().submit {
                runnable.run()
            }

            Target.READ_ACTION -> runReadAction { runnable.run() }
            Target.WRITE_ACTION -> runWriteAction { runnable.run() }
            Target.EDT_LATER -> invokeLater { runnable.run() }
        }
    }

    private fun showCreatingFileError(project: Project, testFileName: String) {
        showErrorDialogLater(
            project,
            message = "Cannot Create File '$testFileName'",
            title = "Failed to Create File"
        )
    }
}