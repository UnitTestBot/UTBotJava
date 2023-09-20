package org.utbot.intellij.plugin.go.generator

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.util.IncorrectOperationException
import org.utbot.go.api.GoUtFile
import org.utbot.intellij.plugin.go.language.GoLanguageAssistant
import org.utbot.intellij.plugin.go.models.GenerateGoTestsModel
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import java.nio.file.Paths

// This class is highly inspired by CodeGenerationController.
object GoUtTestsCodeFileWriter {

    fun createTestsFileWithGeneratedCode(
        model: GenerateGoTestsModel,
        sourceFile: GoUtFile,
        generatedTestsFileCode: String
    ) {
        val testsFileName = createTestsFileName(sourceFile)
        try {
            runWriteAction {
                val sourcePsiFile = findPsiFile(model, sourceFile)
                val sourceFileDir = sourcePsiFile.containingDirectory

                val testsFileNameWithExtension = "$testsFileName.go"
                val testPsiFile = PsiFileFactory.getInstance(model.project)
                    .createFileFromText(
                        testsFileNameWithExtension, GoLanguageAssistant.language, generatedTestsFileCode
                    )
                sourceFileDir.findFile(testsFileNameWithExtension)?.delete()
                sourceFileDir.add(testPsiFile)

                val testFile = sourceFileDir.findFile(testsFileNameWithExtension)!!
                OpenFileDescriptor(model.project, testFile.virtualFile).navigate(true)
            }
        } catch (e: IncorrectOperationException) {
            showCreatingFileError(model.project, testsFileName)
        }
    }

    private fun findPsiFile(model: GenerateGoTestsModel, sourceFile: GoUtFile): PsiFile {
        val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(sourceFile.absolutePath))!!
        return PsiManager.getInstance(model.project).findFile(virtualFile)!!
    }

    private fun createTestsFileName(sourceFile: GoUtFile) = sourceFile.fileNameWithoutExtension + "_go_ut_test"

    private fun showCreatingFileError(project: Project, testFileName: String) {
        showErrorDialogLater(
            project,
            message = "Cannot Create File '$testFileName'",
            title = "Failed to Create File"
        )
    }
}