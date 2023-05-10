package service.coverage

import java.io.File
import mu.KotlinLogging
import org.json.JSONObject
import org.utbot.framework.plugin.api.TimeoutException
import service.ServiceContext
import settings.JsTestGenerationSettings.tempFileName
import utils.JsCmdExec
import utils.data.ResultData

private val logger = KotlinLogging.logger {}

class BasicCoverageService(
    context: ServiceContext,
    baseCoverage: Map<Int, Int>,
    private val scriptTexts: List<String>,
) : CoverageService(context, baseCoverage, scriptTexts) {

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
                val resultData = ResultData(json)
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
