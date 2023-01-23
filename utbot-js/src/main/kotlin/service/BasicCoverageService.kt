package service

import java.io.File
import java.util.Collections
import org.apache.commons.io.FileUtils
import org.json.JSONException
import org.json.JSONObject
import org.utbot.framework.plugin.api.TimeoutException
import settings.JsTestGenerationSettings.tempFileName
import utils.CoverageData
import utils.JsCmdExec

// TODO: 1. Make searching for file coverage in coverage report more specific, not just by file name.
class BasicCoverageService(
    private val context: ServiceContext,
    private val scriptTexts: List<String>,
    private val testCaseIndices: IntRange,
) : ICoverageService {

    private val errors = mutableListOf<Int>()
    private var baseCoverage = emptyList<Int>()
    private val utbotDirPath = "${context.projectPath}/${context.utbotDir}"
    private val _resultList = mutableListOf<Pair<Int, String>>()
    val resultList: List<String>
        get() = _resultList
            .sortedBy { (index, _) -> index }
            .map { (_, obj) -> obj }

    init {
        generateTempFiles()
        baseCoverage = getBaseCoverage()
        generateCoverageReportForAllFiles()
    }

    override fun getCoveredLines(): List<CoverageData> {
        try {
            val res = testCaseIndices.map { index ->
                if (index in errors) CoverageData(emptySet(), baseCoverage.toSet()) else {
                    val fileCoverage =
                        getCoveragePerFile(context.filePathToInference.substringAfterLast("/"), index).toSet()
                    val resFile = File("$utbotDirPath/$tempFileName$index.json")
                    val rawResult = resFile.readText()
                    resFile.delete()
                    val json = JSONObject(rawResult)
                    _resultList.add(index to json.get("result").toString())
                    CoverageData(baseCoverage.toSet(), fileCoverage)
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
        val jsonText = with(context) {
            val file =
                File("$utbotDirPath/coverage$index/coverage-final.json")
            file.readText()
        }
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

    private fun getBaseCoverage(): List<Int> {
        generateCoverageReport(context.filePathToInference, 0)
        return getCoveragePerFile(context.filePathToInference.substringAfterLast("/"), 0)
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
            with(context) {
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
                        dir = context.projectPath,
                        timeout = settings.timeout,
                    )
                val errText = error.readText()
                if (errText.isNotEmpty()) {
                    println(errText)
                }
            }
        } catch (e: TimeoutException) {
            errors += index
            _resultList.add(index to "Error:Timeout")
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