package service

import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import org.json.JSONException
import org.json.JSONObject
import settings.JsTestGenerationSettings.fuzzingThreshold
import settings.JsTestGenerationSettings.tempFileName
import utils.CoverageData
import utils.JsCmdExec
import utils.ResultData

class FastCoverageService(
    context: ServiceContext,
    private val scriptTexts: List<String>,
    private val testCaseIndices: IntRange,
    val baseCoverage: List<Int>,
) : ICoverageService, ContextOwner by context {

    private val utbotDirPath = "${projectPath}/${utbotDir}"
    private val coverageList = ConcurrentLinkedQueue<Pair<Int, JSONObject>>()
    private val _resultList = ConcurrentLinkedQueue<ResultData>()
    val resultList: List<ResultData>
        get() = _resultList
            .sortedBy { (_, index, _, _, _) -> index }

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
                    path = "$utbotDirPath/${tempFileName}Base.js",
                    scriptText = baseCoverageScriptText
                )
                JsCmdExec.runCommand(
                    cmd = arrayOf("\"${settings.pathToNode}\"", "\"$utbotDirPath/${tempFileName}Base.js\""),
                    dir = projectPath,
                    shouldWait = true,
                    timeout = settings.timeout,
                )
                return JSONObject(File("$utbotDirPath/${tempFileName}Base.json").readText())
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
        generateCoverageReport()
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

    override fun getCoveredLines(): List<CoverageData> {
        try {
            return coverageList.sortedBy { (index, _) -> index }
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

    private fun removeTempFiles() {
//        File("$utbotDirPath/${tempFileName}Base.js").delete()
//        File("$utbotDirPath/${tempFileName}Base.json").delete()
//        for (index in testCaseIndices) {
//            File("$utbotDirPath/$tempFileName$index.json").delete()
//        }
//        for (index in scriptTexts.indices) {
//            File("$utbotDirPath/$tempFileName$index.js").delete()
//        }
    }


    private fun generateCoverageReport() {
        scriptTexts.indices.toList().parallelStream().forEach { parallelIndex ->
            val (_, error) = JsCmdExec.runCommand(
                cmd = arrayOf("\"${settings.pathToNode}\"", "\"$utbotDirPath/$tempFileName$parallelIndex.js\""),
                dir = projectPath,
                shouldWait = true,
                timeout = settings.timeout,
            )
            for (i in parallelIndex * fuzzingThreshold..minOf(
                (parallelIndex + 1) * fuzzingThreshold - 1, testCaseIndices.last)
            ) {
                val resFile = File("$utbotDirPath/$tempFileName$i.json")
                val rawResult = resFile.readText()
//                resFile.delete()
                val json = JSONObject(rawResult)
                val index = json.getInt("index")
                if (index != i) println("ERROR: index $index != i $i")
                coverageList.add(index to json.getJSONObject("s"))
                val resultData = ResultData(
                    rawString = json.get("result").toString(),
                    index = index,
                    isNan = json.getBoolean("is_nan"),
                    isInf = json.getBoolean("is_inf"),
                    specSign = json.getInt("spec_sign").toByte()
                )
                _resultList.add(resultData)
            }
            val errText = error.readText()
            if (errText.isNotEmpty()) {
                println(errText)
            }
        }
    }
}