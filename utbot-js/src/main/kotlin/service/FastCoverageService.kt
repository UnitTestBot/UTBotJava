package service

import java.io.File
import org.json.JSONObject
import settings.JsTestGenerationSettings.fuzzingThreshold
import settings.JsTestGenerationSettings.tempFileName
import utils.JsCmdExec
import utils.ResultData

class FastCoverageService(
    context: ServiceContext,
    private val scriptTexts: List<String>,
    private val testCaseIndices: IntRange,
    baseCoverage: List<Int>,
) : CoverageService(context, scriptTexts, baseCoverage) {

    override fun generateCoverageReport() {
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
                resFile.delete()
                val json = JSONObject(rawResult)
                val index = json.getInt("index")
                if (index != i) println("ERROR: index $index != i $i")
                coverageList.add(index to json.getJSONObject("s"))
                val resultData = ResultData(
                    rawString = json.get("result").toString(),
                    type = json.get("type").toString(),
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