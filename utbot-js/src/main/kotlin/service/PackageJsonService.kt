package service

import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FilenameFilter

data class PackageJson(
    val isModule: Boolean
) {
    companion object {
        val defaultConfig = PackageJson(false)
    }
}


class PackageJsonService(context: ServiceContext) : ContextOwner by context {

    fun findClosestConfig(): PackageJson {
        var currDir = File(filePathToInference.first().substringBeforeLast("/"))
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
            isModule = try {
                (configAsJson.getString("type") == "module")
            } catch (e: JSONException) {
                false
            },
        )
    }
}
