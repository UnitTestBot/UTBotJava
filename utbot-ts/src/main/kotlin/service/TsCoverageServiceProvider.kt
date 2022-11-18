package service

import framework.api.ts.TsMethodId
import framework.api.ts.TsPrimitiveModel
import java.io.File
import org.json.JSONObject
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.fuzzer.FuzzedValue
import parser.ast.ClassDeclarationNode
import settings.TsTestGenerationSettings
import settings.TsTestGenerationSettings.tempFileName
import utils.TsPathResolver
import kotlin.io.path.pathString

// TODO: Add "error" field in result json to not collide with "result" field upon error.
class TsCoverageServiceProvider(
    private val context: TsServiceContext,
) {

    private val importFileUnderTest = "instr/${context.filePathToInference.substringAfterLast("/")}"

    //TODO: Make function that accepts strings and build import statement instead of monstrosity below.
    private val imports = "const ${TsTestGenerationSettings.fileUnderTestAliases} = require(\"./$importFileUnderTest\")\n" +
            "const fs = require(\"fs\")\n" +
            context.imports.joinToString(separator = "\n", postfix = "\n") { import ->
                with(import) {
                    val relPath = TsPathResolver.getRelativePath(
                        "${context.projectPath}/${context.utbotDir}",
                            path.pathString
                    ).replace("\\", "/")
                    "const $alias = require(\"$relPath\")"
                }
            }

    init {
        makeConfigFile(context.projectPath, context.settings.tsNycModulePath)
    }

    private fun makeConfigFile(projectPath: String, tsNycPath: String) {
        val configFile = File("$projectPath/.nycrc.json")
        if (configFile.exists()) return
        val json = JSONObject()
        json.put("extends", tsNycPath)
        json.put("all", true)
        configFile.writeText(json.toString())
        configFile.createNewFile()
    }
    fun get(
        mode: TsCoverageMode,
        fuzzedValues: List<List<FuzzedValue>>,
        execId: TsMethodId,
        classNode: ClassDeclarationNode?
    ): Pair<List<Set<Int>>, List<String>> {
        return when (mode) {
            TsCoverageMode.FAST -> runFastCoverageAnalysis(
                context,
                fuzzedValues,
                execId,
                classNode
            )

            TsCoverageMode.BASIC -> runBasicCoverageAnalysis(
                context,
                fuzzedValues,
                execId,
                classNode
            )
        }
    }

    private fun runBasicCoverageAnalysis(
        context: TsServiceContext,
        fuzzedValues: List<List<FuzzedValue>>,
        execId: TsMethodId,
        classNode: ClassDeclarationNode?,
    ): Pair<List<Set<Int>>, List<String>> {
        with(context) {
            val tempScriptTexts = fuzzedValues.indices.map {
                "const ${TsTestGenerationSettings.fileUnderTestAliases} = " +
                        "require(\"./${
                            TsPathResolver.getRelativePath(
                                "${projectPath}/${utbotDir}",
                                filePathToInference
                            )
                        }\")\n" + "const fs = require(\"fs\")\n\n" + makeStringForRunJs(
                    fuzzedValue = fuzzedValues[it],
                    method = execId,
                    containingClass = classNode?.name,
                    index = it,
                    resFilePath = "${projectPath}/${utbotDir}/$tempFileName",
                    mode = TsCoverageMode.BASIC
                )
            }
            val coverageService = TsBasicCoverageService(
                context = context,
                scriptTexts = tempScriptTexts,
                testCaseIndices = fuzzedValues.indices,
            )
            return coverageService.getCoveredLines() to coverageService.resultList
        }
    }

    private fun runFastCoverageAnalysis(
        context: TsServiceContext,
        fuzzedValues: List<List<FuzzedValue>>,
        execId: TsMethodId,
        classNode: ClassDeclarationNode?,
    ): Pair<List<Set<Int>>, List<String>> {
        val covFunName = TsFastCoverageService.instrument(context)
        val tempScriptTexts = fuzzedValues.indices.map {
            makeStringForRunJs(
                fuzzedValue = fuzzedValues[it],
                method = execId,
                containingClass = classNode?.name,
                covFunName = covFunName,
                index = it,
                resFilePath = "${context.projectPath}/${context.utbotDir}/$tempFileName",
                mode = TsCoverageMode.FAST
            )
        }
        val baseCoverageScriptText = makeScriptForBaseCoverage(covFunName,"${context.projectPath}/${context.utbotDir}/${tempFileName}Base.json")
        val coverageService = TsFastCoverageService(
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
// @ts-ignore
json.s = ${TsTestGenerationSettings.fileUnderTestAliases}.$covFunName().s
fs.writeFileSync("$resFilePath", JSON.stringify(json))
        """
    }

    //TODO: refactor this.
    private fun makeStringForRunJs(
        fuzzedValue: List<FuzzedValue>,
        method: TsMethodId,
        containingClass: String?,
        covFunName: String = "",
        index: Int,
        resFilePath: String,
        mode: TsCoverageMode,
    ): String {
        val callString = makeCallFunctionString(fuzzedValue, method, containingClass)
        return """
let json$index = {}

let res$index
try {
    res$index = $callString
} catch(e) {
    // @ts-ignore
    res$index = "Error:" + e.message
}
${
"// @ts-ignore\n" +
"json$index.result = res$index\n" +
"// @ts-ignore\n" +
if (mode == TsCoverageMode.FAST ) "json$index.index = $index\n" +
"// @ts-ignore\n" + 
"json$index.s = ${TsTestGenerationSettings.fileUnderTestAliases}.$covFunName().s\n" else ""
}            
fs.writeFileSync("$resFilePath$index.json", JSON.stringify(json$index))
            """
    }

    private fun makeCallFunctionString(
        fuzzedValue: List<FuzzedValue>,
        method: TsMethodId,
        containingClass: String?
    ): String {
        val initClass = containingClass?.let {
            if (!method.isStatic) {
                "new ${TsTestGenerationSettings.fileUnderTestAliases}.${it}()."
            } else "${TsTestGenerationSettings.fileUnderTestAliases}.$it."
        } ?: "${TsTestGenerationSettings.fileUnderTestAliases}."
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
            val classSource = context.imports.find { it.alias == classId.name }?.alias
                ?: TsTestGenerationSettings.fileUnderTestAliases
            val callConstructorString = "new $classSource.${classId.name}"
            val paramsString = instantiationCall.params.joinToString(
                prefix = "(",
                postfix = ")",
            ) {
                (it as TsPrimitiveModel).value.quoteWrapIfNecessary()
            }
            return callConstructorString + paramsString
        }

    private fun UtModel.toCallString(): String =
        when (this) {
            is UtAssembleModel -> this.toParamString()
            else -> {
                (this as TsPrimitiveModel).value.quoteWrapIfNecessary()
            }
        }
}