package settings

import service.PackageJsonService
import utils.JsCmdExec
import utils.OsProvider

object JsPackagesSettings {
    val mochaData: PackageData = PackageData("mocha", NpmListFlag.L)
    val nycData: PackageData = PackageData("nyc", NpmListFlag.G)
    val ternData: PackageData = PackageData("tern", NpmListFlag.L)
}

enum class NpmListFlag {
    L {
        override fun toString(): String = "-l"
    },
    G {
        override fun toString(): String = "-g"
    }
}

data class PackageData(
    val packageName: String,
    val npmListFlag: NpmListFlag
) {

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
            cmd = arrayOf("\"$pathToNpm\"", "install", npmListFlag.toString(), packageName)
        )

        return Pair(inputText, errorText)
    }
}

class PackageDataService(
    filePathToInference: String,
    projectPath: String,
) {
    private val packageJson = PackageJsonService(filePathToInference, projectPath).findClosestConfig()

    fun findPackageByNpm(packageData: PackageData, projectBasePath: String, pathToNpm: String): Boolean = with(packageData) {
        when (npmListFlag) {
            NpmListFlag.G -> {
                val (inputText, _) = JsCmdExec.runCommand(
                    dir = projectBasePath,
                    shouldWait = true,
                    timeout = 10,
                    cmd = arrayOf("\"$pathToNpm\"", "list", npmListFlag.toString())
                )

                inputText.contains(packageName)
            }
            NpmListFlag.L -> {
                packageJson.deps.contains(packageName)
            }
        }
    }
}
