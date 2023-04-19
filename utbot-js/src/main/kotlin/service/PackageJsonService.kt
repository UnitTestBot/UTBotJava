package service

import java.io.File
import java.io.FilenameFilter
import org.json.JSONObject

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
    private val projectDir: File
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
        } while (currDir != projectDir)
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
