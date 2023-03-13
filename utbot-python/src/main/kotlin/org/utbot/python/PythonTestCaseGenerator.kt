package org.utbot.python

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.framework.minimization.minimizeExecutions
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.python.fuzzing.*
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.constants.ConstantCollector
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.*
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
import org.utbot.python.utils.ExecutionWithTimeoutMode
import org.utbot.python.utils.TestGenerationLimitManager
import org.utbot.python.utils.PriorityCartesianProduct
import org.utbot.python.utils.TimeoutMode
import java.io.File

private val logger = KotlinLogging.logger {}
private const val RANDOM_TYPE_FREQUENCY = 6
private const val MAX_EXECUTIONS = 50000

class PythonTestCaseGenerator(
    private val withMinimization: Boolean = true,
    private val directoriesForSysPath: Set<String>,
    private val curModule: String,
    private val pythonPath: String,
    private val fileOfMethod: String,
    private val isCancelled: () -> Boolean,
    private val timeoutForRun: Long = 0,
    private val sourceFileContent: String,
    private val mypyStorage: MypyAnnotationStorage,
    private val mypyReportLine: List<MypyReportLine>,
    private val mypyConfigFile: File,
) {

    private val storageForMypyMessages: MutableList<MypyAnnotations.MypyReportLine> = mutableListOf()

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
        val hintCollector = HintCollector(method.definition, typeStorage, mypyExpressionTypes, namesStorage, curModule)
        val constantCollector = ConstantCollector(typeStorage)
        val visitor = Visitor(listOf(hintCollector, constantCollector))
        visitor.visit(method.ast)
        return Pair(hintCollector, constantCollector)
    }

    private fun getCandidates(param: TypeParameter, typeStorage: PythonTypeStorage): List<Type> {
        val meta = param.pythonDescription() as PythonTypeVarDescription
        return when (meta.parameterKind) {
            PythonTypeVarDescription.ParameterKind.WithConcreteValues -> {
                param.constraints.map { it.boundary }
            }
            PythonTypeVarDescription.ParameterKind.WithUpperBound -> {
                typeStorage.simpleTypes.filter {
                    if (it.hasBoundedParameters())
                        return@filter false
                    val bound = param.constraints.first().boundary
                    PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(bound, it, typeStorage)
                }
            }
        }
    }

    private val maxSubstitutions = 10

    private fun generateTypesAfterSubstitution(type: Type, typeStorage: PythonTypeStorage): List<Type> {
        val params = type.getBoundedParameters()
        return PriorityCartesianProduct(params.map { getCandidates(it, typeStorage) }).getSequence().map { subst ->
            DefaultSubstitutionProvider.substitute(type, (params zip subst).associate { it })
        }.take(maxSubstitutions).toList()
    }

    private fun substituteTypeParameters(method: PythonMethod, typeStorage: PythonTypeStorage): List<PythonMethod> {
        val newClasses =
            if (method.containingPythonClass != null) {
                generateTypesAfterSubstitution(method.containingPythonClass, typeStorage)
            } else {
                listOf(null)
            }
        return newClasses.flatMap { newClass ->
            val funcType = newClass?.getPythonAttributeByName(typeStorage, method.name)?.type as? FunctionType
                ?: method.definition.type
            val newFuncTypes = generateTypesAfterSubstitution(funcType, typeStorage)
            newFuncTypes.map { newFuncType ->
                val def = PythonFunctionDefinition(method.definition.meta, newFuncType as FunctionType)
                PythonMethod(
                    method.name,
                    method.moduleFilename,
                    newClass as? CompositeType,
                    method.codeAsString,
                    def,
                    method.ast
                )
            }
        }.take(maxSubstitutions)
    }

    private fun methodHandler(
        method: PythonMethod,
        typeStorage: PythonTypeStorage,
        coveredLines: MutableSet<Int>,
        errors: MutableList<UtError>,
        executions: MutableList<UtExecution>,
        initMissingLines: Set<Int>?,
        until: Long,
        additionalVars: String = ""
    ): Set<Int>? {  // returns missing lines
        val limitManager = TestGenerationLimitManager(
            ExecutionWithTimeoutMode,
            until,
        )
        var missingLines = initMissingLines

        val (hintCollector, constantCollector) = constructCollectors(mypyStorage, typeStorage, method)
        val constants = constantCollector.result.map { (type, value) ->
            logger.debug("Collected constant: ${type.pythonTypeRepresentation()}: $value")
            PythonFuzzedConcreteValue(type, value)
        }

        substituteTypeParameters(method, typeStorage).forEach { newMethod ->
            inferAnnotations(
                newMethod,
                mypyStorage,
                typeStorage,
                hintCollector,
                mypyReportLine,
                mypyConfigFile,
                limitManager,
                additionalVars
            ) { functionType ->
                val args = (functionType as FunctionType).arguments

                logger.info { "Inferred annotations: ${args.joinToString { it.pythonTypeRepresentation() }}" }

                val engine = PythonEngine(
                    newMethod,
                    directoriesForSysPath,
                    curModule,
                    pythonPath,
                    constants,
                    timeoutForRun,
                    PythonTypeStorage.get(mypyStorage)
                )

                var feedback: InferredTypeFeedback = SuccessFeedback

                val fuzzerCancellation = {
                    isCancelled() || limitManager.isCancelled() || (errors.size + executions.size) >= MAX_EXECUTIONS
                }

                engine.fuzzing(args, fuzzerCancellation, until).collect {
                    when (it) {
                        is ValidExecution -> {
                            executions += it.utFuzzedExecution
                            missingLines = updateCoverage(it.utFuzzedExecution, coveredLines, missingLines)
                            feedback = SuccessFeedback
                            limitManager.addSuccessExecution()
                        }
                        is InvalidExecution -> {
                            errors += it.utError
                            feedback = SuccessFeedback
                            limitManager.addSuccessExecution()
                        }
                        is ArgumentsTypeErrorFeedback -> {
                            feedback = InvalidTypeFeedback
                            limitManager.addInvalidExecution()
                        }
                        is TypeErrorFeedback -> {
                            feedback = InvalidTypeFeedback
                            limitManager.addInvalidExecution()
                        }
                    }
                    limitManager.missedLines = missingLines?.size
                }
                limitManager.restart()
                feedback
            }
        }
        return missingLines
    }

    fun generate(method: PythonMethod, until: Long): PythonTestSet {
        storageForMypyMessages.clear()

        val typeStorage = PythonTypeStorage.get(mypyStorage)

        val executions = mutableListOf<UtExecution>()
        val errors = mutableListOf<UtError>()
        val coveredLines = mutableSetOf<Int>()

        logger.info("Start test generation for ${method.name}")
        val meta = method.definition.type.pythonDescription() as PythonCallableTypeDescription
        val argKinds = meta.argumentKinds
        if (argKinds.any { it != PythonCallableTypeDescription.ArgKind.ARG_POS }) {
            val now = System.currentTimeMillis()
            val firstUntil = (until - now) / 2 + now
            val originalDef = method.definition
            val shortType = meta.removeNonPositionalArgs(originalDef.type)
            val shortMeta = PythonFuncItemDescription(
                originalDef.meta.name,
                originalDef.meta.args.take(shortType.arguments.size)
            )
            val additionalVars = originalDef.meta.args
                .drop(shortType.arguments.size)
                .joinToString(separator="\n", prefix="\n") { arg ->
                    "${arg.name}: ${pythonAnyType.pythonTypeRepresentation()}"  // TODO: better types
                }
            method.definition = PythonFunctionDefinition(shortMeta, shortType)
            val missingLines = methodHandler(method, typeStorage, coveredLines, errors, executions, null, firstUntil, additionalVars)
            method.definition = originalDef
            methodHandler(method, typeStorage, coveredLines, errors, executions, missingLines, until)
        } else {
            methodHandler(method, typeStorage, coveredLines, errors, executions, null, until)
        }

        logger.info("Collect all test executions for ${method.name}")
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
        limitManager: TestGenerationLimitManager,
        additionalVars: String,
        annotationHandler: suspend (Type) -> InferredTypeFeedback,
    ) {
        val namesInModule = mypyStorage.names
            .getOrDefault(curModule, emptyList())
            .map { it.name }
            .filter {
                it.length < 4 || !it.startsWith("__") || !it.endsWith("__")
            }
        val typeInferenceCancellation = { isCancelled() || limitManager.isCancelled() }

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
            mypyConfigFile,
            additionalVars,
            randomTypeFrequency = RANDOM_TYPE_FREQUENCY
        )

        runBlocking breaking@{
            if (typeInferenceCancellation()) {
                return@breaking
            }

            val iterationNumber = algo.run(hintCollector.result, typeInferenceCancellation, annotationHandler)

            if (iterationNumber == 1) {
                limitManager.mode = TimeoutMode
                val existsAnnotation = method.definition.type
                annotationHandler(existsAnnotation)
            }
        }
    }
}
