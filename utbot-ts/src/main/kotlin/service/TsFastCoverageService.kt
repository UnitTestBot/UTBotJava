package service

import java.io.File
import java.util.Collections
import org.json.JSONException
import org.json.JSONObject
import settings.TsTestGenerationSettings.tempFileName
import utils.TsCmdExec

class TsFastCoverageService(
    private val context: TsServiceContext,
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

    companion object {

        fun instrument(context: TsServiceContext): String = with (context) {
            val destination = "${projectPath}/${utbotDir}/instr"
            val fileName = filePathToInference.substringAfterLast("/")
                TsCmdExec.runCommand(
                    cmd = arrayOf(
                        settings.pathToNYC,
                        "instrument",
                        fileName,
                        destination,
                    ),
                    dir = filePathToInference.substringBeforeLast("/"),
                    shouldWait = true,
                    timeout = settings.timeout,
                )

            val instrumentedFilePath = "$destination/${filePathToInference.substringAfterLast("/")}"
            val instrumentedFileText = File(instrumentedFilePath).readText()
            fixImportsInInstrumentedFile(instrumentedFilePath, instrumentedFileText)
            val covFunRegex = Regex("function (cov_.*)\\(\\).*")
            val covFunName = covFunRegex.find(instrumentedFileText.takeWhile { it != '{' })?.groups?.get(1)?.value
                ?: throw IllegalStateException("No coverage function was found in instrumented source file!")
            val fixedFileText = "$instrumentedFileText\nexports.$covFunName = $covFunName"
            File(instrumentedFilePath).writeText(fixedFileText)
            return covFunName
        }

        private fun fixImportsInInstrumentedFile(filePath: String, fileText: String) {
            val importRegex = Regex("import\\{.*}from\"(.*)\"")
            val imports = importRegex.findAll(fileText).iterator().asSequence().map {
               it.groups[1]?.value
            }.toList()
            // TODO: continue
        }
    }

    init {
        generateTempFiles()
        baseCoverage = getBaseCoverage()
        generateCoverageReport()
    }

    private fun generateTempFiles() {
        scriptTexts.forEachIndexed { index, scriptText ->
            val tempScriptPath = "$utbotDirPath/$tempFileName$index.ts"
            createTempScript(
                path = tempScriptPath,
                scriptText = scriptText
            )
        }
        createTempScript(
            path = "$utbotDirPath/${tempFileName}Base.ts",
            scriptText = baseCoverageScriptText
        )
    }

    private fun getBaseCoverage(): List<Int> {
        with(context) {
            TsCmdExec.runCommand(
                cmd = arrayOf(settings.tsNodePath, "$utbotDirPath/${tempFileName}Base.ts"),
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
//        FileUtils.deleteDirectory(File("$utbotDirPath/instr"))
//        File("$utbotDirPath/${tempFileName}Base.ts").delete()
//        File("$utbotDirPath/${tempFileName}Base.json").delete()
//        for (index in testCaseIndices) {
//            File("$utbotDirPath/$tempFileName$index.json").delete()
//        }
//        for (index in scriptTexts.indices) {
//            File("$utbotDirPath/$tempFileName$index.ts").delete()
//        }
    }


    private fun generateCoverageReport() {
        scriptTexts.indices.toList().parallelStream().forEach { parallelIndex ->
            with(context) {
                val (_, error) = TsCmdExec.runCommand(
                    cmd = arrayOf(settings.tsNodePath, "$utbotDirPath/$tempFileName$parallelIndex.ts"),
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