package org.utbot.intellij.plugin.language.js

import api.JsTestGenerator
import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.lang.ecmascript6.psi.ES6Class
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
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.konan.file.File
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.testModules
import service.CoverageMode
import settings.JsDynamicSettings
import settings.JsExportsSettings.endComment
import settings.JsExportsSettings.startComment
import settings.JsTestGenerationSettings.dummyClassName

object JsDialogProcessor {

    fun createDialogAndGenerateTests(
        project: Project,
        srcModule: Module,
        fileMethods: Set<JSMemberInfo>,
        focusedMethod: JSMemberInfo?,
        containingFilePath: String,
        editor: Editor,
    ) {
        createDialog(project, srcModule, fileMethods, focusedMethod, containingFilePath)?.let { dialogProcessor ->
            if (!dialogProcessor.showAndGet()) return
            /*
                Since Tern.js accesses containing file, sync with file system required before test generation.
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
    ): JsDialogWindow? {
        val testModules = srcModule.testModules(project)

        if (testModules.isEmpty()) {
            val errorMessage = """
                <html>No test source roots found in the project.<br>
                Please, <a href="https://www.jetbrains.com/help/idea/testing.html#add-test-root">create or configure</a> at least one test source root.
            """.trimIndent()
            showErrorDialogLater(project, errorMessage, "Test source roots not found")
            return null
        }

        return JsDialogWindow(
            JsTestsModel(
                project = project,
                srcModule = srcModule,
                potentialTestModules = testModules,
                fileMethods = fileMethods,
                selectedMethods = if (focusedMethod != null) setOf(focusedMethod) else emptySet(),
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

    private fun createTests(model: JsTestsModel, containingFilePath: String, editor: Editor) {
        val normalizedContainingFilePath = containingFilePath.replace("/", File.separator)
        val settings = JsDynamicSettings(
            pathToNode = model.pathToNode,
            pathToNYC = model.pathToNYC,
            pathToNPM = model.pathToNPM,
            timeout = model.timeout,
            coverageMode = CoverageMode.BASIC
        )
        (object : Task.Backgroundable(model.project, "Generate tests") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.text = "Generate tests: read classes"
                val testDir = PsiDirectoryFactory.getInstance(project).createDirectory(
                    model.testSourceRoot!!
                )
                val testFileName = normalizedContainingFilePath.substringAfterLast(File.separator)
                    .replace(Regex(".js"), "Test.js")
                val testGenerator = JsTestGenerator(
                    fileText = editor.document.text,
                    sourceFilePath = normalizedContainingFilePath,
                    projectPath = model.project.basePath?.replace("/", File.separator)
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
                    outputFilePath = "${testDir.virtualFile.path}/$testFileName".replace("/", File.separator),
                    exportsManager = partialApplication(JsDialogProcessor::manageExports, editor, project),
                    settings = settings
                )

                indicator.fraction = indicator.fraction.coerceAtLeast(0.9)
                indicator.text = "Generate code for tests"

                val generatedCode = testGenerator.run()
                invokeLater {
                    runWriteAction {
                        val testPsiFile =
                            testDir.findFile(testFileName) ?: PsiFileFactory.getInstance(project)
                                .createFileFromText(testFileName, JsLanguageAssistant.jsLanguage, generatedCode)
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

    private fun <A, B, C> partialApplication(f: (A, B, C) -> Unit, a: A, b: B): (C) -> Unit {
        return { c: C -> f(a, b, c) }
    }

    private fun manageExports(editor: Editor, project: Project, exports: List<String>) {
        AppExecutorUtil.getAppExecutorService().submit {
            invokeLater {
                val exportSection = exports.joinToString("\n") { "exports.$it = $it" }
                val fileText = editor.document.text
                when {
                    fileText.contains(exportSection) -> {}

                    fileText.contains(startComment) && !fileText.contains(exportSection) -> {
                        val regex = Regex("$startComment((\\r\\n|\\n|\\r|.)*)$endComment")
                        regex.find(fileText)?.groups?.get(1)?.value?.let { existingSection ->
                            val exportRegex = Regex("exports[.](.*) =")
                            val existingExports = existingSection.split("\n").filter { it.contains(exportRegex) }
                            val existingExportsSet = existingExports.map { rawLine ->
                                exportRegex.find(rawLine)?.groups?.get(1)?.value ?: throw IllegalStateException()
                            }.toSet()
                            val resultSet = existingExportsSet + exports.toSet()
                            val resSection = resultSet.joinToString("\n") { "exports.$it = $it" }
                            val swappedText = fileText.replace(existingSection, "\n$resSection\n")
                            runWriteAction {
                                with(editor.document) {
                                    unblockDocument(project, this)
                                    setText(swappedText)
                                    unblockDocument(project, this)
                                }
                                with(FileDocumentManager.getInstance()) {
                                    saveDocument(editor.document)
                                }
                            }
                        }
                    }

                    else -> {
                        val line = buildString {
                            append("\n$startComment\n")
                            append(exportSection)
                            append("\n$endComment")
                        }
                        runWriteAction {
                            with(editor.document) {
                                unblockDocument(project, this)
                                setText(fileText + line)
                                unblockDocument(project, this)
                            }
                            with(FileDocumentManager.getInstance()) {
                                saveDocument(editor.document)
                            }
                        }
                    }
                }
            }
        }
    }
}