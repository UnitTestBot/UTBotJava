package settings

import utils.JsCmdExec
import utils.OsProvider

object JsPackagesSettings {
    val mochaData: PackageData = PackageData("mocha", "-l")
    val nycData: PackageData = PackageData("nyc", "-g")

    // TODO(MINOR): Add tern auto installation
    val ternData: PackageData? = null
}

data class PackageData(
    val packageName: String,
    val npmListFlag: String
) {
    fun findPackageByNpm(projectBasePath: String, pathToNpm: String): Boolean {
        val (inputText, _) = JsCmdExec.runCommand(
            dir = projectBasePath,
            shouldWait = true,
            timeout = 10,
            cmd = arrayOf("\"$pathToNpm\"", "list", npmListFlag)
        )

        return inputText.contains(packageName)
    }

    fun findPackagePath(): String? {
        val (inputText, _) = JsCmdExec.runCommand(
            dir = null,
            shouldWait = true,
            timeout = 10,
            cmd = arrayOf(OsProvider.getProviderByOs().getAbstractivePathTool(), packageName)
        )

        return inputText.split(System.lineSeparator()).first().takeIf { it.contains(packageName) }
    }

    fun installPackage(projectBasePath: String, pathToNpm: String): Pair<String, String> {
        val (inputText, errorText) = JsCmdExec.runCommand(
            dir = projectBasePath,
            shouldWait = true,
            timeout = 10,
            cmd = arrayOf("\"$pathToNpm\"", "install", npmListFlag, packageName)
        )

        return Pair(inputText, errorText)
    }
}
