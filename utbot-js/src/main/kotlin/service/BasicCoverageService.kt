package service

import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.commons.io.FileUtils
import org.json.JSONException
import org.json.JSONObject
import org.utbot.framework.plugin.api.TimeoutException
import settings.JsTestGenerationSettings.tempFileName
import utils.CoverageData
import utils.JsCmdExec
import utils.ResultData

// TODO: 1. Make searching for file coverage in coverage report more specific, not just by file name.
class BasicCoverageService(
    context: ServiceContext,
    private val scriptTexts: List<String>,
    private val testCaseIndices: IntRange,
    private val baseCoverage: List<Int>
) : ICoverageService, ContextOwner by context {

    private val errors = ConcurrentLinkedQueue<Int>()
    private val utbotDirPath = "${context.projectPath}/${context.utbotDir}"
    private val _resultList = ConcurrentLinkedQueue<ResultData>()
    val resultList: List<ResultData>
        get() = _resultList
            .sortedBy { (_, index, _, _, _) -> index }

    init {
        generateTempFiles()
        generateCoverageReportForAllFiles()
    }

    override fun getCoveredLines(): List<CoverageData> {
        try {
            val res = testCaseIndices.map { index ->
                if (index in errors) CoverageData(emptySet()) else {
                    val fileCoverage =
                        getCoveragePerFile(filePathToInference.substringAfterLast("/"), index).toSet()
                    val resFile = File("$utbotDirPath/$tempFileName$index.json")
                    val rawResult = resFile.readText()
                    resFile.delete()
                    val json = JSONObject(rawResult)
                    val resultData = ResultData(
                        rawString = json.get("result").toString(),
                        index = index,
                        isNan = json.getBoolean("is_nan"),
                        isInf = json.getBoolean("is_inf"),
                        specSign = json.getInt("spec_sign").toByte()
                    )
                    _resultList.add(resultData)
                    CoverageData(fileCoverage)
                }
            }
            return res
        } catch (e: Exception) {
            throw Exception("Could not get coverage of test cases!")
        } finally {
            removeTempFiles()
        }
    }

    private fun getCoveragePerFile(fileName: String, index: Int): List<Int> {
        if (index in errors) return emptyList()
        val file =
            File("$utbotDirPath/coverage$index/coverage-final.json")
        val jsonText = file.readText()
        val json = JSONObject(jsonText)
        try {
            val neededKey = json.keySet().find { it.contains(fileName) }
            json.getJSONObject(neededKey)
            val coveredStatements = json
                .getJSONObject(neededKey)
                .getJSONObject("s")
            val result = coveredStatements.keySet().flatMap {
                val count = coveredStatements.getInt(it)
                Collections.nCopies(count, it.toInt())
            }.toMutableList()
            baseCoverage.forEach {
                result.remove(it)
            }
            return result
        } catch (e: JSONException) {
            return emptyList()
        }
    }

    private fun removeTempFiles() {
        for (index in testCaseIndices) {
            File("$utbotDirPath/$tempFileName$index.js").delete()
            FileUtils.deleteDirectory(File("$utbotDirPath/coverage$index"))
            FileUtils.deleteDirectory(File("$utbotDirPath/cache$index"))
        }
    }

    private fun generateCoverageReportForAllFiles() {
        testCaseIndices.toList().parallelStream().forEach { parallelIndex ->
            generateCoverageReport("$utbotDirPath/$tempFileName$parallelIndex.js", parallelIndex)
        }
    }

    private fun generateCoverageReport(filePath: String, index: Int) {
        try {
            val (_, error) =
                JsCmdExec.runCommand(
                    cmd = arrayOf(
                        "\"${settings.pathToNYC}\"",
                        "--report-dir=\"$utbotDir/coverage$index\"",
                        "--reporter=json",
                        "--temp-dir=\"$utbotDir/cache$index\"",
                        "\"${settings.pathToNode}\"",
                        "\"$filePath\""
                    ),
                    shouldWait = true,
                    dir = projectPath,
                    timeout = settings.timeout,
                )
            val errText = error.readText()
            if (errText.isNotEmpty()) {
                println(errText)
            }
        } catch (e: TimeoutException) {
            errors += index
            val resultData = ResultData(
                rawString = "Error:Timeout",
                index = index,
            )
            _resultList.add(resultData)
        }
    }

    private fun generateTempFiles() {
        scriptTexts.forEachIndexed { index, scriptText ->
            val tempScriptPath = "$utbotDirPath/$tempFileName$index.js"
            createTempScript(
                path = tempScriptPath,
                scriptText = scriptText
            )
        }
    }

    private fun createTempScript(path: String, scriptText: String) {
        val file = File(path)
        file.writeText(scriptText)
        file.createNewFile()
    }
}