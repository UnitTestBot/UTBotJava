package service.coverage

import java.io.File
import mu.KotlinLogging
import org.json.JSONObject
import service.ServiceContext
import settings.JsTestGenerationSettings.fuzzingThreshold
import settings.JsTestGenerationSettings.tempFileName
import utils.JsCmdExec
import utils.data.ResultData

private val logger = KotlinLogging.logger {}

class FastCoverageService(
    context: ServiceContext,
    baseCoverage: Map<Int, Int>,
    scriptTexts: List<String>,
    private val testCaseIndices: IntRange,
) : CoverageService(context, baseCoverage, scriptTexts) {

    override fun generateCoverageReport() {
        val (_, errorText) = JsCmdExec.runCommand(
            cmd = arrayOf("\"${settings.pathToNode}\"", "\"$utbotDirPath/$tempFileName" + "0.js\""),
            dir = projectPath,
            shouldWait = true,
            timeout = settings.timeout,
        )
        for (i in 0..minOf(fuzzingThreshold - 1, testCaseIndices.last)) {
            val resFile = File("$utbotDirPath/$tempFileName$i.json")
            val rawResult = resFile.readText()
//            resFile.delete()
            val json = JSONObject(rawResult)
            val index = json.getInt("index")
            if (index != i) logger.error { "Index $index != i $i" }
            coverageList.add(index to json.getJSONObject("s"))
            val resultData = ResultData(json)
            _resultList.add(resultData)
        }
        if (errorText.isNotEmpty()) {
            logger.error { errorText }
        }
    }
}
