package org.utbot.intellij.plugin.language.js

import com.intellij.openapi.ui.Messages
import utils.JsCmdExec
import utils.OsProvider
import java.io.BufferedReader

fun getFrameworkLibraryPath(npmPackageName: String, model: JsTestsModel?): String? {
    val (bufferedReader, errorReader) = JsCmdExec.runCommand(
        dir = model?.project?.basePath!!,
        shouldWait = true,
        timeout = 10,
        cmd = arrayOf(OsProvider.getProviderByOs().getAbstractivePathTool(), npmPackageName)
    )
    val input = bufferedReader.readText()
    val error = errorReader.readText()

    if ((error.isNotEmpty() or !input.contains(npmPackageName)) && !findFrameworkLibrary(npmPackageName, model)) {
        installMissingRequirement(model.project, model.pathToNPM, npmPackageName)
        return null
    }
    return input.substringBefore(npmPackageName) + npmPackageName
}

private fun npmListByFlag(model: JsTestsModel, flag: String): String {
    val (bufferReader, _) = JsCmdExec.runCommand(
        dir = model.project.basePath!!,
        shouldWait = true,
        timeout = 10,
        cmd = arrayOf("\"${model.pathToNPM}\"", "list", flag)
    )
    val packages = bufferReader.readText()
    bufferReader.close()
    return packages
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

fun installRequirement(pathToNPM: String, requirement: String, installingDir: String?): Pair<BufferedReader, BufferedReader> {
    val installationType = if (requirement == "mocha") "-l" else "-g"

    val (buf1, buf2) = JsCmdExec.runCommand(
        dir = installingDir,
        shouldWait = true,
        timeout = 10,
        cmd = arrayOf("\"$pathToNPM\"", "install", installationType) + requirement
    )
    return buf1 to buf2
}