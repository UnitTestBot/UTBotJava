package service

import api.NodeCoverageMode
import com.google.javascript.rhino.Node
import framework.api.js.JsMethodId
import framework.api.js.JsPrimitiveModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.fuzzer.FuzzedValue
import parser.JsParserUtils.getClassName
import settings.JsTestGenerationSettings
import settings.JsTestGenerationSettings.tempFileName
import utils.PathResolver

// TODO: Add "error" field in result json to not collide with "result" field upon error.
class CoverageServiceProvider(private val context: ServiceContext) {

    private val importFileUnderTest = "instr/${context.filePathToInference.substringAfterLast("/")}"

    private val imports =
        "const ${JsTestGenerationSettings.fileUnderTestAliases} = require(\"./$importFileUnderTest\")\n" +
                "const fs = require(\"fs\")\n\n"

    fun get(
        mode: NodeCoverageMode,
        fuzzedValues: List<List<FuzzedValue>>,
        execId: JsMethodId,
        classNode: Node?
    ): Pair<List<Set<Int>>, List<String>> {
        return when (mode) {
            NodeCoverageMode.FAST -> runFastCoverageAnalysis(
                context,
                fuzzedValues,
                execId,
                classNode
            )

            NodeCoverageMode.BASIC -> runBasicCoverageAnalysis(
                context,
                fuzzedValues,
                execId,
                classNode
            )
        }
    }

    private fun runBasicCoverageAnalysis(
        context: ServiceContext,
        fuzzedValues: List<List<FuzzedValue>>,
        execId: JsMethodId,
        classNode: Node?,
    ): Pair<List<Set<Int>>, List<String>> {
        val tempScriptTexts = fuzzedValues.indices.map {
            "const ${JsTestGenerationSettings.fileUnderTestAliases} = require(\"./${
                PathResolver.getRelativePath(
                    "${context.projectPath}/${context.utbotDir}",
                    context.filePathToInference
                )
            }\")\n" + "const fs = require(\"fs\")\n\n" + makeStringForRunJs(
                fuzzedValue = fuzzedValues[it],
                method = execId,
                containingClass = classNode?.getClassName(),
                index = it,
                resFilePath = "${context.projectPath}/${context.utbotDir}/$tempFileName",
                mode = CoverageMode.BASIC
            )
        }
        val coverageService = BasicCoverageService(
            context = context,
            scriptTexts = tempScriptTexts,
            testCaseIndices = fuzzedValues.indices,
        )
        return coverageService.getCoveredLines() to coverageService.resultList
    }

    private fun runFastCoverageAnalysis(
        context: ServiceContext,
        fuzzedValues: List<List<FuzzedValue>>,
        execId: JsMethodId,
        classNode: Node?,
    ): Pair<List<Set<Int>>, List<String>> {
        val covFunName = FastCoverageService.instrument(context)
        val tempScriptTexts = fuzzedValues.indices.map {
            makeStringForRunJs(
                fuzzedValue = fuzzedValues[it],
                method = execId,
                containingClass = classNode?.getClassName(),
                covFunName = covFunName,
                index = it,
                resFilePath = "${context.projectPath}/${context.utbotDir}/$tempFileName",
                mode = CoverageMode.FAST
            )
        }
        val baseCoverageScriptText =
            makeScriptForBaseCoverage(covFunName, "${context.projectPath}/${context.utbotDir}/${tempFileName}Base.json")
        val coverageService = FastCoverageService(
            context = context,
            scriptTexts = splitTempScriptsIfNeeded(tempScriptTexts),
            testCaseIndices = fuzzedValues.indices,
            baseCoverageScriptText = baseCoverageScriptText,
        )
        return coverageService.getCoveredLines() to coverageService.resultList
    }

    // TODO: do not hardcode 1000 constant - move to settings object.
    private fun splitTempScriptsIfNeeded(tempScripts: List<String>): List<String> {
        when {
            // No need to run parallel execution, so only 1 element in the list
            tempScripts.size < 1000 -> {
                return listOf(
                    imports + tempScripts.joinToString("\n\n")
                )
            }

            else -> {
                return tempScripts
                    .withIndex()
                    .groupBy { it.index / 1000 }
                    .map { entry ->
                        imports + entry.value.joinToString("\n\n") { it.value }
                    }
            }
        }
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
        covFunName: String = "",
        index: Int,
        resFilePath: String,
        mode: CoverageMode,
    ): String {
        val callString = makeCallFunctionString(fuzzedValue, method, containingClass)
        return """
let json$index = {}

let res$index
try {
    res$index = $callString
} catch(e) {
    res$index = "Error:" + e.message
}
${
            "json$index.result = res$index\n" +
                    if (mode == CoverageMode.FAST) "json$index.index = $index\n" +
                            "json$index.s = ${JsTestGenerationSettings.fileUnderTestAliases}.$covFunName().s\n" else ""
        }            
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
            is String -> "\"$this\""
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
