package org.utbot.cli.language.python

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.long
import mu.KotlinLogging
import org.utbot.python.newtyping.inference.TypeInferenceProcessor
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.utils.Fail
import org.utbot.python.utils.RequirementsUtils.requirements
import org.utbot.python.utils.Success

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

    private val className by option(
        "-c", "--class",
        help = "Class of the function"
    )

    private val pythonPath by option(
        "-p", "--python-path",
        help = "(required) Path to Python interpreter. Use only UNC format on Windows."
    ).required()

    private val timeout by option(
        "-t", "--timout",
        help = "(required) Timeout in milliseconds for type inference."
    ).long().required()

    private val directoriesForSysPath by option(
        "-s", "--sys-path",
        help = "(required) Directories to add to sys.path. Use only UNC format on Windows. " +
                "One of directories must contain the file with the methods under test."
    ).split(",").required()

    private var startTime: Long = 0

    override fun run() {
        val moduleOpt = findCurrentPythonModule(
            directoriesForSysPath.map { it.toAbsolutePath() },
            sourceFile.toAbsolutePath()
        )
        if (moduleOpt is Fail) {
            logger.error(moduleOpt.message)
        }
        val module = (moduleOpt as Success).value

        val result = TypeInferenceProcessor(
            pythonPath,
            directoriesForSysPath.map{ it.toAbsolutePath() }.toSet(),
            sourceFile,
            module,
            function,
            className
        ).inferTypes(
            startingTypeInferenceAction = {
                startTime = System.currentTimeMillis()
                logger.info("Starting type inference...")
            },
            processSignature = { logger.info("Found signature: " + it.pythonTypeRepresentation()) },
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

        result.forEach { println(it.pythonTypeRepresentation()) }
    }
}