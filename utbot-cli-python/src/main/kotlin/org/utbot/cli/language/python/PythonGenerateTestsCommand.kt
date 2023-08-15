package org.utbot.cli.language.python

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import mu.KotlinLogging
import org.parsers.python.PythonParser
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.python.PythonMethodHeader
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.PythonTestSet
import org.utbot.python.utils.RequirementsInstaller
import org.utbot.python.TestFileInformation
import org.utbot.python.code.PythonCode
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest
import org.utbot.python.newtyping.ast.parseClassDefinition
import org.utbot.python.newtyping.ast.parseFunctionDefinition
import org.utbot.python.newtyping.mypy.dropInitFile
import org.utbot.python.utils.*
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

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

    private val pythonClass by option(
        "-c", "--class",
        help = "Specify top-level (ordinary, not nested) class under test. " +
                "Without this option tests will be generated for top-level functions."
    )

    private val methods by option(
        "-m", "--methods",
        help = "Specify methods under test."
    ).split(",")

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
    )
        .required()
        .check("Must end with .py suffix") {
            it.endsWith(".py")
        }

    private val coverageOutput by option(
        "--coverage",
        help = "File to write coverage report."
    )

    private val installRequirementsIfMissing by option(
        "--install-requirements",
        help = "Install Python requirements if missing."
    ).flag(default = false)

    private val doNotMinimize by option(
        "--do-not-minimize",
        help = "Turn off minimization of the number of generated tests."
    ).flag(default = false)

    private val doNotCheckRequirements by option(
        "--do-not-check-requirements",
        help = "Turn off Python requirements check (to speed up)."
    ).flag(default = false)

    private val timeout by option(
        "-t", "--timeout",
        help = "Specify the maximum time in milliseconds to spend on generating tests ($DEFAULT_TIMEOUT_IN_MILLIS by default)."
    ).long().default(DEFAULT_TIMEOUT_IN_MILLIS)

    private val timeoutForRun by option(
        "--timeout-for-run",
        help = "Specify the maximum time in milliseconds to spend on one function run ($DEFAULT_TIMEOUT_FOR_ONE_RUN_IN_MILLIS by default)."
    ).long().default(DEFAULT_TIMEOUT_FOR_ONE_RUN_IN_MILLIS)

    private val testFrameworkAsString by option("--test-framework", help = "Test framework to be used.")
        .choice(Pytest.toString(), Unittest.toString())
        .default(Unittest.toString())

    private val runtimeExceptionTestsBehaviour by option("--runtime-exception-behaviour", help = "PASS or FAIL")
        .choice("PASS", "FAIL")
        .default("FAIL")

    private val doNotGenerateRegressionSuite by option("--do-not-generate-regression-suite", help = "Do not generate regression test suite")
        .flag(default = false)

    private val testFramework: TestFramework
        get() =
            when (testFrameworkAsString) {
                Unittest.toString() -> Unittest
                Pytest.toString() -> Pytest
                else -> error("Not reachable")
            }

    private val forbiddenMethods = listOf("__init__", "__new__")

    private fun getPythonMethods(): Optional<List<PythonMethodHeader>> {
        val parsedModule = PythonParser(sourceFileContent).Module()

        val topLevelFunctions = PythonCode.getTopLevelFunctions(parsedModule)
        val topLevelClasses = PythonCode.getTopLevelClasses(parsedModule)

        val selectedMethods = methods
        if (pythonClass == null && methods == null) {
            return if (topLevelFunctions.isNotEmpty())
                Success(
                    topLevelFunctions
                        .mapNotNull { parseFunctionDefinition(it) }
                        .map { PythonMethodHeader(it.name.toString(), sourceFile, null) }
                )
            else {
                val topLevelClassMethods = topLevelClasses
                    .mapNotNull { parseClassDefinition(it) }
                    .flatMap { cls ->
                        PythonCode.getClassMethods(cls.body)
                            .mapNotNull { parseFunctionDefinition(it) }
                            .map { function ->
                                val parsedClassName = PythonClassId(cls.name.toString())
                                PythonMethodHeader(function.name.toString(), sourceFile, parsedClassName)
                            }
                    }
                if (topLevelClassMethods.isNotEmpty()) {
                    Success(topLevelClassMethods)
                } else
                    Fail("No top-level functions and top-level classes in the source file to test.")
            }
        } else if (pythonClass == null && selectedMethods != null) {
            val pythonMethodsOpt = selectedMethods.map { functionName ->
                topLevelFunctions
                    .mapNotNull { parseFunctionDefinition(it) }
                    .map { PythonMethodHeader(it.name.toString(), sourceFile, null) }
                    .find { it.name == functionName }
                    ?.let { Success(it) }
                    ?: Fail("Couldn't find top-level function $functionName in the source file.")
            }
            return pack(*pythonMethodsOpt.toTypedArray())
        }

        val pythonClassFromSources = topLevelClasses
            .mapNotNull { parseClassDefinition(it) }
            .find { it.name.toString() == pythonClass }
            ?.let { Success(it) }
            ?: Fail("Couldn't find class $pythonClass in the source file.")

        val methods = bind(pythonClassFromSources) { parsedClass ->
            val parsedClassId = PythonClassId(parsedClass.name.toString())
            val methods = PythonCode.getClassMethods(parsedClass.body).mapNotNull { parseFunctionDefinition(it) }
            val fineMethods = methods
                .filter { !forbiddenMethods.contains(it.name.toString()) }
                .map {
                    PythonMethodHeader(it.name.toString(), sourceFile, parsedClassId)
                }
            if (fineMethods.isNotEmpty())
                Success(fineMethods)
            else
                Fail("No methods in definition of class $pythonClass to test.")
        }

        if (selectedMethods == null)
            return methods

        return bind(methods) { classFineMethods ->
            pack(
                *(selectedMethods.map { methodName ->
                    classFineMethods.find { it.name == methodName }?.let { Success(it) }
                        ?: Fail("Couldn't find method $methodName of class $pythonClass")
                }).toTypedArray()
            )
        }
    }

    private lateinit var currentPythonModule: String
    private lateinit var pythonMethods: List<PythonMethodHeader>
    private lateinit var sourceFileContent: String

    @Suppress("UNCHECKED_CAST")
    private fun calculateValues(): Optional<Unit> {
        val currentPythonModuleOpt = findCurrentPythonModule(directoriesForSysPath, sourceFile)
        sourceFileContent = File(sourceFile).readText()
        val pythonMethodsOpt = bind(currentPythonModuleOpt) { getPythonMethods() }

        return bind(pack(currentPythonModuleOpt, pythonMethodsOpt)) {
            currentPythonModule = it[0] as String
            pythonMethods = it[1] as List<PythonMethodHeader>
            Success(Unit)
        }
    }

    override fun run() {
        val status = calculateValues()
        if (status is Fail) {
            logger.error(status.message)
            return
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

        val config = PythonTestGenerationConfig(
            pythonPath = pythonPath,
            testFileInformation = TestFileInformation(sourceFile.toAbsolutePath(), sourceFileContent, currentPythonModule.dropInitFile()),
            sysPathDirectories = directoriesForSysPath.map { it.toAbsolutePath() } .toSet(),
            testedMethods = pythonMethods,
            timeout = timeout,
            timeoutForRun = timeoutForRun,
            testFramework = testFramework,
            testSourceRootPath = Paths.get(output.toAbsolutePath()).parent.toAbsolutePath(),
            withMinimization = !doNotMinimize,
            isCanceled = { false },
            runtimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.valueOf(runtimeExceptionTestsBehaviour)
        )

        val processor = PythonCliProcessor(
            config,
            output.toAbsolutePath(),
            logger,
            coverageOutput?.toAbsolutePath(),
        )

        logger.info("Loading information about Python types...")
        val (mypyStorage, _) = processor.sourceCodeAnalyze()

        logger.info("Generating tests...")
        var testSets = processor.testGenerate(mypyStorage)
        if (testSets.isEmpty()) return
        if (doNotGenerateRegressionSuite) {
            testSets = testSets.map { testSet ->
                PythonTestSet(
                    testSet.method,
                    testSet.executions.filterNot { it.result is UtExecutionSuccess },
                    testSet.errors,
                    testSet.mypyReport,
                    testSet.classId
                )
            }
        }

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
