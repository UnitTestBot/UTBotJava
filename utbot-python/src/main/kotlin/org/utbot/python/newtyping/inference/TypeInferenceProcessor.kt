package org.utbot.python.newtyping.inference

import kotlinx.coroutines.runBlocking
import org.parsers.python.PythonParser
import org.parsers.python.ast.ClassDefinition
import org.parsers.python.ast.FunctionDefinition
import org.utbot.python.PythonMethod
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.parseClassDefinition
import org.utbot.python.newtyping.ast.parseFunctionDefinition
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.CompositeType
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
    private val functionName: String,
    private val className: String? = null
) {

    private val path: Path = Paths.get(File(sourceFile).canonicalPath)
    private val sourceFileContent = File(sourceFile).readText()
    private val parsedFile = PythonParser(sourceFileContent).Module()

    fun inferTypes(
        cancel: () -> Boolean,
        processSignature: (Type) -> Unit = {},
        checkRequirementsAction: () -> Unit = {},
        missingRequirementsAction: () -> Unit = {},
        loadingInfoAboutTypesAction: () -> Unit = {},
        analyzingCodeAction: () -> Unit = {},
        pythonMethodExtractionFailAction: (String) -> Unit = {},
        startingTypeInferenceAction: () -> Unit = {}
    ): List<Type> {
        Cleaner.restart()
        try {
            TemporaryFileManager.setup()

            checkRequirementsAction()

            if (!RequirementsUtils.requirementsAreInstalled(pythonPath)) {
                missingRequirementsAction()
                return emptyList()
            }

            val configFile = setConfigFile(directoriesForSysPath)

            loadingInfoAboutTypesAction()

            val (mypyStorage, report) = readMypyAnnotationStorageAndInitialErrors(
                pythonPath,
                path.toString(),
                moduleOfSourceFile,
                configFile
            )

            val namesInModule = mypyStorage.names[moduleOfSourceFile]!!.map { it.name }.filter {
                it.length < 4 || !it.startsWith("__") || !it.endsWith("__")
            }

            analyzingCodeAction()

            val typeStorage = PythonTypeStorage.get(mypyStorage)
            val pythonMethodOpt = getPythonMethod(mypyStorage, typeStorage)
            if (pythonMethodOpt is Fail) {
                pythonMethodExtractionFailAction(pythonMethodOpt.message)
                return emptyList()
            }

            val pythonMethod = (pythonMethodOpt as Success).value

            val mypyExpressionTypes = mypyStorage.types[moduleOfSourceFile]!!.associate {
                Pair(it.startOffset.toInt(), it.endOffset.toInt() + 1) to it.type.asUtBotType
            }
            val namesStorage = GlobalNamesStorage(mypyStorage)
            val collector =
                HintCollector(pythonMethod.definition, typeStorage, mypyExpressionTypes, namesStorage, moduleOfSourceFile)
            val visitor = Visitor(listOf(collector))
            visitor.visit(pythonMethod.ast)

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
                    getOffsetLine(sourceFileContent, pythonMethod.ast.beginOffset),
                    getOffsetLine(sourceFileContent, pythonMethod.ast.endOffset)
                ),
                configFile
            )

            startingTypeInferenceAction()
            val annotations = emptyList<Type>().toMutableList()
            runBlocking {
                algo.run(collector.result, cancel) {
                    annotations.add(it)
                    processSignature(it)
                    SuccessFeedback
                }
            }
            return annotations
        } finally {
            Cleaner.doCleaning()
        }
    }

    private fun getPythonMethod(mypyAnnotationStorage: MypyAnnotationStorage, typeStorage: PythonTypeStorage): Optional<PythonMethod> {
        if (className == null) {
            val funcDef = parsedFile.children().firstNotNullOfOrNull { node ->
                val res = (node as? FunctionDefinition)?.let { parseFunctionDefinition(it) }
                if (res?.name?.toString() == functionName) res else null
            } ?: return Fail("Couldn't find top-level function $functionName")

            val def =
                mypyAnnotationStorage.definitions[moduleOfSourceFile]!![functionName]!!.getUtBotDefinition() as? PythonFunctionDefinition
                    ?: return Fail("$functionName is not a function")

            val result = PythonMethod(
                functionName,
                path.toString(),
                null,
                sourceFileContent.substring(funcDef.body.beginOffset, funcDef.body.endOffset).trimIndent(),
                def,
                funcDef.body
            )
            return Success(result)
        }
        val classDef = parsedFile.children().firstNotNullOfOrNull { node ->
            val res = (node as? ClassDefinition)?.let { parseClassDefinition(it) }
            if (res?.name?.toString() == className) res else null
        } ?: return Fail("Couldn't find top-level class $className")
        val funcDef = classDef.body.children().firstNotNullOfOrNull { node ->
            val res = (node as? FunctionDefinition)?.let { parseFunctionDefinition(it) }
            if (res?.name?.toString() == functionName) res else null
        } ?: return Fail("Couldn't find method $functionName in class $className")

        val typeOfClass = mypyAnnotationStorage.definitions[moduleOfSourceFile]!![className]!!.getUtBotType()
            as? CompositeType ?: return Fail("$className is not a class")

        val defOfFunc = typeOfClass.getPythonAttributeByName(typeStorage, functionName) as? PythonFunctionDefinition
            ?: return Fail("$functionName is not a function")

        println(defOfFunc.type.pythonTypeRepresentation())

        val result = PythonMethod(
            functionName,
            path.toString(),
            PythonClassId("$moduleOfSourceFile.$className"),
            sourceFileContent.substring(funcDef.body.beginOffset, funcDef.body.endOffset).trimIndent(),
            defOfFunc,
            funcDef.body
        )
        return Success(result)
    }
}