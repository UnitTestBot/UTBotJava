package service

import java.io.File
import mu.KotlinLogging
import org.json.JSONObject
import settings.JsTestGenerationSettings.fuzzingThreshold
import settings.JsTestGenerationSettings.tempFileName
import utils.JsCmdExec
import utils.ResultData

private val logger = KotlinLogging.logger {}

class FastCoverageService(
    context: ServiceContext,
    scriptTexts: List<String>,
    private val testCaseIndices: IntRange,
    baseCoverage: List<Int>,
) : CoverageService(context, scriptTexts, baseCoverage) {

    override fun generateCoverageReport() {
            val (_, error) = JsCmdExec.runCommand(
                cmd = arrayOf("\"${settings.pathToNode}\"", "\"$utbotDirPath/$tempFileName" + "0.js\""),
                dir = projectPath,
                shouldWait = true,
                timeout = settings.timeout,
            )
            for (i in 0..minOf(fuzzingThreshold - 1, testCaseIndices.last)) {
                val resFile = File("$utbotDirPath/$tempFileName$i.json")
                val rawResult = resFile.readText()
                resFile.delete()
                val json = JSONObject(rawResult)
                val index = json.getInt("index")
                if (index != i) logger.error { "Index $index != i $i" }
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
                logger.error { errText }
            }
    }
}
