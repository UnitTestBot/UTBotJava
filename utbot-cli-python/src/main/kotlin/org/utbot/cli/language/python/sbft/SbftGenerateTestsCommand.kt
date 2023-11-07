package org.utbot.cli.language.python.sbft

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import mu.KotlinLogging
import org.parsers.python.PythonParser
import org.utbot.cli.language.python.CliRequirementsInstaller
import org.utbot.cli.language.python.findCurrentPythonModule
import org.utbot.cli.language.python.toAbsolutePath
import org.utbot.cli.language.python.writeToFileAndSave
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.python.PythonMethodHeader
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.TestFileInformation
import org.utbot.python.UsvmConfig
import org.utbot.python.code.PythonCode
import org.utbot.python.coverage.CoverageOutputFormat
import org.utbot.python.coverage.PythonCoverageMode
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.PythonImport
import org.utbot.python.newtyping.ast.parseClassDefinition
import org.utbot.python.newtyping.ast.parseFunctionDefinition
import org.utbot.python.newtyping.mypy.dropInitFile
import org.utbot.python.utils.Fail
import org.utbot.python.utils.RequirementsInstaller
import org.utbot.python.utils.Success
import java.io.File

private const val DEFAULT_TIMEOUT_IN_MILLIS = 60000L
private const val DEFAULT_TIMEOUT_FOR_ONE_RUN_IN_MILLIS = 2000L

private val logger = KotlinLogging.logger {}

class SbftGenerateTestsCommand : CliktCommand(
    name = "generate_python",
    help = "Generate tests for specified Python classes or top-level functions from a specified file."
) {
    private val sourceFile by argument(
        help = "File with Python code to generate tests for."
    )

    private lateinit var absPathToSourceFile: String
    private lateinit var sourceFileContent: String

    private val directoriesForSysPath by option(
        "-s", "--sys-path",
        help = "(required) Directories to add to sys.path. " +
                "One of directories must contain the file with the methods under test."
    ).split(",").required()

    private val pythonPath by option(
        "-p", "--python-path",
        help = "(required) Path to Python interpreter."
    ).required()

    private val output by option(
        "-o", "--output", help = "(required) File for generated tests."
    ).required()

    private val doNotMinimize by option(
        "--do-not-minimize",
        help = "Turn off minimization of the number of generated tests."
    ).flag(default = false)

    private val timeout by option(
        "-t", "--timeout",
        help = "Specify the maximum time in milliseconds to spend on generating tests ($DEFAULT_TIMEOUT_IN_MILLIS by default)."
    ).long().default(DEFAULT_TIMEOUT_IN_MILLIS)

    private val timeoutForRun by option(
        "--timeout-for-run",
        help = "Specify the maximum time in milliseconds to spend on one function run ($DEFAULT_TIMEOUT_FOR_ONE_RUN_IN_MILLIS by default)."
    ).long().default(DEFAULT_TIMEOUT_FOR_ONE_RUN_IN_MILLIS)

    private val runtimeExceptionTestsBehaviour by option("--runtime-exception-behaviour", help = "PASS or FAIL")
        .choice("PASS", "FAIL")
        .default("FAIL")

    private val coverageMeasureMode by option("--coverage-measure-mode", help = "Use LINES or INSTRUCTIONS for coverage measurement.")
        .choice("INSTRUCTIONS", "LINES")
        .default("INSTRUCTIONS")

    private val doNotSendCoverageContinuously by option("--do-not-send-coverage-continuously", help = "Do not send coverage during execution.")
        .flag(default = false)

    private val javaCmd by option(
        "--java-cmd",
        help = "(required) Path to Java command (ONLY FOR USVM)."
    ).required()

    private val usvmDirectory by option(
        "--usvm-dir",
        help = "(required) Path to usvm directory (ONLY FOR USVM)."
    ).required()

    private fun getPythonMethods(): List<List<PythonMethodHeader>> {
        val parsedModule = PythonParser(sourceFileContent).Module()

        val topLevelFunctions = PythonCode.getTopLevelFunctions(parsedModule)
        val topLevelClasses = PythonCode.getTopLevelClasses(parsedModule)

        val functions = topLevelFunctions
            .mapNotNull { parseFunctionDefinition(it) }
            .map { PythonMethodHeader(it.name.toString(), absPathToSourceFile, null) }
        val methods = topLevelClasses
            .mapNotNull { parseClassDefinition(it) }
            .map { cls ->
                PythonCode.getClassMethods(cls.body)
                    .mapNotNull { parseFunctionDefinition(it) }
                    .map { function ->
                        val parsedClassName = PythonClassId(cls.name.toString())
                        PythonMethodHeader(function.name.toString(), absPathToSourceFile, parsedClassName)
                    }
            }
        return methods + listOf(functions)
    }

    override fun run() {
        absPathToSourceFile = sourceFile.toAbsolutePath()
        sourceFileContent = File(absPathToSourceFile).readText()
        val testFramework = Pytest
        val currentPythonModuleOpt = findCurrentPythonModule(directoriesForSysPath, absPathToSourceFile)
        val currentPythonModule = when (currentPythonModuleOpt) {
            is Success -> { currentPythonModuleOpt.value }
            is Fail -> {
                logger.error(currentPythonModuleOpt.message)
                return
            }
        }
        logger.info("Checking requirements...")
        val installer = CliRequirementsInstaller(true, logger)
        val requirementsAreInstalled = RequirementsInstaller.checkRequirements(
            installer,
            pythonPath,
            if (testFramework.isInstalled) emptyList() else listOf(testFramework.mainPackage)
        )
        if (!requirementsAreInstalled) {
            return
        }

        val pythonMethodGroups = getPythonMethods()

        val globalImportCollection = mutableSetOf<PythonImport>()
        val globalCodeCollection = mutableListOf<String>()
        pythonMethodGroups.map {  pythonMethods ->
            val config = PythonTestGenerationConfig(
                pythonPath = pythonPath,
                testFileInformation = TestFileInformation(absPathToSourceFile, sourceFileContent, currentPythonModule.dropInitFile()),
                sysPathDirectories = directoriesForSysPath.map { it.toAbsolutePath() } .toSet(),
                testedMethods = pythonMethods,
                timeout = timeout,
                timeoutForRun = timeoutForRun,
                testFramework = testFramework,
                testSourceRootPath = null,
                withMinimization = !doNotMinimize,
                isCanceled = { false },
                runtimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.valueOf(runtimeExceptionTestsBehaviour),
                coverageMeasureMode = PythonCoverageMode.parse(coverageMeasureMode),
                sendCoverageContinuously = !doNotSendCoverageContinuously,
                coverageOutputFormat = CoverageOutputFormat.Lines,
                usvmConfig = UsvmConfig(javaCmd, usvmDirectory)
            )

            val processor = SbftCliProcessor(config)

            logger.info("Loading information about Python types...")
            val mypyConfig = processor.sourceCodeAnalyze()

            logger.info("Generating tests...")
            val testSets = processor.testGenerate(mypyConfig)

            val (testCode, imports) = processor.testCodeGenerateSplitImports(testSets)
            globalCodeCollection.add(testCode)
            globalImportCollection.addAll(imports)
        }
        logger.info("Saving tests...")
        val importCode = globalImportCollection
            .sortedBy { it.order }
            .map { renderPythonImport(it) }
        val testCode = (listOf(importCode.joinToString("\n")) + globalCodeCollection).joinToString("\n\n\n")
        writeToFileAndSave(output, testCode)
    }
}