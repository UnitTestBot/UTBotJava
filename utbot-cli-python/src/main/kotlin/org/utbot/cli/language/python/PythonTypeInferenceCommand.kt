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
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.runmypy.getErrorNumber
import org.utbot.python.newtyping.runmypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.runmypy.setConfigFile
import org.utbot.python.utils.Cleaner
import org.utbot.python.utils.RequirementsUtils
import org.utbot.python.utils.RequirementsUtils.requirements
import org.utbot.python.utils.TemporaryFileManager
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

    private val path: Path by lazy { Paths.get(File(sourceFile).canonicalPath) }
    private lateinit var moduleName: String

    private val sourceFileContent by lazy { File(sourceFile).readText() }
    private val parsedFile by lazy { PythonParser(sourceFileContent).Module() }

    private fun getPythonMethod(mypyAnnotationStorage: MypyAnnotationStorage): Optional<PythonMethod> {
        val funcDef = parsedFile.children().asSequence().mapNotNull { node ->
            val res = (node as? FunctionDefinition)?.let { parseFunctionDefinition(it) }
            if (res?.name?.toString() == function) res else null
        }.firstOrNull() ?: return Fail("Couldn't find top-level function $function")

        val type = mypyAnnotationStorage.definitions[moduleName]!![function]!!.annotation.asUtBotType as? FunctionType
            ?: return Fail("$function is not a function")
        val description = type.pythonDescription() as PythonCallableTypeDescription

        val result = PythonMethod(
            function,
            type.returnValue.pythonTypeRepresentation(),
            (type.arguments zip description.argumentNames).map {
                PythonArgument(it.second, it.first.pythonTypeRepresentation())
            },
            path.toString(),
            null,
            sourceFileContent.substring(funcDef.body.beginOffset, funcDef.body.endOffset).trimIndent()
        )
        result.type = type
        result.newAst = funcDef.body
        return Success(result)
    }

    override fun run() {
        Cleaner.restart()
        try {
            TemporaryFileManager.setup()

            logger.info("Checking Python requirements...")
            if (!RequirementsUtils.requirementsAreInstalled(pythonPath)) {
                logger.error(
                    "Some of the following Python requirements are missing: " +
                            "${requirements.joinToString()}. Please install them."
                )
                return
            }

            val directoriesForSysPath = setOf(path.parent.toString())
            val configFile = setConfigFile(directoriesForSysPath)

            logger.info("Loading information about types...")

            val (mypyStorage, report) = readMypyAnnotationStorageAndInitialErrors(
                pythonPath,
                path.toString(),
                configFile,
                path.toString()  // TODO: fix this interface
            )
            moduleName = mypyStorage.fileToModule[path.toString()]!!
            println(moduleName)

            println(mypyStorage.types.keys)
            println(mypyStorage.definitions.keys)

            mypyStorage.types[moduleName]!!.forEach { expressionTypeFromMypy ->
                println("----------")
                println(expressionTypeFromMypy.line)
                println("${expressionTypeFromMypy.startOffset} ${expressionTypeFromMypy.endOffset}")
                println(
                    sourceFileContent.subSequence(
                        expressionTypeFromMypy.startOffset.toInt(),
                        expressionTypeFromMypy.endOffset.toInt() + 1
                    )
                )
                println(expressionTypeFromMypy.type.asUtBotType.pythonTypeRepresentation())
            }

            logger.info("Analyzing code...")

            val pythonMethodOpt = getPythonMethod(mypyStorage)
            if (pythonMethodOpt is Fail) {
                logger.error(pythonMethodOpt.message)
            }
            val pythonMethod = (pythonMethodOpt as Success).value

            val typeStorage = PythonTypeStorage.get(mypyStorage)
            val mypyExpressionTypes = mypyStorage.types[moduleName]!!.associate {
                Pair(it.startOffset.toInt(), it.endOffset.toInt() + 1) to it.type.asUtBotType
            }
            val collector = HintCollector(pythonMethod.type, typeStorage, mypyExpressionTypes)
            val visitor = Visitor(listOf(collector))
            visitor.visit(pythonMethod.newAst)

            collector.astNodeToHintCollectorNode.forEach { (ast, hint) ->
                println("${ast.beginLine}:${ast.beginOffset}:${ast.endOffset}: ${hint.partialType.pythonTypeRepresentation()}")
                hint.upperBounds.forEach {
                    if (!typesAreEqual(it, pythonAnyType))
                        println("    ${it.pythonTypeRepresentation()}")
                }
                println()
            }

            val algo = BaselineAlgorithm(
                typeStorage,
                pythonPath,
                pythonMethod,
                directoriesForSysPath,
                moduleName,
                getErrorNumber(
                    report,
                    path.toString(),
                    getOffsetLine(pythonMethod.newAst.beginOffset),
                    getOffsetLine(pythonMethod.newAst.endOffset)
                )
            )

            logger.info("Starting type inference...")

            val start = System.currentTimeMillis()
            algo.run(collector.result) { System.currentTimeMillis() - start > timeout }.forEach {
                println(it.pythonTypeRepresentation())
            }

            logger.info("Finished.")

        } finally {
            Cleaner.doCleaning()
        }
    }

    private fun getOffsetLine(offset: Int): Int {
        return sourceFileContent.take(offset).count { it == '\n' } + 1
    }
}