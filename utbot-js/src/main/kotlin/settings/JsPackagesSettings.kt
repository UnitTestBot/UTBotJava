package settings

import java.io.File
import org.utbot.common.PathUtil.replaceSeparator
import service.PackageJsonService
import settings.JsPackagesSettings.mochaData
import settings.JsPackagesSettings.nycData
import settings.JsPackagesSettings.ternData
import utils.JsCmdExec
import utils.OsProvider

object JsPackagesSettings {
    val mochaData: PackageData = PackageData("mocha", NpmListFlag.L)
    val nycData: PackageData = PackageData("nyc", NpmListFlag.G)
    val ternData: PackageData = PackageData("tern", NpmListFlag.L)
}

val jsPackagesList = listOf(
    mochaData,
    nycData,
    ternData
)

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
}

class PackageDataService(
    filePathToInference: String,
    private val projectPath: String,
    private val pathToNpm: String,
) {
    private val packageJson = PackageJsonService(filePathToInference, File(projectPath)).findClosestConfig()

    companion object {
        var nycPath: String = ""
            private set
    }

    fun findPackage(packageData: PackageData): Boolean = with(packageData) {
        when (npmListFlag) {
            NpmListFlag.G -> {
                val (inputText, _) = JsCmdExec.runCommand(
                    dir = projectPath,
                    shouldWait = true,
                    timeout = 10,
                    cmd = arrayOf("\"$pathToNpm\"", "list", npmListFlag.toString())
                )
                var result = inputText.contains(packageName)
                if (!result || this == nycData) {
                    val packagePath = this.findPackagePath()
                    nycPath = packagePath?.let {
                        replaceSeparator(it) + OsProvider.getProviderByOs().npmPackagePostfix
                    } ?: "Nyc was not found"
                    if (!result) {
                        result = this.findPackagePath()?.isNotBlank() ?: false
                    }
                }
                return result
            }

            NpmListFlag.L -> {
                packageJson.deps.contains(packageName)
            }
        }
    }

    fun installMissingPackages(packages: List<PackageData>): Pair<String, String> {
        var inputTextAllPackages = ""
        var errorTextAllPackages = ""
        if (packages.isEmpty()) return inputTextAllPackages to errorTextAllPackages

        val localPackageNames = packages.filter { it.npmListFlag == NpmListFlag.L }
            .map { it.packageName }.toTypedArray()
        val globalPackageNames = packages.filter { it.npmListFlag == NpmListFlag.G }
            .map { it.packageName }.toTypedArray()

        // Local packages installation
        if (localPackageNames.isNotEmpty()) {
            val (inputText, errorText) = JsCmdExec.runCommand(
                dir = projectPath,
                shouldWait = true,
                timeout = 10,
                cmd = arrayOf("\"$pathToNpm\"", "install", NpmListFlag.L.toString(), *localPackageNames)
            )
            inputTextAllPackages += inputText
            errorTextAllPackages += errorText
        }
        // Global packages installation
        if (globalPackageNames.isNotEmpty()) {
            val (inputText, errorText) = JsCmdExec.runCommand(
                dir = projectPath,
                shouldWait = true,
                timeout = 10,
                cmd = arrayOf("\"$pathToNpm\"", "install", NpmListFlag.G.toString(), *globalPackageNames)
            )
            inputTextAllPackages += inputText
            errorTextAllPackages += errorText
        }
        // Find path to nyc execution file after installation
        if (packages.contains(nycData)) findPackage(nycData)

        return Pair(inputTextAllPackages, errorTextAllPackages)
    }
}
