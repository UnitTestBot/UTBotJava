package org.utbot.cli.python

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import mu.KotlinLogging
import org.utbot.framework.codegen.Pytest
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.Unittest
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestGenerationProcessor.processTestGeneration
import org.utbot.python.code.PythonCode
import org.utbot.python.utils.RequirementsUtils.installRequirements
import org.utbot.python.utils.RequirementsUtils.requirements
import org.utbot.python.utils.getModuleName
import java.io.File

private const val DEFAULT_TIMEOUT_IN_MILLIS = 60000L
private const val DEFAULT_TIMEOUT_FOR_ONE_RUN_IN_MILLIS = 2000L

private val logger = KotlinLogging.logger {}

class PythonGenerateTestsCommand: CliktCommand(
    name = "generate_python",
    help = "Generate tests for specified Python class or top-level functions from specified file"
) {
    private val sourceFile by argument(
        help = "File with Python code to generate tests for"
    )

    private val pythonClass by option(
        "-c", "--class",
        help = "Specify top-level class under test"
    )

    private val methods by option(
        "-m", "--methods",
        help = "Specify methods under test"
    ).split(",")

    private val directoriesForSysPath by option(
        "-s", "--sys-path",
        help = "Directories to add to sys.path"
    ).split(",").required()

    private val pythonPath by option(
        "-p", "--python-path",
        help = "Path to Python interpreter"
    ).required()

    private val output by option(
        "-o", "--output",
        help = "File for generated tests"
    ).required()

    private val coverageOutput by option(
        "--coverage",
        help = "File to write coverage report"
    )

    private val installRequirementsIfMissing by option(
        "--install-requirements",
        help = "Install requirements if missing"
    ).flag(default = false)

    private val doNotMinimize by option(
        "--do-not-minimize",
        help = "Turn off minimization of number of generated tests"
    ).flag(default = false)

    private val doNotCheckRequirements by option(
        "--do-not-check-requirements",
        help = "Turn off Python requirements check"
    ).flag(default = false)

    private val visitOnlySpecifiedSource by option(
        "--visit-only-specified-source",
        help = "Do not search for classes and imported modules in other Python files from sys.path"
    ).flag(default = false)

    private val timeout by option(
        "-t", "--timeout",
        help = "Specify the maximum time in milliseconds to spend on generating tests ($DEFAULT_TIMEOUT_IN_MILLIS by default)"
    ).long().default(DEFAULT_TIMEOUT_IN_MILLIS)

    private val timeoutForRun by option(
        "--timeout-for-run",
        help = "Specify the maximum time in milliseconds to spend on one function run ($DEFAULT_TIMEOUT_FOR_ONE_RUN_IN_MILLIS by default)"
    ).long().default(DEFAULT_TIMEOUT_FOR_ONE_RUN_IN_MILLIS)

    private val testFrameworkAsString by option("--test-framework", help = "Test framework to be used")
        .choice(Pytest.toString(), Unittest.toString())
        .default(Unittest.toString())

    private val testFramework: TestFramework
        get() =
            when (testFrameworkAsString) {
                Unittest.toString() -> Unittest
                Pytest.toString() -> Pytest
                else -> error("Not reachable")
            }

    private fun findCurrentPythonModule(): Optional<String> {
        directoriesForSysPath.forEach { path ->
            val module = getModuleName(path, sourceFile)
            if (module != null)
                return Success(module)
        }
        return Fail("Couldn't find path for $sourceFile in --sys-path option. Please, specify it.")
    }

    private val forbiddenMethods = listOf("__init__", "__new__")

    private fun getPythonMethods(sourceCodeContent: String, currentModule: String): Optional<List<PythonMethod>> {
        val code = PythonCode.getFromString(
            sourceCodeContent,
            sourceFile.toAbsolutePath(),
            pythonModule = currentModule
        )
            ?: return Fail("Couldn't parse source file. Maybe it contains syntax error?")

        val topLevelFunctions = code.getToplevelFunctions()
        val selectedMethods = methods
        if (pythonClass == null && methods == null) {
            return if (topLevelFunctions.isNotEmpty())
                Success(topLevelFunctions)
            else
                Fail("No top-level functions in the source file to test.")
        } else if (pythonClass == null && selectedMethods != null) {
            val pythonMethodsOpt = selectedMethods.map { functionName ->
                topLevelFunctions
                    .find { it.name == functionName }
                    ?.let { Success(it) }
                    ?: Fail("Couldn't find top-level function $functionName in the source file.")
            }
            return pack(*pythonMethodsOpt.toTypedArray())
        }

        val pythonClassFromSources = code.getToplevelClasses().find { it.name == pythonClass }
            ?.let { Success(it) }
            ?: Fail("Couldn't find class $pythonClass in the source file.")

        val methods = bind(pythonClassFromSources) {
            val fineMethods: List<PythonMethod> = it.methods.filter { method -> method.name !in forbiddenMethods }
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
                    classFineMethods.find { it.name == methodName } ?.let { Success(it) }
                        ?: Fail("Couldn't find method $methodName of class $pythonClass")
                }).toTypedArray()
            )
        }
    }

    private lateinit var currentPythonModule: String
    private lateinit var pythonMethods: List<PythonMethod>
    private lateinit var sourceFileContent: String
    private lateinit var testSourceRoot: String
    private lateinit var outputFilename: String

    @Suppress("UNCHECKED_CAST")
    private fun calculateValues(): Optional<Unit> {
        val outputFile = File(output.toAbsolutePath())
        testSourceRoot = outputFile.parentFile.path
        outputFilename = outputFile.name
        val currentPythonModuleOpt = findCurrentPythonModule()
        sourceFileContent = File(sourceFile).readText()
        val pythonMethodsOpt = bind(currentPythonModuleOpt) { getPythonMethods(sourceFileContent, it) }

        return bind(pack(currentPythonModuleOpt, pythonMethodsOpt)) {
            currentPythonModule = it[0] as String
            pythonMethods = it[1] as List<PythonMethod>
            Success(Unit)
        }
    }

    private fun processMissingRequirements(): PythonTestGenerationProcessor.MissingRequirementsActionResult {
        if (installRequirementsIfMissing) {
            logger.info("Installing requirements...")
            val result = installRequirements(pythonPath)
            if (result.exitValue == 0)
                return PythonTestGenerationProcessor.MissingRequirementsActionResult.INSTALLED
            System.err.println(result.stderr)
            logger.error("Failed to install requirements.")
        } else {
            logger.error("Missing some requirements. Please add --install-requirements flag or install them manually.")
        }
        logger.info("Requirements: ${requirements.joinToString()}")
        return PythonTestGenerationProcessor.MissingRequirementsActionResult.NOT_INSTALLED
    }

    override fun run() {
        val status = calculateValues()
        if (status is Fail) {
            logger.error(status.message)
            return
        }

        processTestGeneration(
            pythonPath = pythonPath.toAbsolutePath(),
            testSourceRoot = testSourceRoot,
            pythonFilePath = sourceFile.toAbsolutePath(),
            pythonFileContent = sourceFileContent,
            directoriesForSysPath = directoriesForSysPath.map { it.toAbsolutePath() } .toSet(),
            currentPythonModule = currentPythonModule,
            pythonMethods = pythonMethods,
            containingClassName = pythonClass,
            timeout = timeout,
            testFramework = testFramework,
            codegenLanguage = CodegenLanguage.PYTHON,
            outputFilename = outputFilename,
            timeoutForRun = timeoutForRun,
            withMinimization = !doNotMinimize,
            doNotCheckRequirements = doNotCheckRequirements,
            visitOnlySpecifiedSource = visitOnlySpecifiedSource,
            checkingRequirementsAction = {
                logger.info("Checking requirements...")
            },
            requirementsAreNotInstalledAction = ::processMissingRequirements,
            startedLoadingPythonTypesAction = {
                logger.info("Loading information about Python types...")
            },
            startedTestGenerationAction = {
                logger.info("Generating tests...")
            },
            notGeneratedTestsAction = {
                logger.error(
                    "Couldn't generate tests for the following functions: ${it.joinToString()}"
                )
            },
            processMypyWarnings = { messages -> messages.forEach { println(it) } },
            finishedAction = {
                logger.info("Finished test generation for the following functions: ${it.joinToString()}")
            },
            processCoverageInfo = { coverageReport ->
                val output = coverageOutput ?: return@processTestGeneration
                val file = File(output)
                file.writeText(coverageReport)
                file.createNewFile()
            }
        )
    }

    private fun String.toAbsolutePath(): String =
        File(this).canonicalPath
}