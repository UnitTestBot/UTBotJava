package org.utbot.intellij.plugin.language.ts

import org.utbot.language.ts.api.TsTestGenerator
import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.lang.ecmascript6.psi.ES6Class
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.konan.file.File
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.testModules
import org.utbot.language.ts.settings.TsDynamicSettings
import org.utbot.language.ts.settings.TsTestGenerationSettings.dummyClassName

object TsDialogProcessor {

    fun createDialogAndGenerateTests(
        project: Project,
        srcModule: Module,
        fileMethods: Set<JSMemberInfo>,
        focusedMethod: JSMemberInfo?,
        containingFilePath: String,
        editor: Editor,
        file: JSFile
    ) {
        createDialog(project, srcModule, fileMethods, focusedMethod, containingFilePath, file)?.let { dialogProcessor ->
            if (!dialogProcessor.showAndGet()) return
            /*
                Since Tern.ts accesses containing file, sync with file system required before test generation.
             */
            runWriteAction {
                with(FileDocumentManager.getInstance()) {
                    saveDocument(editor.document)
                }
            }
            createTests(dialogProcessor.model, containingFilePath, editor)
        }
    }

    private fun createDialog(
        project: Project,
        srcModule: Module,
        fileMethods: Set<JSMemberInfo>,
        focusedMethod: JSMemberInfo?,
        filePath: String,
        file: JSFile
    ): TsDialogWindow? {
        val testModules = srcModule.testModules(project)

        if (testModules.isEmpty()) {
            val errorMessage = """
                <html>No test source roots found in the project.<br>
                Please, <a href="https://www.jetbrains.com/help/idea/testing.html#add-test-root">create or configure</a> at least one test source root.
            """.trimIndent()
            showErrorDialogLater(project, errorMessage, "Test source roots not found")
            return null
        }

        return TsDialogWindow(
            TsTestsModel(
                project = project,
                srcModule = srcModule,
                potentialTestModules = testModules,
                fileMethods = fileMethods,
                selectedMethods = if (focusedMethod != null) setOf(focusedMethod) else emptySet(),
                file = file
            ).apply {
                containingFilePath = filePath
            }
        )
    }

    private fun unblockDocument(project: Project, document: Document) {
        PsiDocumentManager.getInstance(project).apply {
            commitDocument(document)
            doPostponedOperationsAndUnblockDocument(document)
        }
    }

    private fun createTests(model: TsTestsModel, containingFilePath: String, editor: Editor) {
        val normalizedContainingFilePath = containingFilePath.replace(File.separator, "/")
        (object : Task.Backgroundable(model.project, "Generate tests") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.text = "Generate tests: read classes"
                val testDir = PsiDirectoryFactory.getInstance(project).createDirectory(
                    model.testSourceRoot!!
                )
                val testFileName = normalizedContainingFilePath.substringAfterLast("/")
                    .replace(Regex(".ts"), "Test.ts")
                val testGenerator = org.utbot.language.ts.api.TsTestGenerator(
                    sourceFilePath = normalizedContainingFilePath,
                    projectPath = model.project.basePath?.replace(File.separator, "/")
                        ?: throw IllegalStateException("Can't access project path."),
                    selectedMethods = runReadAction {
                        model.selectedMethods.map {
                            it.member.name!!
                        }
                    },
                    parentClassName = runReadAction {
                        val name = (model.selectedMethods.first().member.parent as ES6Class).name
                        if (name == dummyClassName) null else name
                    },
                    outputFilePath = "${testDir.virtualFile.path}/$testFileName".replace(File.separator, "/"),
                    settings = TsDynamicSettings(
                        pathToNYC = model.pathToNYC,
                        timeout = model.timeout,
                        coverageMode = model.coverageMode,
                        tsNycModulePath = model.tsNycModulePath,
                        tsModulePath = model.tsModulePath,
                    )
                )

                indicator.fraction = indicator.fraction.coerceAtLeast(0.9)
                indicator.text = "Generate code for tests"

                val generatedCode = testGenerator.run()
                invokeLater {
                    runWriteAction {
                        val testPsiFile =
                            testDir.findFile(testFileName) ?: PsiFileFactory.getInstance(project)
                                .createFileFromText(testFileName, TsLanguageAssistant.tsLanguage, generatedCode)
                        val testFileEditor =
                            CodeInsightUtil.positionCursor(project, testPsiFile, testPsiFile)
                        unblockDocument(project, testFileEditor.document)
                        testFileEditor.document.setText(generatedCode)
                        unblockDocument(project, testFileEditor.document)
                        testDir.findFile(testFileName) ?: testDir.add(testPsiFile)
                    }
                }
            }
        }).queue()
    }
}
