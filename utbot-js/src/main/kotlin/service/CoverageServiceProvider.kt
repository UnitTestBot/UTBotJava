package service

import framework.api.js.JsMethodId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isUndefined
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.fuzzer.FuzzedValue
import settings.JsTestGenerationSettings
import settings.JsTestGenerationSettings.tempFileName
import utils.CoverageData
import utils.ResultData

// TODO: Add "error" field in result json to not collide with "result" field upon error.
class CoverageServiceProvider(
    private val context: ServiceContext,
    private val instrumentationService: InstrumentationService,
    private val mode: CoverageMode
) : ContextOwner by context {

    private val importFileUnderTest = "instr/${filePathToInference.substringAfterLast("/")}"

    private val imports =
        "const ${JsTestGenerationSettings.fileUnderTestAliases} = require(\"./$importFileUnderTest\")\n" +
                "const fs = require(\"fs\")\n\n"

    private val filePredicate = """
function check_value(value, json) {
    if (value === Infinity) {
        json.is_inf = true
        json.spec_sign = 1
    }
    if (value === -Infinity) {
        json.is_inf = true
        json.spec_sign = -1
    }
    if (isNaN(value)) {
        json.is_nan = true
    }
}
    """

    val baseCoverage: List<Int>

    init {
        val temp = makeScriptForBaseCoverage(
            instrumentationService.covFunName,
            "${projectPath}/${utbotDir}/${tempFileName}Base.json"
        )
        baseCoverage = CoverageService.getBaseCoverage(
            context,
            temp
        )
    }

    fun get(
        fuzzedValues: List<List<FuzzedValue>>,
        execId: JsMethodId,
    ): Pair<List<CoverageData>, List<ResultData>> {
        return when (mode) {
            CoverageMode.FAST -> runFastCoverageAnalysis(
                fuzzedValues,
                execId
            )

            CoverageMode.BASIC -> runBasicCoverageAnalysis(
                fuzzedValues,
                execId
            )
        }
    }

    private fun runBasicCoverageAnalysis(
        fuzzedValues: List<List<FuzzedValue>>,
        execId: JsMethodId,
    ): Pair<List<CoverageData>, List<ResultData>> {
        val covFunName = instrumentationService.covFunName
        val tempScriptTexts = fuzzedValues.indices.map {
            imports + "$filePredicate\n\n" + makeStringForRunJs(
                fuzzedValue = fuzzedValues[it],
                method = execId,
                containingClass = if (!execId.classId.isUndefined) execId.classId.name else null,
                covFunName = covFunName,
                index = it,
                resFilePath = "${projectPath}/${utbotDir}/$tempFileName",
            )
        }
        val coverageService = BasicCoverageService(
            context = context,
            scriptTexts = tempScriptTexts,
            baseCoverage = baseCoverage
        )
        coverageService.generateCoverageReport()
        return coverageService.getCoveredLines() to coverageService.resultList
    }

    private fun runFastCoverageAnalysis(
        fuzzedValues: List<List<FuzzedValue>>,
        execId: JsMethodId,
    ): Pair<List<CoverageData>, List<ResultData>> {
        val covFunName = instrumentationService.covFunName
        val tempScriptTexts = imports + "$filePredicate\n\n" + fuzzedValues.indices.joinToString("\n\n") {
            makeStringForRunJs(
                fuzzedValue = fuzzedValues[it],
                method = execId,
                containingClass = if (!execId.classId.isUndefined) execId.classId.name else null,
                covFunName = covFunName,
                index = it,
                resFilePath = "${projectPath}/${utbotDir}/$tempFileName",
            )
        }
        val coverageService = FastCoverageService(
            context = context,
            scriptTexts = listOf(tempScriptTexts),
            testCaseIndices = fuzzedValues.indices,
            baseCoverage = baseCoverage,
        )
        coverageService.generateCoverageReport()
        return coverageService.getCoveredLines() to coverageService.resultList
    }

    private fun makeScriptForBaseCoverage(covFunName: String, resFilePath: String): String {
        return """
$imports

let json = {}
json.s = ${JsTestGenerationSettings.fileUnderTestAliases}.$covFunName().s
fs.writeFileSync("$resFilePath", JSON.stringify(json))
        """
    }

    private fun makeStringForRunJs(
        fuzzedValue: List<FuzzedValue>,
        method: JsMethodId,
        containingClass: String?,
        covFunName: String,
        index: Int,
        resFilePath: String,
    ): String {
        val callString = makeCallFunctionString(fuzzedValue, method, containingClass)
        return """
let json$index = {}
json$index.is_inf = false
json$index.is_nan = false
json$index.spec_sign = 1
let res$index
try {
    res$index = $callString
    check_value(res$index, json$index)
} catch(e) {
    res$index = "Error:" + e.message
}
json$index.result = res$index
json$index.type = typeof res$index
json$index.index = $index
json$index.s = ${JsTestGenerationSettings.fileUnderTestAliases}.$covFunName().s   
    
fs.writeFileSync("$resFilePath$index.json", JSON.stringify(json$index))
            """
    }

    private fun makeCallFunctionString(
        fuzzedValue: List<FuzzedValue>,
        method: JsMethodId,
        containingClass: String?
    ): String {
        val initClass = containingClass?.let {
            if (!method.isStatic) {
                "new ${JsTestGenerationSettings.fileUnderTestAliases}.${it}()."
            } else "${JsTestGenerationSettings.fileUnderTestAliases}.$it."
        } ?: "${JsTestGenerationSettings.fileUnderTestAliases}."
        var callString = "$initClass${method.name}"
        callString += fuzzedValue.joinToString(
            prefix = "(",
            postfix = ")",
        ) { value -> value.model.toCallString() }
        return callString
    }

    private fun Any.quoteWrapIfNecessary(): String =
        when (this) {
            is String -> "`$this`"
            else -> "$this"
        }

    private fun UtAssembleModel.toParamString(): String =
        with(this) {
            val callConstructorString = "new ${JsTestGenerationSettings.fileUnderTestAliases}.${classId.name}"
            val paramsString = instantiationCall.params.joinToString(
                prefix = "(",
                postfix = ")",
            ) {
                (it as JsPrimitiveModel).value.quoteWrapIfNecessary()
            }
            return callConstructorString + paramsString
        }

    private fun UtModel.toCallString(): String =
        when (this) {
            is UtAssembleModel -> this.toParamString()
            else -> {
                (this as JsPrimitiveModel).value.quoteWrapIfNecessary()
            }
        }
}
