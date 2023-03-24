package service

import org.json.JSONObject
import java.io.File
import java.io.FilenameFilter

data class PackageJson(
    val isModule: Boolean,
    val deps: Set<String>
) {
    companion object {
        val defaultConfig = PackageJson(false, emptySet())
    }
}

class PackageJsonService(
    private val filePathToInference: String,
    private val projectPath: String
) {

    fun findClosestConfig(): PackageJson {
        var currDir = File(filePathToInference)
        do {
            currDir = currDir.parentFile
            val matchingFiles: Array<File> = currDir.listFiles(
                FilenameFilter { _, name ->
                    return@FilenameFilter name == "package.json"
                }
            ) ?: throw IllegalStateException("Error occurred while scanning file system")
            if (matchingFiles.isNotEmpty()) return parseConfig(matchingFiles.first())
        } while (currDir.path.replace("\\", "/") != projectPath)
        return PackageJson.defaultConfig
    }

    private fun parseConfig(configFile: File): PackageJson {
        val configAsJson = JSONObject(configFile.readText())
        return PackageJson(
            isModule = configAsJson.optString("type") == "module",
            deps = configAsJson.optJSONObject("dependencies")?.keySet() ?: emptySet()
        )
    }
}
