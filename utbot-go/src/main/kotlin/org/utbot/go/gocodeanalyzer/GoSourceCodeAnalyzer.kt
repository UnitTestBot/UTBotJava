package org.utbot.go.gocodeanalyzer

import org.utbot.common.FileUtil.extractDirectoryFromArchive
import org.utbot.common.scanForResourcesContaining
import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFunctionParameter
import org.utbot.go.fuzzer.providers.GoPrimitivesValueProvider
import org.utbot.go.util.executeCommandByNewProcessOrFail
import org.utbot.go.util.parseFromJsonOrFail
import org.utbot.go.util.writeJsonToFileOrFail
import java.io.File

object GoSourceCodeAnalyzer {

    data class GoSourceFileAnalysisResult(
        val functions: List<GoUtFunction>,
        val notSupportedFunctionsNames: List<String>,
        val notFoundFunctionsNames: List<String>
    )

    /**
     * Takes map from absolute paths of Go source files to names of their selected functions.
     *
     * Returns GoSourceFileAnalysisResult-s grouped by their source files.
     */
    fun analyzeGoSourceFilesForFunctions(
        targetFunctionsNamesBySourceFiles: Map<String, List<String>>,
        goExecutableAbsolutePath: String
    ): Map<GoUtFile, GoSourceFileAnalysisResult> {
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
            executeCommandByNewProcessOrFail(
                goCodeAnalyzerRunCommand,
                goCodeAnalyzerSourceDir,
                "GoSourceCodeAnalyzer for $analysisTargets"
            )
            val analysisResults = parseFromJsonOrFail<AnalysisResults>(analysisResultsFile)
            GoPrimitivesValueProvider.intSize = analysisResults.intSize
            return analysisResults.results.map { analysisResult ->
                GoUtFile(analysisResult.absoluteFilePath, analysisResult.packageName) to analysisResult
            }.associateBy({ (sourceFile, _) -> sourceFile }) { (sourceFile, analysisResult) ->
                val functions = analysisResult.analyzedFunctions.map { analyzedFunction ->
                    val parameters = analyzedFunction.parameters.map { analyzedFunctionParameter ->
                        GoUtFunctionParameter(
                            analyzedFunctionParameter.name,
                            analyzedFunctionParameter.type.toGoTypeId()
                        )
                    }
                    val resultTypes = analyzedFunction.resultTypes.map { analyzedType -> analyzedType.toGoTypeId() }
                    GoUtFunction(
                        analyzedFunction.name,
                        analyzedFunction.modifiedName,
                        parameters,
                        resultTypes,
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
            }
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
        return listOf("main.go", "analyzer_core.go", "analysis_targets.go", "analysis_results.go", "cover.go")
    }

    private fun createAnalysisTargetsFileName(): String {
        return "ut_go_analysis_targets.json"
    }

    private fun createAnalysisResultsFileName(): String {
        return "ut_go_analysis_results.json"
    }
}