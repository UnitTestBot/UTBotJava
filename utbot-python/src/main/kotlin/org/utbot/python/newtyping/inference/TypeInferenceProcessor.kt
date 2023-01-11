package org.utbot.python.newtyping.inference

import org.parsers.python.PythonParser
import org.parsers.python.ast.FunctionDefinition
import org.utbot.python.PythonArgument
import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.parseFunctionDefinition
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.mypy.*
import org.utbot.python.utils.*
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class TypeInferenceProcessor(
    private val pythonPath: String,
    private val directoriesForSysPath: Set<String>,
    sourceFile: String,
    private val moduleOfSourceFile: String,
    private val functionName: String
) {

    private val path: Path = Paths.get(File(sourceFile).canonicalPath)
    private val sourceFileContent = File(sourceFile).readText()
    private val parsedFile = PythonParser(sourceFileContent).Module()

    fun inferTypes(
        cancel: () -> Boolean,
        checkRequirementsAction: () -> Unit = {},
        missingRequirementsAction: () -> Unit = {},
        loadingInfoAboutTypesAction: () -> Unit = {},
        analyzingCodeAction: () -> Unit = {},
        pythonMethodExtractionFailAction: (String) -> Unit = {},
        startingTypeInferenceAction: () -> Unit = {}
    ): Sequence<Type> = sequence {
        Cleaner.restart()
        try {
            TemporaryFileManager.setup()

            checkRequirementsAction()

            if (!RequirementsUtils.requirementsAreInstalled(pythonPath)) {
                missingRequirementsAction()
                return@sequence
            }

            val configFile = setConfigFile(directoriesForSysPath)

            loadingInfoAboutTypesAction()

            val (mypyStorage, report) = readMypyAnnotationStorageAndInitialErrors(
                pythonPath,
                path.toString(),
                moduleOfSourceFile,
                configFile,
                path.toString()  // TODO: fix this interface
            )

            val namesInModule = mypyStorage.names[moduleOfSourceFile]!!.map { it.name }.filter {
                it.length < 4 || !it.startsWith("__") || !it.endsWith("__")
            }

            // moduleName = mypyStorage.fileToModule[path.toString()]!!

            analyzingCodeAction()

            val pythonMethodOpt = getPythonMethod(mypyStorage)
            if (pythonMethodOpt is Fail) {
                pythonMethodExtractionFailAction(pythonMethodOpt.message)
                return@sequence
            }

            val pythonMethod = (pythonMethodOpt as Success).value

            val typeStorage = PythonTypeStorage.get(mypyStorage)
            val mypyExpressionTypes = mypyStorage.types[moduleOfSourceFile]!!.associate {
                Pair(it.startOffset.toInt(), it.endOffset.toInt() + 1) to it.type.asUtBotType
            }
            val namesStorage = GlobalNamesStorage(mypyStorage)
            val collector =
                HintCollector(pythonMethod.type, typeStorage, mypyExpressionTypes, namesStorage, moduleOfSourceFile)
            val visitor = Visitor(listOf(collector))
            visitor.visit(pythonMethod.newAst)

            val algo = BaselineAlgorithm(
                typeStorage,
                pythonPath,
                pythonMethod,
                directoriesForSysPath,
                moduleOfSourceFile,
                namesInModule,
                getErrorNumber(
                    report,
                    path.toString(),
                    getOffsetLine(sourceFileContent, pythonMethod.newAst.beginOffset),
                    getOffsetLine(sourceFileContent, pythonMethod.newAst.endOffset)
                ),
                configFile
            )

            startingTypeInferenceAction()
            yieldAll(algo.run(collector.result, cancel))
        } finally {
            Cleaner.doCleaning()
        }
    }

    private fun getPythonMethod(mypyAnnotationStorage: MypyAnnotationStorage): Optional<PythonMethod> {
        val funcDef = parsedFile.children().asSequence().mapNotNull { node ->
            val res = (node as? FunctionDefinition)?.let { parseFunctionDefinition(it) }
            if (res?.name?.toString() == functionName) res else null
        }.firstOrNull() ?: return Fail("Couldn't find top-level function $functionName")

        val type =
            mypyAnnotationStorage.definitions[moduleOfSourceFile]!![functionName]!!.annotation.asUtBotType as? FunctionType
                ?: return Fail("$functionName is not a function")
        val description = type.pythonDescription() as PythonCallableTypeDescription

        val result = PythonMethod(
            functionName,
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
}