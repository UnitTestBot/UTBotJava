package service

import java.io.File
import java.util.Collections
import org.apache.commons.io.FileUtils
import org.json.JSONException
import org.json.JSONObject
import settings.JsTestGenerationSettings.tempFileName
import utils.JsCmdExec

class FastCoverageService(
    private val context: ServiceContext,
    private val scriptTexts: List<String>,
    private val testCaseIndices: IntRange,
    private val baseCoverageScriptText: String,
): ICoverageService {

    private val utbotDirPath = "${context.projectPath}/${context.utbotDir}"
    private val coverageList = mutableListOf<Pair<Int, JSONObject>>()
    private val _resultList = mutableListOf<Pair<Int, String>>()
    val resultList: List<String>
        get() = _resultList
            .sortedBy { (index, _) -> index }
            .map { (_, obj) -> obj }
    private var baseCoverage: List<Int>

    init {
        generateTempFiles()
        baseCoverage = getBaseCoverage()
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
        createTempScript(
            path = "$utbotDirPath/${tempFileName}Base.js",
            scriptText = baseCoverageScriptText
        )
    }

    private fun getBaseCoverage(): List<Int> {
        with(context) {
            JsCmdExec.runCommand(
                cmd = arrayOf(settings.pathToNode, "$utbotDirPath/${tempFileName}Base.js"),
                dir = context.projectPath,
                shouldWait = true,
                timeout = settings.timeout,
            )
        }
        return JSONObject(File("$utbotDirPath/${tempFileName}Base.json").readText())
            .getJSONObject("s").let {
                it.keySet().flatMap { key ->
                    val count = it.getInt(key)
                    Collections.nCopies(count, key.toInt())
                }
            }
    }

    override fun getCoveredLines(): List<Set<Int>> {
        try {
            return coverageList.sortedBy { (index, _) -> index }
                .map { (_, obj) ->
                    val dirtyCoverage = obj
                        .let {
                            it.keySet().flatMap {key ->
                                val count = it.getInt(key)
                                Collections.nCopies(count, key.toInt())
                            }.toMutableList()
                        }
                    baseCoverage.forEach {
                        dirtyCoverage.remove(it)
                    }
                    dirtyCoverage.toSet()
                }
        } catch (e: JSONException) {
            throw Exception("Could not get coverage of test cases!")
        } finally {
            removeTempFiles()
        }
    }

    private fun removeTempFiles() {
        FileUtils.deleteDirectory(File("$utbotDirPath/instr"))
        File("$utbotDirPath/${tempFileName}Base.js").delete()
        File("$utbotDirPath/${tempFileName}Base.json").delete()
        for (index in testCaseIndices) {
            File("$utbotDirPath/$tempFileName$index.json").delete()
        }
        for (index in scriptTexts.indices) {
            File("$utbotDirPath/$tempFileName$index.js").delete()
        }
    }


    private fun generateCoverageReport() {
        scriptTexts.indices.toList().parallelStream().forEach { parallelIndex ->
            with(context) {
                val (_, error) = JsCmdExec.runCommand(
                    cmd = arrayOf(settings.pathToNode, "$utbotDirPath/$tempFileName$parallelIndex.js"),
                    dir = context.projectPath,
                    shouldWait = true,
                    timeout = settings.timeout,
                )
                for (i in parallelIndex * 1000..minOf(parallelIndex * 1000 + 999, testCaseIndices.last)) {
                    val resFile = File("$utbotDirPath/$tempFileName$i.json")
                    val rawResult = resFile.readText()
                    resFile.delete()
                    val json = JSONObject(rawResult)
                    val index = json.getInt("index")
                    if (index != i) println("ERROR: index $index != i $i")
                    coverageList.add(index to json.getJSONObject("s"))
                    _resultList.add(index to json.get("result").toString())
                }
                val errText = error.readText()
                if (errText.isNotEmpty()) {
                    println(errText)
                }
            }
        }

    }

    private fun createTempScript(path: String, scriptText: String) {
        val file = File(path)
        file.writeText(scriptText)
        file.createNewFile()
    }
}