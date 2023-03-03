package org.utbot.intellij.plugin.language.go.generator

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
import org.utbot.go.logic.GoUtTestsGenerationConfig
import org.utbot.intellij.plugin.language.go.models.GenerateGoTestsModel
import org.utbot.intellij.plugin.language.go.ui.GenerateGoTestsDialogWindow
import org.utbot.intellij.plugin.language.go.ui.utils.resolveGoExecutablePath

object GoUtTestsDialogProcessor {

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

        return GenerateGoTestsDialogWindow(
            GenerateGoTestsModel(
                project,
                goSdk.resolveGoExecutablePath()!!,
                targetFunctions,
                focusedTargetFunctions,
            )
        )
    }

    private fun createTests(model: GenerateGoTestsModel) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(model.project, "Generate Go tests") {
            override fun run(indicator: ProgressIndicator) {
                // readAction is required to read PSI-tree or else "Read access" exception occurs.
                val selectedFunctionsNamesBySourceFiles = runReadAction {
                    model.selectedFunctions.groupBy({ it.containingFile.virtualFile.canonicalPath!! }) { it.name!! }
                }
                val testsGenerationConfig = GoUtTestsGenerationConfig(
                    model.goExecutableAbsolutePath,
                    model.eachFunctionExecutionTimeoutMillis,
                    model.allFunctionExecutionTimeoutMillis
                )

                IntellijGoUtTestsGenerationController(model, indicator).generateTests(
                    selectedFunctionsNamesBySourceFiles, testsGenerationConfig
                ) { indicator.isCanceled }
            }
        })
    }
}