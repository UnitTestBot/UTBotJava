package service

import java.io.File
import java.io.FilenameFilter
import org.json.JSONObject

data class PackageJson(
    val isModule: Boolean
) {
    companion object {
        val defaultConfig = PackageJson(false)
    }
}


class PackageJsonService(context: ServiceContext) : ContextOwner by context {

    fun findClosestConfig(): PackageJson {
        var currDir = File(filePathToInference.substringBeforeLast("/"))
        do {
            val matchingFiles: Array<File> = currDir.listFiles(
                FilenameFilter { _, name ->
                    return@FilenameFilter name == "package.json"
                }
            ) ?: throw IllegalStateException("Error occurred while scanning file system")
            if (matchingFiles.isNotEmpty()) return parseConfig(matchingFiles.first())
            currDir = currDir.parentFile
        } while (currDir.path != projectPath)
        return PackageJson.defaultConfig
    }

    private fun parseConfig(configFile: File): PackageJson {
        val configAsJson = JSONObject(configFile.readText())
        return PackageJson(
            isModule = (configAsJson.getString("type") == "module"),
        )
    }
}