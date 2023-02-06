package org.utbot.intellij.plugin.language.js

import com.intellij.openapi.ui.Messages
import utils.JsCmdExec
import utils.OsProvider

fun getFrameworkLibraryPath(npmPackageName: String, model: JsTestsModel?): String? {
    val (inputText, _) = JsCmdExec.runCommand(
        dir = model?.project?.basePath!!,
        shouldWait = true,
        timeout = 10,
        cmd = arrayOf(OsProvider.getProviderByOs().getAbstractivePathTool(), npmPackageName)
    )

    if (!inputText.contains(npmPackageName) && !findFrameworkLibrary(npmPackageName, model)) {
        installMissingRequirement(model.project, model.pathToNPM, npmPackageName)
        return null
    }
    return inputText.substringBefore(npmPackageName) + npmPackageName
}

private fun npmListByFlag(model: JsTestsModel, flag: String): String {
    val (inputText, _) = JsCmdExec.runCommand(
        dir = model.project.basePath!!,
        shouldWait = true,
        timeout = 10,
        cmd = arrayOf(model.pathToNPM, "list", flag)
    )
    return inputText
}

fun findFrameworkLibrary(npmPackageName: String, model: JsTestsModel): Boolean {
    val packageText = npmListByFlag(model, "-g") + npmListByFlag(model, "-l")

    if (packageText.isEmpty()) {
        Messages.showErrorDialog(
            model.project,
            "Node.js is not installed",
            "Generation Failed",
        )
        return false
    }
    return packageText.contains(npmPackageName)
}

fun installRequirement(pathToNPM: String, requirement: String, installingDir: String?): Pair<String, String> {
    val installationType = if (requirement == "mocha") "-l" else "-g"

    val (inputText, errorText) = JsCmdExec.runCommand(
        dir = installingDir,
        shouldWait = true,
        timeout = 10,
        cmd = arrayOf(pathToNPM, "install", installationType) + requirement
    )

    return Pair(inputText, errorText)
}
