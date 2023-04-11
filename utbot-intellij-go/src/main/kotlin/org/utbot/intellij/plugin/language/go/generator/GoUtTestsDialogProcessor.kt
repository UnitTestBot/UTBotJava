package org.utbot.intellij.plugin.language.go.generator

import com.goide.project.DefaultGoRootsProvider
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.GoSdkVersion
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.utbot.go.gocodeanalyzer.GoParsingSourceCodeAnalysisResultException
import org.utbot.go.logic.GoUtTestsGenerationConfig
import org.utbot.go.worker.GoWorkerFailedException
import org.utbot.intellij.plugin.language.go.models.GenerateGoTestsModel
import org.utbot.intellij.plugin.language.go.ui.GenerateGoTestsDialogWindow
import org.utbot.intellij.plugin.language.go.ui.utils.resolveGoExecutablePath
import org.utbot.intellij.plugin.ui.utils.showErrorDialogLater
import java.nio.file.Paths

object GoUtTestsDialogProcessor {

    private const val helpMessage: String =
        "Please try running \"go mod tidy\" in one of the project directories or fix any errors in the code."

    fun createDialogAndGenerateTests(
        project: Project,
        module: Module,
        targetFunctions: Set<GoFunctionOrMethodDeclaration>,
        focusedTargetFunctions: Set<GoFunctionOrMethodDeclaration>,
    ) {
        createDialog(project, module, targetFunctions, focusedTargetFunctions)?.let {
            if (it.showAndGet()) createTests(it.model)
        }
    }

    private fun createDialog(
        project: Project,
        module: Module,
        targetFunctions: Set<GoFunctionOrMethodDeclaration>,
        focusedTargetFunctions: Set<GoFunctionOrMethodDeclaration>,
    ): GenerateGoTestsDialogWindow? {
        val goSdk = GoSdkService.getInstance(project).getSdk(module)
        if (goSdk == GoSdk.NULL) {
            val result = Messages.showOkCancelDialog(
                project,
                "GOROOT is not defined. Select it?",
                "Unsupported Go SDK",
                "Select",
                "Cancel",
                Messages.getErrorIcon()
            )
            if (result == Messages.OK) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "GOROOT")
            }
            return null
        } else if (!goSdk.isValid || GoSdkVersion.fromText(goSdk.version).isLessThan(GoSdkVersion.GO_1_18)) {
            val result = Messages.showOkCancelDialog(
                project,
                "Go SDK isn't valid or version less than 1.18. Select another SDK?",
                "Unsupported Go SDK",
                "Select",
                "Cancel",
                Messages.getErrorIcon()
            )
            if (result == Messages.OK) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "GOROOT")
            }
            return null
        }

        val goPath = DefaultGoRootsProvider().getGoPathRoots(project, module).first().path
        return GenerateGoTestsDialogWindow(
            GenerateGoTestsModel(
                project,
                goExecutableAbsolutePath = Paths.get(goSdk.resolveGoExecutablePath()!!).toAbsolutePath(),
                gopathAbsolutePath = Paths.get(goPath).toAbsolutePath(),
                targetFunctions,
                focusedTargetFunctions,
            )
        )
    }

    private fun buildErrorMessage(exception: Exception): String =
        if (exception.message == null) {
            helpMessage
        } else {
            buildString {
                appendLine(exception.message)
                appendLine(helpMessage)
            }
        }

    private fun createTests(model: GenerateGoTestsModel) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(model.project, "Generate Go tests") {
            override fun run(indicator: ProgressIndicator) {
                // readAction is required to read PSI-tree or else "Read access" exception occurs.
                val selectedFunctionsNamesBySourceFiles = runReadAction {
                    model.selectedFunctions.groupBy({ Paths.get(it.containingFile.virtualFile.path) }) { it.name!! }
                }
                val testsGenerationConfig = GoUtTestsGenerationConfig(
                    model.goExecutableAbsolutePath,
                    model.gopathAbsolutePath,
                    model.eachFunctionExecutionTimeoutMillis,
                    model.allFunctionExecutionTimeoutMillis
                )

                try {
                    IntellijGoUtTestsGenerationController(model, indicator).generateTests(
                        selectedFunctionsNamesBySourceFiles, testsGenerationConfig
                    ) { indicator.isCanceled }
                } catch (e: GoParsingSourceCodeAnalysisResultException) {
                    val errorMessage = buildErrorMessage(e)
                    showErrorDialogLater(
                        model.project,
                        errorMessage,
                        title = "Unit tests generation is cancelled"
                    )
                } catch (e: GoWorkerFailedException) {
                    showErrorDialogLater(
                        model.project,
                        helpMessage,
                        title = "Unit tests generation is cancelled"
                    )
                }
            }
        })
    }
}