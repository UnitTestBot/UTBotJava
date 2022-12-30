package org.utbot.cli.language.python

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import mu.KotlinLogging
import org.parsers.python.PythonParser
import org.parsers.python.ast.FunctionDefinition
import org.utbot.python.PythonArgument
import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.parseFunctionDefinition
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.inference.TypeInferenceProcessor
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.runmypy.getErrorNumber
import org.utbot.python.newtyping.runmypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.runmypy.setConfigFile
import org.utbot.python.utils.*
import org.utbot.python.utils.RequirementsUtils.requirements
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

class PythonTypeInferenceCommand : CliktCommand(
    name = "infer_types",
    help = "Infer types for the specified Python top-level function."
) {
    private val sourceFile by argument(
        help = "File with Python code."
    )

    private val function by argument(
        help = "Function to infer types for."
    )

    private val pythonPath by option(
        "-p", "--python-path",
        help = "(required) Path to Python interpreter."
    ).required()

    private val timeout by option(
        "-t", "--timout",
        help = "(required) Timeout in milliseconds for type inference."
    ).long().required()

    private var startTime: Long = 0

    override fun run() {
        val types = TypeInferenceProcessor(
            pythonPath,
            sourceFile,
            function
        ).inferTypes(
            startingTypeInferenceAction = {
                startTime = System.currentTimeMillis()
                logger.info("Starting type inference...")
            },
            cancel = { System.currentTimeMillis() - startTime > timeout },
            checkRequirementsAction = { logger.info("Checking Python requirements...") },
            missingRequirementsAction = {
                logger.error("Some of the following Python requirements are missing: " +
                        "${requirements.joinToString()}. Please install them.")
            },
            loadingInfoAboutTypesAction = { logger.info("Loading information about types...") },
            analyzingCodeAction = { logger.info("Analyzing code...") },
            pythonMethodExtractionFailAction = { logger.error(it) }
        )

        types.forEach {
            println(it.pythonTypeRepresentation())
        }
    }
}