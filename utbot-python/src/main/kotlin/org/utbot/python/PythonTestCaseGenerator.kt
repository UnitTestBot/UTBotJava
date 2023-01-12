package org.utbot.python

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.parsers.python.PythonParser
import org.utbot.framework.minimization.minimizeExecutions
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.python.code.PythonCode
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.constants.ConstantCollector
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.getPythonAttributes
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.mypy.MypyAnnotationStorage
import org.utbot.python.newtyping.mypy.MypyReportLine
import org.utbot.python.newtyping.mypy.getErrorNumber
import org.utbot.python.newtyping.mypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.mypy.setConfigFile
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.newtyping.utils.getOffsetLine
import org.utbot.python.typing.MypyAnnotations
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

object PythonTestCaseGenerator {
    private var withMinimization: Boolean = true
    private var pythonRunRoot: Path? = null
    private lateinit var directoriesForSysPath: Set<String>
    private lateinit var curModule: String
    private lateinit var pythonPath: String
    private lateinit var fileOfMethod: String
    private lateinit var isCancelled: () -> Boolean
    private var timeoutForRun: Long = 0
    private var until: Long = 0
    private lateinit var sourceFileContent: String

    private const val COVERAGE_LIMIT = 20

    fun init(
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        pythonPath: String,
        fileOfMethod: String,
        timeoutForRun: Long,
        withMinimization: Boolean = true,
        pythonRunRoot: Path? = null,
        until: Long,
        isCancelled: () -> Boolean
    ) {
        this.directoriesForSysPath = directoriesForSysPath
        this.curModule = moduleToImport
        this.pythonPath = pythonPath
        this.fileOfMethod = fileOfMethod
        this.withMinimization = withMinimization
        this.isCancelled = isCancelled
        this.timeoutForRun = timeoutForRun
        this.pythonRunRoot = pythonRunRoot
        this.until = until
        this.sourceFileContent = File(fileOfMethod).readText()
    }

    private val storageForMypyMessages: MutableList<MypyAnnotations.MypyReportLine> = mutableListOf()

    private fun findFunctionDefinition(mypyStorage: MypyAnnotationStorage, method: PythonMethod) {
        val containingClass = method.containingPythonClassId
        val functionDef = if (containingClass == null) {
            mypyStorage.definitions[curModule]!![method.name]!!.annotation.asUtBotType
        } else {
            mypyStorage.definitions[curModule]!![containingClass.simpleName]!!.annotation.asUtBotType.getPythonAttributes().first {
                it.name == method.name
            }.type
        } as FunctionType

        val parsedFile = PythonParser(sourceFileContent).Module()
        val funcDef = PythonCode.findFunctionDefinition(parsedFile, method)

        method.returnAnnotation = functionDef.returnValue.pythonTypeRepresentation()
        method.arguments = (method.arguments zip functionDef.arguments).map { PythonArgument(it.first.name, it.second.pythonTypeRepresentation()) }
        method.type = functionDef
        method.newAst = funcDef.body
        method.codeAsString = funcDef.body.source
    }

    private fun constructCollectors(mypyStorage: MypyAnnotationStorage, typeStorage: PythonTypeStorage, method: PythonMethod): Pair<HintCollector, ConstantCollector> {
        findFunctionDefinition(mypyStorage, method)

        val mypyExpressionTypes = mypyStorage.types[curModule]?.let { moduleTypes ->
            moduleTypes.associate {
                Pair(it.startOffset.toInt(), it.endOffset.toInt() + 1) to it.type.asUtBotType
            }
        } ?: emptyMap()

        val hintCollector = HintCollector(method.type, typeStorage, mypyExpressionTypes)
        val constantCollector = ConstantCollector(typeStorage)
        val visitor = Visitor(listOf(hintCollector, constantCollector))
        visitor.visit(method.newAst)
        return Pair(hintCollector, constantCollector)
    }

    fun generate(method: PythonMethod): PythonTestSet {
        val mypyConfigFile = setConfigFile(directoriesForSysPath)
        val (mypyStorage, report) = readMypyAnnotationStorageAndInitialErrors(
            pythonPath,
            method.moduleFilename,
            curModule,
            mypyConfigFile
        )
        storageForMypyMessages.clear()

        val typeStorage = PythonTypeStorage.get(mypyStorage)

        val (hintCollector, constantCollector) = constructCollectors(mypyStorage, typeStorage, method)
        val constants = constantCollector.result.map { (type, value) ->
            PythonFuzzedConcreteValue(type, value)
        }

        val executions = mutableListOf<UtExecution>()
        val errors = mutableListOf<UtError>()
        var missingLines: Set<Int>? = null
        val coveredLines = mutableSetOf<Int>()
        var generated = 0
        val typeInferenceCancellation = { isCancelled() || System.currentTimeMillis() >= until || missingLines?.size == 0 }

        val annotations = getAnnotations(
            method,
            mypyStorage,
            typeStorage,
            hintCollector,
            report,
            mypyConfigFile,
            typeInferenceCancellation
        )

        annotations.forEach { functionType ->
            val args = (functionType as FunctionType).arguments

            logger.info {
                "Inferred annotations: ${
                    args.joinToString { it.pythonTypeRepresentation() }
                }"
            }

            val engine = PythonEngine(
                method,
                directoriesForSysPath,
                curModule,
                pythonPath,
                constants,
                timeoutForRun,
                coveredLines,
                PythonTypeStorage.get(mypyStorage)
            )

            var coverageLimit = COVERAGE_LIMIT
            var coveredBefore = coveredLines.size

            val fuzzerCancellation = { typeInferenceCancellation() || coverageLimit == 0 }

            runBlocking {
                engine.fuzzing(args, fuzzerCancellation, until).collect {
                    generated += 1
                    when (it) {
                        is UtExecution -> {
                            logger.debug("Added execution: $it")
                            executions += it
                            missingLines = updateCoverage(it, coveredLines, missingLines)
                        }

                        is UtError -> {
                            logger.debug("Failed evaluation. Reason: ${it.description}")
                            errors += it
                        }
                    }
                    val coveredAfter = coveredLines.size
                    if (coveredAfter == coveredBefore) {
                        coverageLimit -= 1
                    }
                    coveredBefore = coveredAfter
                }
            }
        }

        val (successfulExecutions, failedExecutions) = executions.partition { it.result is UtExecutionSuccess }

        return PythonTestSet(
            method,
            if (withMinimization)
                minimizeExecutions(successfulExecutions) + minimizeExecutions(failedExecutions)
            else
                executions,
            errors,
            storageForMypyMessages
        )
    }

    // returns new missingLines
    private fun updateCoverage(
        execution: UtExecution,
        coveredLines: MutableSet<Int>,
        missingLines: Set<Int>?
    ): Set<Int> {
        execution.coverage?.coveredInstructions?.map { instr -> coveredLines.add(instr.lineNumber) }
        val curMissing =
            execution.coverage
                ?.missedInstructions
                ?.map { x -> x.lineNumber }?.toSet()
                ?: emptySet()
        return if (missingLines == null) curMissing else missingLines intersect curMissing
    }

    private fun getAnnotations(
        method: PythonMethod,
        mypyStorage: MypyAnnotationStorage,
        typeStorage: PythonTypeStorage,
        hintCollector: HintCollector,
        report: List<MypyReportLine>,
        mypyConfigFile: File,
        isCancelled: () -> Boolean
    ): Sequence<Type> {
        val namesInModule = mypyStorage.names.getOrDefault(curModule, emptyMap()).keys.filter {
            it.length < 4 || !it.startsWith("__") || !it.endsWith("__")
        }

        val algo = BaselineAlgorithm(
            typeStorage,
            pythonPath,
            method,
            directoriesForSysPath,
            curModule,
            namesInModule,
            getErrorNumber(
                report,
                fileOfMethod,
                getOffsetLine(sourceFileContent, method.newAst.beginOffset),
                getOffsetLine(sourceFileContent, method.newAst.endOffset)
            ),
            mypyConfigFile
        )

        var annotations = emptyList<Type>().asSequence()
        val existsAnnotation = method.type
        if (existsAnnotation.arguments.all {it.pythonTypeName() != "typing.Any"}) {
            annotations += listOf(method.type).asSequence()
        }
        annotations += algo.run(hintCollector.result, isCancelled)

        return annotations
    }
}