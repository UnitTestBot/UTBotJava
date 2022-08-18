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
import org.utbot.python.utils.getModuleName
import java.io.File
import java.nio.file.Paths

private const val DEFAULT_TIMEOUT_IN_MILLIS = 60000L

private val logger = KotlinLogging.logger {}

class PythonGenerateTestsCommand: CliktCommand(
    name = "generate_python",
    help = "Generates tests for specified Python class or top-level functions from specified file"
) {
    private val sourceFile by argument(
        help = "File with Python code to generate tests for"
    )

    private val pythonClass by option(
        "-c", "--class",
        help = "Specify top-level class under test"
    )

    private val directoriesForSysPath by option(
        "-sp", "--sys-path",
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

    private val installRequirementsIfMissing by option(
        "-r", "--install-requirements",
        help = "Install requirements if missing"
    ).flag(default = false)

    private val timeout by option(
        "-t", "--timeout",
        help = "Specifies the maximum time in milliseconds used to generate tests ($DEFAULT_TIMEOUT_IN_MILLIS by default)"
    ).long().default(DEFAULT_TIMEOUT_IN_MILLIS)

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
        return Fail("Couldn't find path for $sourceFile in --python-path option. Please, specify it.")
    }

    private val forbiddenMethods = listOf("__init__", "__new__")

    private fun getPythonMethods(sourceCodeContent: String, currentModule: String): Optional<List<PythonMethod>> {
        val code = PythonCode.getFromString(sourceCodeContent, pythonModule = currentModule)
        if (pythonClass == null) {
            val functions = code.getToplevelFunctions()
            return if (functions.isNotEmpty()) Success(functions) else Fail("No top-level functions in file to test.")
        }

        code.getToplevelClasses().forEach { curClass ->
            if (curClass.name == pythonClass) {
                val methods = curClass.methods.filter { it.name !in forbiddenMethods }
                return if (methods.isNotEmpty())
                    Success(methods)
                else
                    Fail("No methods in definition of class $pythonClass to test.")
            }
        }

        return Fail("Couldn't find class $pythonClass in file $output.")
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
        val currentPythonModuleE = findCurrentPythonModule()
        sourceFileContent = File(sourceFile).readText()
        val pythonMethodsE = bind(currentPythonModuleE) { getPythonMethods(sourceFileContent, it) }

        return bind(pack(currentPythonModuleE, pythonMethodsE)) {
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
            println(result.stderr)
            logger.error("Failed to install requirements.")
        } else {
            logger.error("Missing some requirements. Please add --install-requirements flag or install them manually.")
        }
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
            checkingRequirementsAction = {
                logger.info("Checking requirements...")
            },
            requirementsAreNotInstalledAction = ::processMissingRequirements,
            startedLoadingPythonTypesAction = {
                logger.info("Loading information about Python Types...")
            },
            startedTestGenerationAction = {
                logger.info("Generating tests...")
            },
            notGeneratedTestsAction = {
                logger.error(
                    "Couldn't generate tests for the following functions: ${it.joinToString()}"
                )
            },
            finishedAction = {
                logger.info("Finished test generation for the following functions: ${it.joinToString()}")
            }
        )
    }

    private fun String.toAbsolutePath(): String =
        Paths.get(this).toAbsolutePath().toString()
}