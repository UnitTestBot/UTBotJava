package org.utbot.cli.python

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import mu.KotlinLogging
import org.utbot.framework.codegen.Unittest
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationProcessor.processTestGeneration
import org.utbot.python.code.PythonCode
import org.utbot.python.utils.getModuleName
import java.io.File

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

    private fun findCurrentPythonModule(): Either<String> {
        directoriesForSysPath.forEach { path ->
            val module = getModuleName(path, sourceFile)
            if (module != null)
                return Success(module)
        }
        return Fail("Couldn't find path for $sourceFile in --python-path option. Please, specify it.")
    }

    private fun getPythonMethods(sourceCodeContent: String, currentModule: String): Either<List<PythonMethod>> {
        val code = PythonCode.getFromString(sourceCodeContent, pythonModule = currentModule)
        if (pythonClass == null)
            return Success(code.getToplevelFunctions())

        code.getToplevelClasses().forEach {
            if (it.name == pythonClass)
                return Success(it.methods)
        }

        return Fail("Couldn't find class $pythonClass in file $output.")
    }

    private lateinit var currentPythonModule: String
    private lateinit var pythonMethods: List<PythonMethod>
    private lateinit var sourceFileContent: String

    @Suppress("UNCHECKED_CAST")
    private fun calculateValues(): Either<Unit> {
        val currentPythonModuleE = findCurrentPythonModule()
        sourceFileContent = File(sourceFile).readText()
        val pythonMethodsE = go(currentPythonModuleE) { getPythonMethods(sourceFileContent, it) }

        return go(pack(currentPythonModuleE, pythonMethodsE)) {
            currentPythonModule = it[0] as String
            pythonMethods = it[1] as List<PythonMethod>
            Success(Unit)
        }
    }

    override fun run() {
        val outputFile = File(output)
        val testSourceRoot = outputFile.parentFile.path
        val outputFilename = outputFile.name
        val status = calculateValues()
        if (status is Fail) {
            println(status.message)
            return
        }

        processTestGeneration(
            pythonPath = pythonPath,
            testSourceRoot = testSourceRoot,
            pythonFilePath = sourceFile,
            pythonFileContent = sourceFileContent,
            directoriesForSysPath = directoriesForSysPath.toSet(),
            currentPythonModule = currentPythonModule,
            pythonMethods = pythonMethods,
            containingClassName = pythonClass,
            timeout = DEFAULT_TIMEOUT_IN_MILLIS,
            testFramework = Unittest,
            codegenLanguage = CodegenLanguage.PYTHON,
            outputFilename = outputFilename,
            checkingRequirementsAction = {
                logger.info("Checking requirements...")
            },
            requirementsAreNotInstalledAction = {
                logger.error("Requirements are not installed")
            },
            startedLoadingPythonTypesAction = {
                logger.info("Loading information about Python Types...")
            },
            startedTestGenerationAction = {
                logger.info("Generating tests...")
            },
            notGeneratedTestsAction = {
                logger.warn(
                    "Couldn't generate tests for the following functions: ${it.joinToString()}"
                )
            }
        )
    }
}