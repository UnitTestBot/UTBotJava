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
import org.utbot.python.MypyConfig
import org.utbot.python.PythonMethodHeader
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.TestFileInformation
import org.utbot.python.UsvmConfig
import org.utbot.python.code.PythonCode
import org.utbot.python.coverage.CoverageOutputFormat
import org.utbot.python.coverage.PythonCoverageMode
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.pythonBuiltinsModuleName
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.PythonImport
import org.utbot.python.newtyping.ast.parseClassDefinition
import org.utbot.python.newtyping.ast.parseFunctionDefinition
import org.utbot.python.newtyping.mypy.dropInitFile
import org.utbot.python.utils.Cleaner
import org.utbot.python.utils.Fail
import org.utbot.python.utils.RequirementsInstaller
import org.utbot.python.utils.Success
import org.utbot.python.utils.separateTimeout
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

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

    private val includeMypyAnalysisTime by option(
        "--include-mypy-analysis-time",
        help = "Include mypy static analysis time in the total timeout."
    ).flag(default = false)

    private val runtimeExceptionTestsBehaviour by option("--runtime-exception-behaviour", help = "PASS or FAIL")
        .choice("PASS", "FAIL")
        .default("FAIL")

    private val coverageMeasureMode by option("--coverage-measure-mode", help = "Use LINES or INSTRUCTIONS for coverage measurement.")
        .choice("INSTRUCTIONS", "LINES")
        .default("INSTRUCTIONS")

    private val doNotSendCoverageContinuously by option("--do-not-send-coverage-continuously", help = "Do not send coverage during execution.")
        .flag(default = false)

    private val prohibitedExceptions by option("--prohibited-exceptions", help = "Do not generate tests with these exceptions. Set '-' to generate tests for all exceptions.")
        .split(",")
        .default(PythonTestGenerationConfig.defaultProhibitedExceptions)

    private val doNotGenerateStateAssertions by option(
        "--do-not-generate-state-assertions",
        help = "Do not generate state assertions for all functions excluding functions with None return value."
    )
        .flag(default = false)

    private val javaCmd by option(
        "--java-cmd",
        help = "(required) Path to Java command (ONLY FOR USVM)."
    ).required()

    private val usvmDirectory by option(
        "--usvm-dir",
        help = "(required) Path to usvm directory (ONLY FOR USVM)."
    ).required()

    private val checkUsvm by option("--check-usvm", help = "Check usvm (ONLY FOR USVM).")
        .flag(default = false)

    private fun getPythonMethods(): List<List<PythonMethodHeader>> {
        val parsedModule = PythonParser(sourceFileContent).Module()

        val topLevelFunctions = PythonCode.getTopLevelFunctions(parsedModule)
        val topLevelClasses = PythonCode.getTopLevelClasses(parsedModule)

        val functions = topLevelFunctions
            .mapNotNull { parseFunctionDefinition(it) }
            .map { PythonMethodHeader(it.name.toString(), absPathToSourceFile, null) }
        val methods = topLevelClasses
            .mapNotNull { cls ->
                val parsedClass = parseClassDefinition(cls) ?: return@mapNotNull null
                val innerClasses = PythonCode.getInnerClasses(cls)
                (listOf(parsedClass to null) + innerClasses.mapNotNull { innerClass -> parseClassDefinition(innerClass)?.let { it to parsedClass } }).map { (cls, parent) ->
                    PythonCode.getClassMethods(cls.body)
                        .mapNotNull { parseFunctionDefinition(it) }
                        .map { function ->
                            val clsName = (parent?.let { "${it.name}." } ?: "") + cls.name.toString()
                            val parsedClassName = PythonClassId(pythonBuiltinsModuleName, clsName)
                            PythonMethodHeader(function.name.toString(), absPathToSourceFile, parsedClassName)
                        }
                }
            }
            .flatten()
        return (methods + listOf(functions)).filter { it.isNotEmpty() }
    }

    private val globalImportCollection = mutableSetOf<PythonImport>()
    private val globalCodeCollection = mutableListOf<String>()

    private val shutdown: AtomicBoolean = AtomicBoolean(false)
    private val alreadySaved: AtomicBoolean = AtomicBoolean(false)

    private val shutdownThread =
        object : Thread() {
            override fun run() {
                shutdown.set(true)
                try {
                    if (!alreadySaved.get()) {
                        saveTests()
                    }
                } catch (_: InterruptedException) {
                    logger.warn { "Interrupted exception" }
                }
            }
        }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(shutdownThread)
    }

    private fun removeShutdownHook() {
        Runtime.getRuntime().removeShutdownHook(shutdownThread)
    }

    private fun saveTests() {
        logger.info("Saving tests...")
        val importCode = globalImportCollection
            .sortedBy { it.order }
            .map { renderPythonImport(it) }
        val testCode = (listOf(importCode.joinToString("\n")) + globalCodeCollection).joinToString("\n\n\n")
        writeToFileAndSave(output, testCode)

        Cleaner.doCleaning()
        alreadySaved.set(true)
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

        val pythonMethodGroups = getPythonMethods().let { if (checkUsvm) it.take(1).map { it.take(1) } else it }

        val sysPathDirectories = directoriesForSysPath.map { it.toAbsolutePath() } .toSet()
        val testFile = TestFileInformation(absPathToSourceFile, sourceFileContent, currentPythonModule.dropInitFile())

        val mypyConfig: MypyConfig
        val mypyTime = measureTimeMillis {
            logger.info("Loading information about Python types...")
            mypyConfig = PythonTestGenerationProcessor.sourceCodeAnalyze(
                sysPathDirectories,
                pythonPath,
                testFile,
            )
        }
        logger.info { "Mypy time: $mypyTime" }

        addShutdownHook()

        val startTime = System.currentTimeMillis()
        val countOfFunctions = pythonMethodGroups.sumOf { it.size }
        val timeoutAfterMypy = if (includeMypyAnalysisTime) timeout - mypyTime else timeout
        val oneFunctionTimeout = separateTimeout(timeoutAfterMypy, countOfFunctions)
        logger.info { "One function timeout: ${oneFunctionTimeout}ms. x${countOfFunctions}" }
        pythonMethodGroups.mapIndexed { index, pythonMethods ->
            val usedTime = System.currentTimeMillis() - startTime
            val countOfTestedFunctions = pythonMethodGroups.take(index).sumOf { it.size }
            val expectedTime = countOfTestedFunctions * oneFunctionTimeout
            val localOneFunctionTimeout = if (usedTime < expectedTime) {
                separateTimeout(timeoutAfterMypy - usedTime, countOfFunctions - countOfTestedFunctions)
            } else {
                oneFunctionTimeout
            }
            val localTimeout = pythonMethods.size * localOneFunctionTimeout
            logger.info { "Timeout for current group: ${localTimeout}ms" }

            val config = PythonTestGenerationConfig(
                pythonPath = pythonPath,
                testFileInformation = testFile,
                sysPathDirectories = sysPathDirectories,
                testedMethods = pythonMethods,
                timeout = localTimeout,
                timeoutForRun = timeoutForRun,
                testFramework = testFramework,
                testSourceRootPath = null,
                withMinimization = !doNotMinimize,
                isCanceled = { shutdown.get() },
                runtimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.valueOf(runtimeExceptionTestsBehaviour),
                coverageMeasureMode = PythonCoverageMode.parse(coverageMeasureMode),
                sendCoverageContinuously = !doNotSendCoverageContinuously,
                coverageOutputFormat = CoverageOutputFormat.Lines,
                usvmConfig = UsvmConfig(javaCmd, usvmDirectory),
                prohibitedExceptions = if (prohibitedExceptions == listOf("-")) emptyList() else prohibitedExceptions,
                checkUsvm = checkUsvm,
                doNotGenerateStateAssertions = doNotGenerateStateAssertions,
            )
            val processor = SbftCliProcessor(config)

            logger.info("Generating tests...")
            val testSets = processor.testGenerate(mypyConfig)
            if (testSets.isNotEmpty()) {
                val (testCode, imports) = processor.testCodeGenerateSplitImports(testSets)
                globalCodeCollection.add(testCode)
                globalImportCollection.addAll(imports)
            }
        }
        saveTests()
        removeShutdownHook()
        exitProcess(0)
    }
}
