package org.utbot.go.gocodeanalyzer

import com.beust.klaxon.KlaxonException
import org.utbot.common.FileUtil.extractDirectoryFromArchive
import org.utbot.common.scanForResourcesContaining
import org.utbot.go.api.GoPrimitiveTypeId
import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFunctionParameter
import org.utbot.go.api.util.goSupportedConstantTypes
import org.utbot.go.api.util.rawValueOfGoPrimitiveTypeToValue
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.util.executeCommandByNewProcessOrFail
import org.utbot.go.util.parseFromJsonOrFail
import org.utbot.go.util.writeJsonToFileOrFail
import java.io.File
import java.nio.file.Paths

object GoSourceCodeAnalyzer {

    data class GoSourceFileAnalysisResult(
        val functions: List<GoUtFunction>,
        val notSupportedFunctionsNames: List<String>,
        val notFoundFunctionsNames: List<String>
    )

    data class GoSourceCodeAnalyzerResult(
        val analysisResults: Map<GoUtFile, GoSourceFileAnalysisResult>,
        val intSize: Int,
        val maxTraceLength: Int,
    )

    /**
     * Takes map from absolute paths of Go source files to names of their selected functions.
     *
     * Returns GoSourceFileAnalysisResult-s grouped by their source files.
     */
    fun analyzeGoSourceFilesForFunctions(
        targetFunctionsNamesBySourceFiles: Map<String, List<String>>,
        goExecutableAbsolutePath: String
    ): GoSourceCodeAnalyzerResult {
        val analysisTargets = AnalysisTargets(
            targetFunctionsNamesBySourceFiles.map { (absoluteFilePath, targetFunctionsNames) ->
                AnalysisTarget(absoluteFilePath, targetFunctionsNames)
            }
        )
        val analysisTargetsFileName = createAnalysisTargetsFileName()
        val analysisResultsFileName = createAnalysisResultsFileName()

        val goCodeAnalyzerSourceDir = extractGoCodeAnalyzerSourceDirectory()
        val analysisTargetsFile = goCodeAnalyzerSourceDir.resolve(analysisTargetsFileName)
        val analysisResultsFile = goCodeAnalyzerSourceDir.resolve(analysisResultsFileName)

        val goCodeAnalyzerRunCommand = listOf(
            goExecutableAbsolutePath,
            "run"
        ) + getGoCodeAnalyzerSourceFilesNames() + listOf(
            "-targets",
            analysisTargetsFileName,
            "-results",
            analysisResultsFileName,
        )

        try {
            writeJsonToFileOrFail(analysisTargets, analysisTargetsFile)
            val environment = System.getenv().toMutableMap().apply {
                this["Path"] = (this["Path"] ?: "") + File.pathSeparator + Paths.get(goExecutableAbsolutePath).parent
            }
            executeCommandByNewProcessOrFail(
                goCodeAnalyzerRunCommand,
                goCodeAnalyzerSourceDir,
                "GoSourceCodeAnalyzer for $analysisTargets",
                environment
            )
            val analysisResults = parseFromJsonOrFail<AnalysisResults>(analysisResultsFile)
            val intSize = analysisResults.intSize
            val maxTraceLength = analysisResults.maxTraceLength
            return GoSourceCodeAnalyzerResult(analysisResults.results.map { analysisResult ->
                GoUtFile(analysisResult.absoluteFilePath, analysisResult.sourcePackage) to analysisResult
            }.associateBy({ (sourceFile, _) -> sourceFile }) { (sourceFile, analysisResult) ->
                val functions = analysisResult.analyzedFunctions.map { analyzedFunction ->
                    val parameters = analyzedFunction.parameters.map { analyzedFunctionParameter ->
                        GoUtFunctionParameter(
                            analyzedFunctionParameter.name,
                            analyzedFunctionParameter.type.toGoTypeId()
                        )
                    }
                    val resultTypes = analyzedFunction.resultTypes.map { analyzedType -> analyzedType.toGoTypeId() }
                    val constants = mutableMapOf<GoTypeId, List<Any>>()
                    analyzedFunction.constants.map { (type, rawValues) ->
                        val typeId = GoPrimitiveTypeId(type)
                        if (typeId !in goSupportedConstantTypes) {
                            error("Constants extraction: $type is a unsupported constant type")
                        }
                        val values = rawValues.map { rawValue ->
                            rawValueOfGoPrimitiveTypeToValue(typeId, rawValue, intSize)
                        }
                        constants.compute(typeId) { _, v -> if (v == null) values else v + values }
                    }
                    GoUtFunction(
                        analyzedFunction.name,
                        analyzedFunction.modifiedName,
                        parameters,
                        resultTypes,
                        analyzedFunction.requiredImports,
                        constants,
                        analyzedFunction.modifiedFunctionForCollectingTraces,
                        analyzedFunction.numberOfAllStatements,
                        sourceFile
                    )
                }
                GoSourceFileAnalysisResult(
                    functions,
                    analysisResult.notSupportedFunctionsNames,
                    analysisResult.notFoundFunctionsNames
                )
            }, intSize, maxTraceLength)
        } catch (exception: KlaxonException) {
            throw GoParsingSourceCodeAnalysisResultException(
                "An error occurred while parsing the result of the source code analysis.",
                exception
            )
        } finally {
            // TODO correctly?
            analysisTargetsFile.delete()
            analysisResultsFile.delete()
            goCodeAnalyzerSourceDir.deleteRecursively()
        }
    }

    private fun extractGoCodeAnalyzerSourceDirectory(): File {
        val sourceDirectoryName = "go_source_code_analyzer"
        val classLoader = GoSourceCodeAnalyzer::class.java.classLoader

        val containingResourceFile = classLoader.scanForResourcesContaining(sourceDirectoryName).firstOrNull()
            ?: error("Can't find resource containing $sourceDirectoryName directory.")
        if (containingResourceFile.extension != "jar") {
            error("Resource for $sourceDirectoryName directory is expected to be JAR: others are not supported yet.")
        }

        val archiveFilePath = containingResourceFile.toPath()
        return extractDirectoryFromArchive(archiveFilePath, sourceDirectoryName)?.toFile()
            ?: error("Can't find $sourceDirectoryName directory at the top level of JAR ${archiveFilePath.toAbsolutePath()}.")
    }

    private fun getGoCodeAnalyzerSourceFilesNames(): List<String> {
        return listOf(
            "main.go",
            "analyzer_core.go",
            "analysis_targets.go",
            "analysis_results.go",
            "function_modifier.go",
            "imports_collector.go",
            "constant_extractor.go"
        )
    }

    private fun createAnalysisTargetsFileName(): String {
        return "ut_go_analysis_targets.json"
    }

    private fun createAnalysisResultsFileName(): String {
        return "ut_go_analysis_results.json"
    }
}