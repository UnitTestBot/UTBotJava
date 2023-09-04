package org.utbot.python

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.framework.minimization.minimizeExecutions
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.python.framework.api.python.PythonUtExecution
import org.utbot.python.framework.api.python.util.pythonStrClassId
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
import org.utbot.python.newtyping.mypy.MypyInfoBuild
import org.utbot.python.newtyping.mypy.MypyReportLine
import org.utbot.python.newtyping.mypy.getErrorNumber
import org.utbot.python.newtyping.utils.getOffsetLine
import org.utbot.python.newtyping.utils.isRequired
import org.utbot.python.utils.ExecutionWithTimeoutMode
import org.utbot.python.utils.TestGenerationLimitManager
import org.utbot.python.utils.PriorityCartesianProduct
import org.utbot.python.utils.TimeoutMode

private val logger = KotlinLogging.logger {}
private const val RANDOM_TYPE_FREQUENCY = 6
private const val MAX_EMPTY_COVERAGE_TESTS = 5
private const val MAX_SUBSTITUTIONS = 10

class PythonTestCaseGenerator(
    private val withMinimization: Boolean = true,
    private val directoriesForSysPath: Set<String>,
    private val curModule: String,
    private val pythonPath: String,
    private val fileOfMethod: String,
    private val isCancelled: () -> Boolean,
    private val timeoutForRun: Long = 0,
    private val sourceFileContent: String,
    private val mypyStorage: MypyInfoBuild,
    private val mypyReportLine: List<MypyReportLine>
) {

    private val storageForMypyMessages: MutableList<MypyReportLine> = mutableListOf()

    private fun constructCollectors(
        mypyStorage: MypyInfoBuild,
        typeStorage: PythonTypeHintsStorage,
        method: PythonMethod
    ): Pair<HintCollector, ConstantCollector> {

        // initialize definitions first
        mypyStorage.definitions[curModule]!!.values.map { def ->
            def.getUtBotDefinition()
        }

        val mypyExpressionTypes = mypyStorage.exprTypes[curModule]?.let { moduleTypes ->
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

    private fun getCandidates(param: TypeParameter, typeStorage: PythonTypeHintsStorage): List<UtType> {
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

    private fun generateTypesAfterSubstitution(type: UtType, typeStorage: PythonTypeHintsStorage): List<UtType> {
        val params = type.getBoundedParameters()
        return PriorityCartesianProduct(params.map { getCandidates(it, typeStorage) }).getSequence().map { subst ->
            DefaultSubstitutionProvider.substitute(type, (params zip subst).associate { it })
        }.take(MAX_SUBSTITUTIONS).toList()
    }

    private fun substituteTypeParameters(
        method: PythonMethod,
        typeStorage: PythonTypeHintsStorage,
        ): List<PythonMethod> {
        val newClasses = method.containingPythonClass?.let {
            generateTypesAfterSubstitution(it, typeStorage)
        } ?: listOf(null)
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
        }.take(MAX_SUBSTITUTIONS)
    }

    private fun methodHandler(
        method: PythonMethod,
        typeStorage: PythonTypeHintsStorage,
        coveredLines: MutableSet<Int>,
        errors: MutableList<UtError>,
        executions: MutableList<PythonUtExecution>,
        initMissingLines: Set<Int>?,
        until: Long,
        additionalVars: String = "",
    ): Set<Int>? {  // returns missing lines
        val limitManager = TestGenerationLimitManager(
            ExecutionWithTimeoutMode,
            until,
        )
        var missingLines = initMissingLines

        val (hintCollector, constantCollector) = constructCollectors(mypyStorage, typeStorage, method)
        val constants = constantCollector.result
            .mapNotNull { (type, value) ->
                if (type.pythonTypeName() == pythonStrClassId.name && value is String) {
                    // Filter doctests
                    if (value.contains(">>>")) return@mapNotNull null
                }
                logger.debug { "Collected constant: ${type.pythonTypeRepresentation()}: $value" }
                PythonFuzzedConcreteValue(type, value)
            }

        inferAnnotations(
            method,
            mypyStorage,
            typeStorage,
            hintCollector,
            mypyReportLine,
            limitManager,
            additionalVars
        ) { functionType ->
            val args = (functionType as FunctionType).arguments

            logger.debug { "Inferred annotations: ${args.joinToString { it.pythonTypeRepresentation() }}" }

            val engine = PythonEngine(
                method,
                directoriesForSysPath,
                curModule,
                pythonPath,
                constants,
                timeoutForRun,
                PythonTypeHintsStorage.get(mypyStorage)
            )

            var feedback: InferredTypeFeedback = SuccessFeedback

            val fuzzerCancellation = { isCancelled() || limitManager.isCancelled() }

            engine.fuzzing(args, fuzzerCancellation, until).collect {
                when (it) {
                    is ValidExecution -> {
                        executions += it.utFuzzedExecution
                        missingLines = updateMissingLines(it.utFuzzedExecution, coveredLines, missingLines)
                        feedback = SuccessFeedback
                        limitManager.addSuccessExecution()
                    }
                    is InvalidExecution -> {
                        errors += it.utError
                        feedback = InvalidTypeFeedback
                        limitManager.addInvalidExecution()
                    }
                    is ArgumentsTypeErrorFeedback -> {
                        feedback = InvalidTypeFeedback
                        limitManager.addInvalidExecution()
                    }
                    is TypeErrorFeedback -> {
                        feedback = InvalidTypeFeedback
                        limitManager.addInvalidExecution()
                    }
                    is CachedExecutionFeedback -> {
                        when (it.cachedFeedback) {
                            is ValidExecution -> {
                                limitManager.addSuccessExecution()
                            }
                            else -> {
                                limitManager.addInvalidExecution()
                            }
                        }
                    }
                    is FakeNodeFeedback -> {
                       limitManager.addFakeNodeExecutions()
                    }
                }
                limitManager.missedLines = missingLines?.size
            }
            limitManager.restart()
            feedback
        }
        return missingLines
    }

    fun generate(method: PythonMethod, until: Long): PythonTestSet {
        storageForMypyMessages.clear()

        val typeStorage = PythonTypeHintsStorage.get(mypyStorage)

        val executions = mutableListOf<PythonUtExecution>()
        val errors = mutableListOf<UtError>()
        val coveredLines = mutableSetOf<Int>()

        logger.info { "Start test generation for ${method.name}" }
        try {
            val methodModifications = mutableSetOf<Pair<PythonMethod, String>>()  // Set of pairs <PythonMethod, additionalVars>

            substituteTypeParameters(method, typeStorage).forEach { newMethod ->
                createShortForm(newMethod)?.let { methodModifications.add(it) }
                methodModifications.add(newMethod to "")
            }

            val now = System.currentTimeMillis()
            val timeout = (until - now) / methodModifications.size
            var missingLines: Set<Int>? = null
            methodModifications.forEach { (method, additionalVars) ->
                missingLines = methodHandler(
                    method,
                    typeStorage,
                    coveredLines,
                    errors,
                    executions,
                    missingLines,
                    minOf(until, System.currentTimeMillis() + timeout),
                    additionalVars,
                )
            }
        } catch (_: OutOfMemoryError) {
            logger.debug { "Out of memory error. Stop test generation process" }
        }

        logger.info { "Collect all test executions for ${method.name}" }
        val (emptyCoverageExecutions, coverageExecutions) = executions.partition { it.coverage == null }
        val (successfulExecutions, failedExecutions) = coverageExecutions.partition { it.result is UtExecutionSuccess }

        return PythonTestSet(
            method,
            if (withMinimization)
                minimizeExecutions(successfulExecutions) +
                        minimizeExecutions(failedExecutions) +
                        emptyCoverageExecutions.take(MAX_EMPTY_COVERAGE_TESTS)
            else
                coverageExecutions + emptyCoverageExecutions.take(MAX_EMPTY_COVERAGE_TESTS),
            errors,
            storageForMypyMessages
        )
    }

    /**
     * Calculate a new set of missing lines in tested function
     */
    private fun updateMissingLines(
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
        mypyStorage: MypyInfoBuild,
        typeStorage: PythonTypeHintsStorage,
        hintCollector: HintCollector,
        report: List<MypyReportLine>,
        limitManager: TestGenerationLimitManager,
        additionalVars: String,
        annotationHandler: suspend (UtType) -> InferredTypeFeedback,
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
            mypyStorage.buildRoot.configFile,
            additionalVars,
            randomTypeFrequency = RANDOM_TYPE_FREQUENCY,
            dMypyTimeout = timeoutForRun
        )

        runBlocking breaking@{
            if (typeInferenceCancellation()) {
                return@breaking
            }

            val iterationNumber = algo.run(hintCollector.result, typeInferenceCancellation, annotationHandler)

            if (iterationNumber == 1) {  // Initial annotation can't be substituted
                limitManager.mode = TimeoutMode
                val existsAnnotation = method.definition.type
                annotationHandler(existsAnnotation)
            }
        }
    }

    companion object {
        fun createShortForm(method: PythonMethod): Pair<PythonMethod, String>? {
            val meta = method.definition.type.pythonDescription() as PythonCallableTypeDescription
            val argKinds = meta.argumentKinds
            if (argKinds.any { !isRequired(it) }) {
                val originalDef = method.definition
                val shortType = meta.removeNotRequiredArgs(originalDef.type)
                val shortMeta = PythonFuncItemDescription(
                    originalDef.meta.name,
                    originalDef.meta.args.filterIndexed { index, _ -> isRequired(argKinds[index]) }
                )
                val additionalVars = originalDef.meta.args
                    .filterIndexed { index, _ -> !isRequired(argKinds[index]) }
                    .mapIndexed { index, arg ->
                        "${arg.name}: ${method.argumentsWithoutSelf[index].annotation ?: pythonAnyType.pythonTypeRepresentation()}"
                    }
                    .joinToString(separator = "\n", prefix = "\n")
                val shortDef = PythonFunctionDefinition(shortMeta, shortType)
                val shortMethod = PythonMethod(
                    method.name,
                    method.moduleFilename,
                    method.containingPythonClass,
                    method.codeAsString,
                    shortDef,
                    method.ast
                )
                return Pair(shortMethod, additionalVars)
            }
            return null
        }
    }
}
