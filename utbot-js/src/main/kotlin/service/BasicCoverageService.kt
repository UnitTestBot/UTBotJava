package service

import mu.KotlinLogging
import org.json.JSONObject
import org.utbot.framework.plugin.api.TimeoutException
import settings.JsTestGenerationSettings.tempFileName
import utils.JsCmdExec
import utils.ResultData
import java.io.File

private val logger = KotlinLogging.logger {}

class BasicCoverageService(
    context: ServiceContext,
    private val scriptTexts: List<String>,
    baseCoverage: List<Int>,
) : CoverageService(context, scriptTexts, baseCoverage) {

    override fun generateCoverageReport() {
        scriptTexts.indices.forEach { index ->
            try {
                val (_, errorText) = JsCmdExec.runCommand(
                    cmd = arrayOf("\"${settings.pathToNode}\"", "\"$utbotDirPath/$tempFileName$index.js\""),
                    dir = projectPath,
                    shouldWait = true,
                    timeout = settings.timeout,
                )
                val resFile = File("$utbotDirPath/$tempFileName$index.json")
                val rawResult = resFile.readText()
                resFile.delete()
                val json = JSONObject(rawResult)
                coverageList.add(index to json.getJSONObject("s"))
                val resultData = ResultData(
                    rawString = if (json.has("result")) json.get("result").toString() else "undefined",
                    type = json.get("type").toString(),
                    index = index,
                    isNan = json.getBoolean("is_nan"),
                    isInf = json.getBoolean("is_inf"),
                    isError = json.getBoolean("is_error"),
                    specSign = json.getInt("spec_sign").toByte()
                )
                _resultList.add(resultData)
                if (errorText.isNotEmpty()) {
                    logger.error { errorText }
                }
            } catch (e: TimeoutException) {
                val resultData = ResultData(
                    rawString = "Timeout",
                    index = index,
                    isError = true,
                )
                coverageList.add(index to JSONObject())
                _resultList.add(resultData)
            }
        }
    }
}
