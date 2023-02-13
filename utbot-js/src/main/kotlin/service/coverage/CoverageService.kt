package service.coverage

import java.io.File
import java.util.Collections
import org.json.JSONException
import org.json.JSONObject
import service.ContextOwner
import service.ServiceContext
import settings.JsTestGenerationSettings
import utils.JsCmdExec
import utils.data.CoverageData
import utils.data.ResultData

abstract class CoverageService(
    context: ServiceContext,
    private val scriptTexts: List<String>,
    private val baseCoverage: List<Int>,
): ContextOwner by context {

    private val _utbotDirPath = lazy { "${projectPath}/${utbotDir}" }
    protected val utbotDirPath: String
        get() = _utbotDirPath.value
    protected val coverageList = mutableListOf<Pair<Int, JSONObject>>()
    protected val _resultList = mutableListOf<ResultData>()
    val resultList: List<ResultData>
        get() = _resultList.toList()

    companion object {

        private fun createTempScript(path: String, scriptText: String) {
            val file = File(path)
            file.writeText(scriptText)
            file.createNewFile()
        }

        fun getBaseCoverage(context: ServiceContext, baseCoverageScriptText: String): List<Int> {
            with(context) {
                val utbotDirPath = "${projectPath}/${utbotDir}"
                createTempScript(
                    path = "$utbotDirPath/${JsTestGenerationSettings.tempFileName}Base.js",
                    scriptText = baseCoverageScriptText
                )
                JsCmdExec.runCommand(
                    cmd = arrayOf(
                        "\"${settings.pathToNode}\"",
                        "\"$utbotDirPath/${JsTestGenerationSettings.tempFileName}Base.js\""
                    ),
                    dir = projectPath,
                    shouldWait = true,
                    timeout = settings.timeout,
                )
                return JSONObject(File("$utbotDirPath/${JsTestGenerationSettings.tempFileName}Base.json").readText())
                    .getJSONObject("s").let {
                        it.keySet().flatMap { key ->
                            val count = it.getInt(key)
                            Collections.nCopies(count, key.toInt())
                        }
                    }
            }
        }
    }

    init {
        generateTempFiles()
    }

    private fun generateTempFiles() {
        scriptTexts.forEachIndexed { index, scriptText ->
            val tempScriptPath = "$utbotDirPath/${JsTestGenerationSettings.tempFileName}$index.js"
            createTempScript(
                path = tempScriptPath,
                scriptText = scriptText
            )
        }
    }

    fun getCoveredLines(): List<CoverageData> {
        try {
            // TODO: sort by coverage size desc
            return coverageList
                .map { (_, obj) ->
                    val dirtyCoverage = obj
                        .let {
                            it.keySet().flatMap { key ->
                                val count = it.getInt(key)
                                Collections.nCopies(count, key.toInt())
                            }.toMutableList()
                        }
                    baseCoverage.forEach {
                        dirtyCoverage.remove(it)
                    }
                    CoverageData(dirtyCoverage.toSet())
                }
        } catch (e: JSONException) {
            throw Exception("Could not get coverage of test cases!")
        } finally {
            removeTempFiles()
        }
    }

    abstract fun generateCoverageReport()


    private fun createTempScript(path: String, scriptText: String) {
        val file = File(path)
        file.writeText(scriptText)
        file.createNewFile()
    }

    private fun removeTempFiles() {
        File("$utbotDirPath/${JsTestGenerationSettings.tempFileName}Base.js").delete()
        File("$utbotDirPath/${JsTestGenerationSettings.tempFileName}Base.json").delete()
        for (index in scriptTexts.indices) {
            File("$utbotDirPath/${JsTestGenerationSettings.tempFileName}$index.js").delete()
        }
    }
}