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
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.constants.ConstantCollector
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.InferredTypeFeedback
import org.utbot.python.newtyping.inference.InvalidTypeFeedback
import org.utbot.python.newtyping.inference.SuccessFeedback
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.mypy.GlobalNamesStorage
import org.utbot.python.newtyping.mypy.MypyAnnotationStorage
import org.utbot.python.newtyping.mypy.MypyReportLine
import org.utbot.python.newtyping.mypy.getErrorNumber
import org.utbot.python.newtyping.utils.getOffsetLine
import org.utbot.python.typing.MypyAnnotations
import java.io.File

private val logger = KotlinLogging.logger {}

private const val COVERAGE_LIMIT = 20

class PythonTestCaseGenerator(
    private val withMinimization: Boolean = true,
    private val directoriesForSysPath: Set<String>,
    private val curModule: String,
    private val pythonPath: String,
    private val fileOfMethod: String,
    private val isCancelled: () -> Boolean,
    private val timeoutForRun: Long = 0,
    private val until: Long = 0,
    private val sourceFileContent: String,
    private val mypyStorage: MypyAnnotationStorage,
    private val mypyReportLine: List<MypyReportLine>,
    private val mypyConfigFile: File,
){

    private val storageForMypyMessages: MutableList<MypyAnnotations.MypyReportLine> = mutableListOf()

    private fun findMethodByDescription(mypyStorage: MypyAnnotationStorage, method: PythonMethodHeader): PythonMethod {
        val containingClass = method.containingPythonClassId
        val functionDef = if (containingClass == null) {
            mypyStorage.definitions[curModule]!![method.name]!!.getUtBotDefinition()!!
        } else {
            mypyStorage.definitions[curModule]!![containingClass.simpleName]!!.type.asUtBotType.getPythonAttributes().first {
                it.meta.name == method.name
            }
        } as? PythonFunctionDefinition ?: error("Selected method is not a function definition")

        val parsedFile = PythonParser(sourceFileContent).Module()
        val funcDef = PythonCode.findFunctionDefinition(parsedFile, method)

        return PythonMethod(
            name = method.name,
            moduleFilename = method.moduleFilename,
            containingPythonClassId = method.containingPythonClassId,
            codeAsString = funcDef.body.source,
            definition = functionDef,
            ast = funcDef.body
        )
    }

    private fun constructCollectors(
        mypyStorage: MypyAnnotationStorage,
        typeStorage: PythonTypeStorage,
        method: PythonMethod
    ): Pair<HintCollector, ConstantCollector> {

        val mypyExpressionTypes = mypyStorage.types[curModule]?.let { moduleTypes ->
            moduleTypes.associate {
                Pair(it.startOffset.toInt(), it.endOffset.toInt() + 1) to it.type.asUtBotType
            }
        } ?: emptyMap()

        val namesStorage = GlobalNamesStorage(mypyStorage)
        val hintCollector = HintCollector(method.definition, typeStorage, mypyExpressionTypes , namesStorage, curModule)
        val constantCollector = ConstantCollector(typeStorage)
        val visitor = Visitor(listOf(hintCollector, constantCollector))
        visitor.visit(method.ast)
        return Pair(hintCollector, constantCollector)
    }

    fun generate(methodDescription: PythonMethodHeader): PythonTestSet {
        storageForMypyMessages.clear()

        val typeStorage = PythonTypeStorage.get(mypyStorage)
        val method = findMethodByDescription(mypyStorage, methodDescription)

        val (hintCollector, constantCollector) = constructCollectors(mypyStorage, typeStorage, method)
        val constants = constantCollector.result.map { (type, value) ->
            logger.debug("Collected constant: ${type.pythonTypeRepresentation()}: $value")
            PythonFuzzedConcreteValue(type, value)
        }

        val executions = mutableListOf<UtExecution>()
        val errors = mutableListOf<UtError>()
        var missingLines: Set<Int>? = null
        val coveredLines = mutableSetOf<Int>()
        var generated = 0
        val typeInferenceCancellation = { isCancelled() || System.currentTimeMillis() >= until || missingLines?.size == 0 }

        inferAnnotations(
            method,
            mypyStorage,
            typeStorage,
            hintCollector,
            mypyReportLine,
            mypyConfigFile,
            typeInferenceCancellation
        ) { functionType ->
            val args = (functionType as FunctionType).arguments

            logger.info { "Inferred annotations: ${ args.joinToString { it.pythonTypeRepresentation() } }" }

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

            var feedback: InferredTypeFeedback = SuccessFeedback

            val fuzzerCancellation = { typeInferenceCancellation() || coverageLimit == 0 } // || feedback is InvalidTypeFeedback }

            engine.fuzzing(args, fuzzerCancellation, until).collect {
                generated += 1
                when (it) {
                    is ValidExecution -> {
                        executions += it.utFuzzedExecution
                        missingLines = updateCoverage(it.utFuzzedExecution, coveredLines, missingLines)
                        feedback = SuccessFeedback
                    }
                    is InvalidExecution -> {
                        errors += it.utError
                        feedback = SuccessFeedback
                    }
                    is ArgumentsTypeErrorFeedback -> {
                        feedback = InvalidTypeFeedback
                    }
                    is TypeErrorFeedback -> {
                        feedback = InvalidTypeFeedback
                    }
                }
                val coveredAfter = coveredLines.size
                if (coveredAfter == coveredBefore) {
                    coverageLimit -= 1
                }
                coveredBefore = coveredAfter
            }
            feedback
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

    private fun inferAnnotations(
        method: PythonMethod,
        mypyStorage: MypyAnnotationStorage,
        typeStorage: PythonTypeStorage,
        hintCollector: HintCollector,
        report: List<MypyReportLine>,
        mypyConfigFile: File,
        isCancelled: () -> Boolean,
        annotationHandler: suspend (Type) -> InferredTypeFeedback,
    ) {
        val namesInModule = mypyStorage.names
            .getOrDefault(curModule, emptyList())
            .map { it.name }
            .filter {
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
                getOffsetLine(sourceFileContent, method.ast.beginOffset),
                getOffsetLine(sourceFileContent, method.ast.endOffset)
            ),
            mypyConfigFile
        )

        runBlocking breaking@ {
            if (isCancelled()) {
                return@breaking
            }

            val existsAnnotation = method.definition.type
            if (existsAnnotation.arguments.all {it.pythonTypeName() != "typing.Any"}) {
                annotationHandler(existsAnnotation)
            }

            algo.run(hintCollector.result, isCancelled, annotationHandler)
        }
    }
}