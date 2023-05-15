package org.utbot.go.logic

import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFuzzedFunctionTestCase
import org.utbot.go.gocodeanalyzer.GoSourceCodeAnalyzer
import org.utbot.go.gocodeinstrumentation.GoPackageInstrumentation
import org.utbot.go.gocodeinstrumentation.InstrumentationResult
import org.utbot.go.simplecodegeneration.GoTestCasesCodeGenerator
import java.nio.file.Path

abstract class AbstractGoUtTestsGenerationController {

    fun generateTests(
        selectedFunctionNamesBySourceFiles: Map<Path, List<String>>,
        selectedMethodNamesBySourceFiles: Map<Path, List<String>>,
        testsGenerationConfig: GoUtTestsGenerationConfig,
        isCanceled: () -> Boolean = { false }
    ) {
        if (!onSourceCodeAnalysisStart(selectedFunctionNamesBySourceFiles, selectedMethodNamesBySourceFiles)) return
        val (analysisResults, intSize) = GoSourceCodeAnalyzer.analyzeGoSourceFilesForFunctions(
            selectedFunctionNamesBySourceFiles,
            selectedMethodNamesBySourceFiles,
            testsGenerationConfig.goExecutableAbsolutePath,
            testsGenerationConfig.gopathAbsolutePath
        )
        if (!onSourceCodeAnalysisFinished(analysisResults)) return

        if (!onPackageInstrumentationStart()) return
        val instrumentationResults = mutableMapOf<String, InstrumentationResult>()
        analysisResults.forEach { (file, analysisResult) ->
            val absoluteDirectoryPath = file.absoluteDirectoryPath
            if (instrumentationResults[absoluteDirectoryPath] == null) {
                instrumentationResults[absoluteDirectoryPath] = GoPackageInstrumentation.instrumentGoPackage(
                    testedFunctions = analysisResult.functions.map { it.name },
                    absoluteDirectoryPath = absoluteDirectoryPath,
                    goExecutableAbsolutePath = testsGenerationConfig.goExecutableAbsolutePath,
                    gopathAbsolutePath = testsGenerationConfig.gopathAbsolutePath
                )
            }
        }
        if (!onPackageInstrumentationFinished()) return

        val numOfFunctions = analysisResults.values
            .map { it.functions.size }
            .reduce { acc, numOfFunctions -> acc + numOfFunctions }
        val functionTimeoutStepMillis = testsGenerationConfig.allFunctionExecutionTimeoutMillis / numOfFunctions
        var startTimeMillis = System.currentTimeMillis()
        val testCasesBySourceFiles = analysisResults.mapValues { (sourceFile, analysisResult) ->
            val functions = analysisResult.functions
            if (!onTestCasesGenerationForGoSourceFileFunctionsStart(sourceFile, functions)) return
            val (absolutePathToInstrumentedPackage, absolutePathToInstrumentedModule, needToCoverLines) =
                instrumentationResults[sourceFile.absoluteDirectoryPath]!!
            GoTestCasesGenerator.generateTestCasesForGoSourceFileFunctions(
                sourceFile,
                functions,
                absolutePathToInstrumentedPackage,
                absolutePathToInstrumentedModule,
                needToCoverLines,
                intSize,
                testsGenerationConfig
            ) { index -> isCanceled() || System.currentTimeMillis() - (startTimeMillis + (index + 1) * functionTimeoutStepMillis) > 0 }
                .also {
                    startTimeMillis += functionTimeoutStepMillis * functions.size
                    if (!onTestCasesGenerationForGoSourceFileFunctionsFinished(sourceFile, it)) return
                }
        }

        testCasesBySourceFiles.forEach { (sourceFile, testCases) ->
            if (!onTestCasesFileCodeGenerationStart(sourceFile, testCases)) return
            val generatedTestsFileCode = GoTestCasesCodeGenerator.generateTestCasesFileCode(sourceFile, testCases)
            if (!onTestCasesFileCodeGenerationFinished(sourceFile, generatedTestsFileCode)) return
        }
    }

    protected abstract fun onSourceCodeAnalysisStart(
        targetFunctionNamesBySourceFiles: Map<Path, List<String>>,
        targetMethodNamesBySourceFiles: Map<Path, List<String>>,
    ): Boolean

    protected abstract fun onSourceCodeAnalysisFinished(
        analysisResults: Map<GoUtFile, GoSourceCodeAnalyzer.GoSourceFileAnalysisResult>
    ): Boolean

    protected abstract fun onPackageInstrumentationStart(): Boolean

    protected abstract fun onPackageInstrumentationFinished(): Boolean

    protected abstract fun onTestCasesGenerationForGoSourceFileFunctionsStart(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>
    ): Boolean

    protected abstract fun onTestCasesGenerationForGoSourceFileFunctionsFinished(
        sourceFile: GoUtFile,
        testCases: List<GoUtFuzzedFunctionTestCase>
    ): Boolean

    protected abstract fun onTestCasesFileCodeGenerationStart(
        sourceFile: GoUtFile,
        testCases: List<GoUtFuzzedFunctionTestCase>
    ): Boolean

    protected abstract fun onTestCasesFileCodeGenerationFinished(
        sourceFile: GoUtFile,
        generatedTestsFileCode: String
    ): Boolean

    protected fun generateMissingSelectedFunctionsListMessage(
        analysisResults: Map<GoUtFile, GoSourceCodeAnalyzer.GoSourceFileAnalysisResult>,
    ): String? {
        val missingSelectedFunctions = analysisResults.filter { (_, analysisResult) ->
            analysisResult.notSupportedFunctionAndMethodNames.isNotEmpty() || analysisResult.notFoundFunctionAndMethodNames.isNotEmpty()
        }
        if (missingSelectedFunctions.isEmpty()) {
            return null
        }
        return missingSelectedFunctions.map { (sourceFile, analysisResult) ->
            val notSupportedFunctions = analysisResult.notSupportedFunctionAndMethodNames.joinToString(separator = ", ")
            val notFoundFunctions = analysisResult.notFoundFunctionAndMethodNames.joinToString(separator = ", ")
            val messageSb = StringBuilder()
            messageSb.append("File ${sourceFile.absolutePath}")
            if (notSupportedFunctions.isNotEmpty()) {
                messageSb.append("\n-- contains currently unsupported functions: $notSupportedFunctions")
            }
            if (notFoundFunctions.isNotEmpty()) {
                messageSb.append("\n-- does not contain functions: $notFoundFunctions")
            }
            messageSb.toString()
        }.joinToString(separator = "\n\n", prefix = "\n\n", postfix = "\n\n")
    }
}