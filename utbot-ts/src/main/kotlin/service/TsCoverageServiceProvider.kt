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
import parser.ast.PropertyDeclarationNode
import settings.TsTestGenerationSettings.fileUnderTestAliases
import settings.TsTestGenerationSettings.tempFileName
import utils.TsPathResolver
import kotlin.io.path.pathString

// TODO: Add "error" field in result json to not collide with "result" field upon error.
class TsCoverageServiceProvider(
    private val context: TsServiceContext,
) {

    init {
        makeConfigFile(context.projectPath, context.settings.tsNycModulePath)
    }

    private val importsSection = buildImportSection()

    private fun buildImportSection(): String {
        val fileUnderTestImport = buildAllImport(
            fileUnderTestAliases,
            "./instr/${context.filePathToInference.substringAfterLast("/")}"
        )
        val fsImport = buildAllImport("fs", "fs")
        val otherImports = context.imports.map { import ->
            val relPath = TsPathResolver.getRelativePath(
                "${context.projectPath}/${context.utbotDir}",
                import.path.pathString
            ).replace("\\", "/")
            buildImportStatement(import.alias, relPath)
        }
        return buildString {
            if (context.settings.workMode == TsWorkMode.EXPERIMENTAL) {
                val relPathToGodObject = TsPathResolver.getRelativePath(
                    "${context.projectPath}/${context.utbotDir}",
                    context.settings.godObject!!.substringBeforeLast(".")
                ).replace("\\", "/")
                append(
                    buildImportStatement(
                        context.settings.godObject.substringAfterLast("."),
                        relPathToGodObject
                    )
                )
                val relPathToDumpFunction = TsPathResolver.getRelativePath(
                    "${context.projectPath}/${context.utbotDir}",
                    context.settings.dumpFunction!!.substringBeforeLast(".")
                ).replace("\\", "/")
                append(
                    buildImportStatement(
                        context.settings.dumpFunction.substringAfterLast("."),
                        relPathToDumpFunction
                    )
                )
            }
            append(fileUnderTestImport)
            append("// @ts-ignore\n")
            append(fsImport)
            otherImports.forEach {
                append(it)
            }
            appendLine()
        }
    }

    private fun buildImportStatement(alias: String, path: String): String =
        "import { $alias } from \"${path.removeSuffix(".ts")}\";\n"

    private fun buildAllImport(alias: String, path: String): String =
        "import * as $alias from \"${path.removeSuffix(".ts")}\";\n"

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
        coverageMode: TsCoverageMode,
        fuzzedValues: List<List<FuzzedValue>>,
        execId: TsMethodId,
        classNode: ClassDeclarationNode?,
        statics: Set<PropertyDeclarationNode> = emptySet()
    ): Pair<List<Set<Int>>, List<String>> {
        return when (coverageMode) {
            TsCoverageMode.FAST -> runFastCoverageAnalysis(
                context,
                fuzzedValues,
                execId,
                classNode,
                statics
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
                val importSection = buildString {
                    append(buildImportStatement(
                        fileUnderTestAliases,
                        "./${TsPathResolver.getRelativePath(
                            "${projectPath}/${utbotDir}",
                                filePathToInference
                            )}"
                    ))
                    appendLine(buildImportStatement("fs", "fs"))
                }
                importSection + makeStringForRunTs(
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
        statics: Set<PropertyDeclarationNode>
    ): Pair<List<Set<Int>>, List<String>> {
        val covFunName = TsFastCoverageService.instrument(context)
        val tempScriptTexts = fuzzedValues.indices.map {
            when (context.settings.workMode) {
                TsWorkMode.PLANE -> makeStringForRunTs(
                    fuzzedValue = fuzzedValues[it],
                    method = execId,
                    containingClass = classNode?.name,
                    covFunName = covFunName,
                    index = it,
                    resFilePath = "${context.projectPath}/${context.utbotDir}/$tempFileName",
                    mode = TsCoverageMode.FAST
                )
                else -> makeStringForRunExperimentalTs(
                    fuzzedValue = fuzzedValues[it],
                    method = execId,
                    containingClass = classNode?.name,
                    covFunName = covFunName,
                    index = it,
                    resFilePath = "${context.projectPath}/${context.utbotDir}/$tempFileName",
                    mode = TsCoverageMode.FAST,
                    godObjectName = context.settings.godObject!!.substringAfterLast("."),
                    dumpFunctionName = context.settings.dumpFunction!!.substringAfterLast("."),
                    statics = statics
                )
            }
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
                    importsSection + tempScripts.joinToString("\n\n")
                )
            }
            else -> {
                return tempScripts
                    .withIndex()
                    .groupBy { it.index / 1000 }
                    .map { entry ->
                        importsSection + entry.value.joinToString("\n\n") { it.value }
                    }
            }
        }
    }

    private fun makeScriptForBaseCoverage(covFunName: String, resFilePath: String): String =
        """
$importsSection

let json = {}
// @ts-ignore
json.s = $fileUnderTestAliases.$covFunName().s
fs.writeFileSync("$resFilePath", JSON.stringify(json))
        """

    private fun makeStringForRunExperimentalTs(
        fuzzedValue: List<FuzzedValue>,
        method: TsMethodId,
        containingClass: String?,
        covFunName: String = "",
        index: Int,
        resFilePath: String,
        mode: TsCoverageMode,
        godObjectName: String,
        dumpFunctionName: String,
        statics: Set<PropertyDeclarationNode>
    ): String =
        """
let json$index = {}

let res$index
try {
    ${buildStaticsSection(fuzzedValue, statics, godObjectName)}
    res$index = $dumpFunctionName(${makeCallFunctionString(emptyList(), method, containingClass)})
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
"json$index.s = $fileUnderTestAliases.$covFunName().s\n" else ""
}            
fs.writeFileSync("$resFilePath$index.json", JSON.stringify(json$index))
        """

    private fun buildStaticsSection(fuzzedValue: List<FuzzedValue>, statics: Set<PropertyDeclarationNode>, name: String): String {
        return buildString {
            statics.forEachIndexed { index, node ->
                appendLine("$name.${node.name} = ${fuzzedValue[index].model.toCallString()}")
            }
        }
    }

    private fun makeStringForRunTs(
        fuzzedValue: List<FuzzedValue>,
        method: TsMethodId,
        containingClass: String?,
        covFunName: String = "",
        index: Int,
        resFilePath: String,
        mode: TsCoverageMode,
    ): String =
        """
let json$index = {}

let res$index
try {
    res$index = ${makeCallFunctionString(fuzzedValue, method, containingClass)}
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
"json$index.s = $fileUnderTestAliases.$covFunName().s\n" else ""
}            
fs.writeFileSync("$resFilePath$index.json", JSON.stringify(json$index))
        """

    private fun makeCallFunctionString(
        fuzzedValue: List<FuzzedValue>,
        method: TsMethodId,
        containingClass: String?
    ): String {
        val initClass = containingClass?.let {
            if (!method.isStatic) {
                "new $fileUnderTestAliases.${it}()."
            } else "$fileUnderTestAliases.$it."
        } ?: "$fileUnderTestAliases."
        var callString = "$initClass${method.name}"
        if (context.settings.workMode == TsWorkMode.PLANE) callString += fuzzedValue.joinToString(
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
                ?: fileUnderTestAliases
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