package service

import org.apache.commons.io.FileUtils
import org.json.JSONException
import org.json.JSONObject
import org.utbot.framework.plugin.api.TimeoutException
import utils.JsCmdExec
import java.io.File
import java.util.Collections

class CoverageService(
    private val context: ServiceContext,
    private val scriptText: String,
    private val id: Int,
    private val originalFileName: String,
    private val newFileName: String,
    private val basicCoverage: List<Int> = emptyList(),
    val errors: MutableList<Int>
) {
    init {
        with(context) {
            generateCoverageReport(projectPath, filePathToInference)
        }
    }

    fun getCoveredLines(): List<Int> {
        if (id in errors) return emptyList()
        val jsonText = with(context) {
            val file =
                File("$projectPath${File.separator}$utbotDir${File.separator}coverage$id${File.separator}coverage-final.json")
            file.readText()
        }
        val json = JSONObject(jsonText)
        try {
            val neededKey = json.keySet().find { it.contains(originalFileName) }
            json.getJSONObject(neededKey)
            val coveredStatements = json
                .getJSONObject(neededKey)
                .getJSONObject("s")
            val result = coveredStatements.keySet().flatMap {
                val count = coveredStatements.getInt(it)
                Collections.nCopies(count, it.toInt())
            }.toMutableList()
            basicCoverage.forEach {
                result.remove(it)
            }
            return result
        } catch (e: JSONException) {
            return emptyList()
        }
    }

    fun removeTempFiles() {
        with(context) {
            FileUtils.deleteDirectory(File("$projectPath${File.separator}$utbotDir${File.separator}coverage$id"))
        }
    }

    private fun generateCoverageReport(workingDir: String, filePath: String) {
        val dir = File("$workingDir${File.separator}${context.utbotDir}${File.separator}coverage$id")
            .also { it.mkdir() }
        try {
            val (_, error) = when (originalFileName) {
                newFileName -> {
                    JsCmdExec.runCommand(
                        cmd = "nyc " +
                                "--report-dir=${dir.absolutePath} " +
                                "--reporter=\"json\" " +
                                "--temp-dir=${dir.absolutePath}${File.separator}cache$id " +
                                "node $filePath",
                        shouldWait = true,
                        dir = workingDir,
                        timeout = context.nodeTimeout,
                    )
                }

                else -> {
                    JsCmdExec.runCommand(
                        cmd = "nyc " +
                                "--report-dir=${dir.absolutePath} " +
                                "--reporter=\"json\" " +
                                "--temp-dir=${dir.absolutePath}${File.separator}cache$id " +
                                "node -e \"$scriptText\" ",
                        shouldWait = true,
                        dir = File(filePath).parent,
                        timeout = context.nodeTimeout,
                    )
                }
            }
            val errText = error.readText()
            if (errText.isNotEmpty()) {
                println(errText)
            }
        } catch (e: TimeoutException) {
            errors += id
        }
    }
}