package org.utbot.intellij.plugin.language.js

import com.intellij.openapi.ui.Messages
import utils.JsCmdExec
import utils.OsProvider

fun getFrameworkLibraryPath(npmPackageName: String, model: JsTestsModel): String? {
    val (bufferedReader, errorReader) = JsCmdExec.runCommand(
        dir = model.project.basePath!!,
        shouldWait = true,
        timeout = 10,
        cmd = arrayOf(OsProvider.getProviderByOs().getAbstractivePathTool(), model.pathToNYC)
    )
    val input = bufferedReader.readText()
    val error = errorReader.readText()

    if (error.isNotEmpty() or !input.contains(npmPackageName)) {
        if (findFrameworkLibrary(npmPackageName, model)) {
            Messages.showErrorDialog(
                model.project,
                "The following packages were not found, please set it in menu manually:\n $npmPackageName",
                "$npmPackageName missing!",
            )
        } else {
            Messages.showErrorDialog(
                model.project,
                "The following packages are not installed: $npmPackageName \nPlease install it via npm `> npm i -g $npmPackageName`",
                "$npmPackageName missing!",
            )
        }
        return null
    }
    return input.substringBefore(npmPackageName) + npmPackageName
}

fun findFrameworkLibrary(npmPackageName: String, model: JsTestsModel): Boolean {
    val (bufferedReader, _) = JsCmdExec.runCommand(
        dir = model.project.basePath!!,
        shouldWait = true,
        timeout = 10,
        cmd = arrayOf(model.pathToNPM, "list", "-g")
    )
    val checkForPackageText = bufferedReader.readText()
    bufferedReader.close()
    if (checkForPackageText == "") {
        Messages.showErrorDialog(
            model.project,
            "Node.js is not installed",
            "Generation Failed",
        )
        return false
    }
    return checkForPackageText.contains(npmPackageName)
}
