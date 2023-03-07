package org.utbot.intellij.plugin.language.js

import api.JsTestGenerator
import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreterManager
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
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.util.concurrency.AppExecutorUtil
import mu.KotlinLogging
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.konan.file.File
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import org.utbot.intellij.plugin.ui.utils.testModules
import settings.JsDynamicSettings
import settings.JsExportsSettings.endComment
import settings.JsExportsSettings.startComment
import settings.JsTestGenerationSettings.dummyClassName
import settings.PackageDataService
import settings.jsPackagesList
import utils.JsCmdExec
import utils.OsProvider
import java.io.IOException

private val logger = KotlinLogging.logger {}

object JsDialogProcessor {

    fun createDialogAndGenerateTests(
        project: Project,
        srcModule: Module,
        fileMethods: Set<JSMemberInfo>,
        focusedMethod: JSMemberInfo?,
        containingFilePath: String,
        editor: Editor?,
        file: JSFile
    ) {
        val model =
            createJsTestModel(project, srcModule, fileMethods, focusedMethod, containingFilePath, file) ?: return
        (object : Task.Backgroundable(
            project,
            "Check the requirements"
        ) {
            override fun run(indicator: ProgressIndicator) {
                invokeLater {
                    if (!PackageDataService(
                            model.containingFilePath, model.project.basePath!!, model.pathToNPM
                        ).checkAndInstallRequirements(project)
                    ) return@invokeLater
                    createDialog(model)?.let { dialogWindow ->
                        if (!dialogWindow.showAndGet()) return@invokeLater
                        // Since Tern.js accesses containing file, sync with file system required before test generation.
                        editor?.let {
                            runWriteAction {
                                with(FileDocumentManager.getInstance()) {
                                    saveDocument(editor.document)
                                }
                            }
                        }
                        createTests(
                            dialogWindow.model,
                            containingFilePath,
                            editor,
                            dialogWindow.model.file.getContent()
                        )
                    }
                }
            }
        }).queue()
    }

    private fun findNodeAndNPM(): Pair<String, String>? = try {
        val pathToNode =
            NodeJsLocalInterpreterManager.getInstance().interpreters.first().interpreterSystemIndependentPath
        val (_, errorText) = JsCmdExec.runCommand(
            shouldWait = true, cmd = arrayOf("\"${pathToNode}\"", "-v")
        )
        if (errorText.isNotEmpty()) throw NoSuchElementException()
        val pathToNPM =
            pathToNode.substringBeforeLast("/") + "/" + "npm" + OsProvider.getProviderByOs().npmPackagePostfix
        pathToNode to pathToNPM
    } catch (e: NoSuchElementException) {
        Messages.showErrorDialog(
            "Node.js interpreter is not found in IDEA settings.\n" + "Please set it in Settings > Languages & Frameworks > Node.js",
            "Requirement Error"
        )
        logger.error { "Node.js interpreter was not found in IDEA settings." }
        null
    } catch (e: IOException) {
        Messages.showErrorDialog(
            "Node.js interpreter path is corrupted in IDEA settings.\n" + "Please check Settings > Languages & Frameworks > Node.js",
            "Requirement Error"
        )
        logger.error { "Node.js interpreter path is corrupted in IDEA settings." }
        null
    }

    private fun createJsTestModel(
        project: Project,
        srcModule: Module,
        fileMethods: Set<JSMemberInfo>,
        focusedMethod: JSMemberInfo?,
        filePath: String,
        file: JSFile
    ): JsTestsModel? {
        val testModules = srcModule.testModules(project)

        if (testModules.isEmpty()) {
            val errorMessage = """
                <html>No test source roots found in the project.<br>
                Please, <a href="https://www.jetbrains.com/help/idea/testing.html#add-test-root">create or configure</a> at least one test source root.
            """.trimIndent()
            showErrorDialogLater(project, errorMessage, "Test source roots not found")
            return null
        }
        val (pathToNode, pathToNPM) = findNodeAndNPM() ?: return null
        return JsTestsModel(
            project = project,
            srcModule = srcModule,
            potentialTestModules = testModules,
            fileMethods = fileMethods,
            selectedMethods = if (focusedMethod != null) setOf(focusedMethod) else emptySet(),
            file = file,
        ).apply {
            containingFilePath = filePath
            this.pathToNode = pathToNode
            this.pathToNPM = pathToNPM
        }
    }

    private fun createDialog(jsTestsModel: JsTestsModel?) = jsTestsModel?.let { JsDialogWindow(it) }

    private fun createTests(model: JsTestsModel, containingFilePath: String, editor: Editor?, contents: String) {
        val normalizedContainingFilePath = containingFilePath.replace(File.separator, "/")
        (object : Task.Backgroundable(model.project, "Generate tests") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.text = "Generate tests: read classes"
                val testDir = PsiDirectoryFactory.getInstance(project).createDirectory(
                    model.testSourceRoot!!
                )
                val testFileName = normalizedContainingFilePath.substringAfterLast("/").replace(Regex(".js"), "Test.js")
                currentFileText = model.file.getContent()
                val testGenerator = JsTestGenerator(
                    fileText = contents,
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
                    exportsManager = partialApplication(
                        JsDialogProcessor::manageExports, editor, project, model
                    ),
                    settings = JsDynamicSettings(
                        pathToNode = model.pathToNode,
                        pathToNYC = model.pathToNYC,
                        pathToNPM = model.pathToNPM,
                        timeout = model.timeout,
                        coverageMode = model.coverageMode
                    ),
                    isCancelled = { indicator.isCanceled })

                indicator.fraction = indicator.fraction.coerceAtLeast(0.9)
                indicator.text = "Generate code for tests"

                val generatedCode = testGenerator.run()
                invokeLater {
                    runWriteAction {
                        val testPsiFile = testDir.findFile(testFileName) ?: run {
                            val temp = PsiFileFactory.getInstance(project)
                            .createFileFromText(testFileName, JsLanguageAssistant.jsLanguage, generatedCode)
                            testDir.add(temp)
                            testDir.findFile(testFileName)!!
                        }
                        val testFileEditor = CodeInsightUtil.positionCursor(project, testPsiFile, testPsiFile)
                        unblockDocument(project, testFileEditor.document)
                        testFileEditor.document.setText(generatedCode)
                        unblockDocument(project, testFileEditor.document)
                    }
                }
            }
        }).queue()
    }

    private fun <A, B, C> partialApplication(f: (A, B, C) -> Unit, a: A, b: B): (C) -> Unit {
        return { c: C -> f(a, b, c) }
    }

    private fun JSFile.getContent(): String = this.viewProvider.contents.toString()

    // Needed for continuous exports managing
    private var currentFileText = ""

    private fun manageExports(
        editor: Editor?,
        project: Project,
        model: JsTestsModel,
        swappedText: (String?, String) -> String
    ) {
        AppExecutorUtil.getAppExecutorService().submit {
            invokeLater {
                val exportSection = exports.joinToString("\n") { "exports.$it = $it" }
                when {
                    currentFileText.contains(startComment) -> {
                        val regex = Regex("$startComment((\\r\\n|\\n|\\r|.)*)$endComment")
                        regex.find(currentFileText)?.groups?.get(1)?.value?.let { existingSection ->
                            val newText = swappedText(existingSection, currentFileText)
                            editor?.let {
                                runWriteAction {
                                    with(editor.document) {
                                        unblockDocument(project, this)
                                        setText(newText)
                                        unblockDocument(project, this)
                                    }
                                    with(FileDocumentManager.getInstance()) {
                                        saveDocument(editor.document)
                                    }
                                }
                            } ?: run {
                                File(model.containingFilePath).writeText(newText)
                            }
                            currentFileText = newText
                        }
                    }

                    else -> {
                        val line = buildString {
                            append("\n$startComment")
                            append(swappedText(null, currentFileText))
                            append(endComment)
                        }
                        editor?.let {
                            runWriteAction {
                                with(editor.document) {
                                    unblockDocument(project, this)
                                    setText(currentFileText + line)
                                    unblockDocument(project, this)
                                }
                                with(FileDocumentManager.getInstance()) {
                                    saveDocument(editor.document)
                                }
                            }
                        } ?: run {
                            File(model.containingFilePath).writeText(currentFileText + line)
                        }
                        currentFileText += line
                    }
                }
            }
        }
    }
}

private fun PackageDataService.checkAndInstallRequirements(project: Project): Boolean {
    val missingPackages = jsPackagesList.filterNot { this.findPackage(it) }
    if (missingPackages.isEmpty()) return true
    val message = """
            Requirements are not installed:
            ${missingPackages.joinToString { it.packageName }}
            Install them?
        """.trimIndent()
    val result = Messages.showOkCancelDialog(
        project, message, "Requirements Missmatch Error", "Install", "Cancel", null
    )

    if (result == Messages.CANCEL)
        return false

    try {
        val (_, errorText) = this.installMissingPackages(missingPackages)
        if (errorText.isNotEmpty()) {
            showErrorDialogLater(
                project,
                "Requirements installing failed with some reason:\n${errorText}",
                "Failed to install requirements"
            )
            return false
        }
        return true
    } catch (_: TimeoutException) {
        showErrorDialogLater(
            project,
            """
                Requirements installing failed due to the exceeded waiting time for the installation, check your internet connection.
                
                Try to install missing npm packages manually:
            ${
                missingPackages.joinToString(separator = "\n") {
                    "> npm install ${it.npmListFlag} ${it.packageName}"
                }
            }
            """.trimIndent(),
            "Failed to install requirements"
        )
        return false
    }
}
