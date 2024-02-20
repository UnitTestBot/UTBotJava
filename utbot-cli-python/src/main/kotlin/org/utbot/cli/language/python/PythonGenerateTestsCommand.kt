package org.utbot.cli.language.python

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
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.python.MypyConfig
import org.utbot.python.PythonMethodHeader
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestSet
import org.utbot.python.TestFileInformation
import org.utbot.python.code.PythonCode
import org.utbot.python.coverage.CoverageOutputFormat
import org.utbot.python.coverage.PythonCoverageMode
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.pythonBuiltinsModuleName
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest
import org.utbot.python.newtyping.ast.parseClassDefinition
import org.utbot.python.newtyping.ast.parseFunctionDefinition
import org.utpython.types.mypy.dropInitFile
import org.utbot.python.utils.Cleaner
import org.utbot.python.utils.Fail
import org.utbot.python.utils.RequirementsInstaller
import org.utbot.python.utils.Success
import org.utbot.python.utils.separateTimeout
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

private const val DEFAULT_TIMEOUT_IN_MILLIS = 60000L
private const val DEFAULT_TIMEOUT_FOR_ONE_RUN_IN_MILLIS = 2000L

private val logger = KotlinLogging.logger {}

class PythonGenerateTestsCommand : CliktCommand(
    name = "generate_python",
    help = "Generate tests for a specified Python class or top-level functions from a specified file."
) {
    private val sourceFile by argument(
        help = "File with Python code to generate tests for."
    )

    private lateinit var absPathToSourceFile: String
    private lateinit var sourceFileContent: String

    private val classesToTest by option(
        "-c", "--classes",
        help = "Generate tests for all methods of selected classes. Use '-' to specify top-level functions group."
    )
        .split(",")
    private fun classesToTest() = classesToTest?.map { if (it == "-") null else it }

    private val methodsToTest by option(
        "-m", "--methods",
        help = "Specify methods under test using full path (use qualified name: only name for top-level function and <class>.<name> for methods. Use '-' to skip test generation for top-level functions"
    )
        .split(",")

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

    private val coverageOutput by option(
        "--coverage",
        help = "File to write coverage report."
    )

    private val executionCounterOutput by option(
        "--executions-counter",
        help = "File to write number of executions."
    )

    private val installRequirementsIfMissing by option(
        "--install-requirements",
        help = "Install Python requirements if missing."
    ).flag(default = false)

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

    private val testFrameworkAsString by option("--test-framework", help = "Test framework to be used.")
        .choice(Pytest.toString(), Unittest.toString())
        .default(Unittest.toString())

    private val runtimeExceptionTestsBehaviour by option("--runtime-exception-behaviour", help = "PASS or FAIL")
        .choice("PASS", "FAIL")
        .default("FAIL")

    private val doNotGenerateRegressionSuite by option("--do-not-generate-regression-suite", help = "Do not generate regression test suite.")
        .flag(default = false)

    private val coverageMeasureMode by option("--coverage-measure-mode", help = "Use LINES or INSTRUCTIONS for coverage measurement.")
        .choice("INSTRUCTIONS", "LINES")
        .default("INSTRUCTIONS")

    private val doNotSendCoverageContinuously by option("--do-not-send-coverage-continuously", help = "Do not send coverage during execution.")
        .flag(default = false)

    private val prohibitedExceptions by option(
        "--prohibited-exceptions",
        help = "Do not generate tests with these exceptions. Set '-' to generate tests for all exceptions."
    )
        .split(",")
        .default(PythonTestGenerationConfig.defaultProhibitedExceptions)

    private val testFramework: TestFramework
        get() =
            when (testFrameworkAsString) {
                Unittest.toString() -> Unittest
                Pytest.toString() -> Pytest
                else -> error("Not reachable")
            }

    private val forbiddenMethods = listOf("__init__", "__new__")

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

        fun functionsFilter(group: List<PythonMethodHeader>, methodFilter: List<String>? = methodsToTest): List<PythonMethodHeader> {
            return methodFilter?.let {
                if (it.isEmpty()) group
                else group.filter { method -> method.fullname in it }
            } ?: group
        }

        fun methodsFilter(group: List<PythonMethodHeader>, containingClass: PythonClassId): List<PythonMethodHeader> {
            val localMethodFilter = methodsToTest?.let { it.filter { name -> name.startsWith(containingClass.typeName) } }
            return functionsFilter(group, localMethodFilter)
        }

        fun groupFilter(group: List<PythonMethodHeader>, classFilter: List<String?>?): List<PythonMethodHeader> {
            if (group.isEmpty()) return emptyList()
            val groupClass = group.first().containingPythonClassId
            if (classFilter != null && groupClass?.typeName !in classFilter) return emptyList()
            return if (groupClass == null) functionsFilter(group)
            else methodsFilter(group, groupClass)
        }

        val methodGroups = (methods + listOf(functions))
            .map { groupFilter(it, classesToTest()) }
            .map {
                it.filter { forbiddenMethods.all { name -> !it.name.endsWith(name) } }
            }
            .filter { it.isNotEmpty() }

        methodsToTest?.forEach { name ->
            require(methodGroups.flatten().any { it.fullname == name }) { "Cannot find function '$name' in file '$absPathToSourceFile'" }
        }
        classesToTest()?.forEach { name ->
            require(methodGroups.flatten().any { it.containingPythonClassId?.typeName == name }) { "Cannot find class '$name' or methods in file '$absPathToSourceFile'" }
        }
        return methodGroups
    }

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

    private val testWriter = TestWriter()

    private fun saveTests() {
        logger.info("Saving tests...")
        val testCode = testWriter.generateTestCode()
        writeToFileAndSave(output, testCode)

        Cleaner.doCleaning()
        alreadySaved.set(true)
    }

    private fun initialize() {
        absPathToSourceFile = sourceFile.toAbsolutePath()
        sourceFileContent = File(absPathToSourceFile).readText()
    }

    override fun run() {
        initialize()

        val sysPathDirectories = directoriesForSysPath.map { it.toAbsolutePath() }.toSet()
        val currentPythonModule = when (val module = findCurrentPythonModule(sysPathDirectories, absPathToSourceFile)) {
            is Success -> {
                module.value
            }

            is Fail -> {
                logger.error { module.message }
                return
            }
        }

        logger.info("Checking requirements...")
        val installer = CliRequirementsInstaller(installRequirementsIfMissing, logger)
        val requirementsAreInstalled = RequirementsInstaller.checkRequirements(
            installer,
            pythonPath,
            if (testFramework.isInstalled) emptyList() else listOf(testFramework.mainPackage)
        )
        if (!requirementsAreInstalled) {
            return
        }

        val pythonMethodGroups = getPythonMethods()

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
        pythonMethodGroups.forEachIndexed { index, pythonMethods ->
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
                testSourceRootPath = Paths.get(output.toAbsolutePath()).parent.toAbsolutePath(),
                withMinimization = !doNotMinimize,
                isCanceled = { shutdown.get() },
                runtimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.valueOf(runtimeExceptionTestsBehaviour),
                coverageMeasureMode = PythonCoverageMode.parse(coverageMeasureMode),
                sendCoverageContinuously = !doNotSendCoverageContinuously,
                coverageOutputFormat = CoverageOutputFormat.Lines,
                prohibitedExceptions = if (prohibitedExceptions == listOf("-")) emptyList() else prohibitedExceptions,
            )
            val processor = PythonCliProcessor(
                config,
                testWriter,
                coverageOutput,
                executionCounterOutput,
            )

            logger.info("Generating tests...")
            val testSets = processor.testGenerate(mypyConfig).let {
                return@let if (doNotGenerateRegressionSuite) {
                    it.map { testSet ->
                        PythonTestSet(
                            testSet.method,
                            testSet.executions.filterNot { execution -> execution.result is UtExecutionSuccess },
                            testSet.errors,
                            testSet.executionsNumber,
                            testSet.clustersInfo,
                        )
                    }
                } else {
                    it
                }
            }
            if (testSets.isNotEmpty()) {
                logger.info("Saving tests...")
                val testCode = processor.testCodeGenerate(testSets)
                processor.saveTests(testCode)


                logger.info("Saving coverage report...")
                processor.processCoverageInfo(testSets)

                logger.info(
                    "Finished test generation for the following functions: ${
                        testSets.joinToString { it.method.name }
                    }"
                )
            }
        }
        saveTests()
        removeShutdownHook()
        exitProcess(0)
    }
}
