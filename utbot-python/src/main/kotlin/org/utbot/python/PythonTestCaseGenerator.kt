package org.utbot.python

import kotlinx.coroutines.flow.takeWhile
import mu.KotlinLogging
import org.utbot.common.runBlockingWithCancellationPredicate
import org.utbot.framework.minimization.minimizeExecutions
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.PythonTypeDescription
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.getPythonAttributes
import org.utbot.python.newtyping.runmypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.runmypy.setConfigFile
import org.utbot.python.typing.AnnotationFinder.findAnnotations
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.utils.AnnotationNormalizer.annotationFromProjectToClassId
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
    }

    private val storageForMypyMessages: MutableList<MypyAnnotations.MypyReportLine> = mutableListOf()

    fun generate(method: PythonMethod): PythonTestSet {
        return if (method.arguments.any {it.annotation == null}) {
            oldGenerate(method)
        } else {
            newGenerate(method)
        }
    }

    private fun newGenerate(method: PythonMethod): PythonTestSet {
        val mypyConfigFile = setConfigFile(directoriesForSysPath)
        val (mypyStorage, _) = readMypyAnnotationStorageAndInitialErrors(
            pythonPath,
            method.moduleFilename,
            mypyConfigFile
        )

        val containingClass = method.containingPythonClassId
        val functionDef = if (containingClass == null) {
            mypyStorage.definitions[curModule]!![method.name]!!.annotation.asUtBotType
        } else {
            mypyStorage.definitions[curModule]!![containingClass.simpleName]!!.annotation.asUtBotType.getPythonAttributes().first {
                it.name == method.name
            }.type
        }
        val args = (functionDef as FunctionType).arguments

        storageForMypyMessages.clear()

        val executions = mutableListOf<UtExecution>()
        val errors = mutableListOf<UtError>()
        var missingLines: Set<Int>? = null
        val coveredLines = mutableSetOf<Int>()
        var generated = 0

        logger.debug(
            "Existing annotations: ${
                args.joinToString(" ") { "${it.meta}" }
            }"
        )

        val engine = PythonEngine(
            method,
            directoriesForSysPath,
            curModule,
            pythonPath,
            emptyList(),
            method.arguments.zip(args).associate { it.first.name to NormalizedPythonAnnotation((it.second.meta as PythonTypeDescription).name.toString()) },
            timeoutForRun,
            coveredLines,
            PythonTypeStorage.get(storage)
        )

        runBlockingWithCancellationPredicate(isCancelled) {
            engine.newFuzzing(args, isCancelled, until).collect {
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

    private fun oldGenerate(method: PythonMethod): PythonTestSet {
        storageForMypyMessages.clear()

        val initialArgumentTypes = method.arguments.map {
            annotationFromProjectToClassId(
                it.annotation,
                pythonPath,
                curModule,
                fileOfMethod,
                directoriesForSysPath
            )
        }.toMutableList()

        // TODO: consider static and class methods
        if (method.containingPythonClassId != null) {
            initialArgumentTypes[0] = NormalizedPythonAnnotation(method.containingPythonClassId.name)
        }

        logger.debug("Collecting hints about arguments")
        val argInfoCollector = ArgInfoCollector(method, initialArgumentTypes)
        logger.debug("Collected.")
        val annotationSequence = getAnnotations(method, initialArgumentTypes, argInfoCollector, isCancelled)

        val executions = mutableListOf<UtExecution>()
        val errors = mutableListOf<UtError>()
        var missingLines: Set<Int>? = null
        val coveredLines = mutableSetOf<Int>()
        var generated = 0

        var stopFuzzing = false

        runBlockingWithCancellationPredicate(isCancelled) {
            annotationSequence.takeWhile { !stopFuzzing }.forEach { annotations ->
                if (isCancelled())
                    return@runBlockingWithCancellationPredicate

                logger.debug(
                    "Found annotations: ${
                        annotations.map { "${it.key}: ${it.value}" }.joinToString(" ")
                    }"
                )

                val engine = PythonEngine(
                    method,
                    directoriesForSysPath,
                    curModule,
                    pythonPath,
                    argInfoCollector.getConstants(),
                    annotations,
                    timeoutForRun,
                    coveredLines,
                )

                var coverageLimit = 10

                engine.fuzzing().takeWhile { coverageLimit > 0 }.collect {
                    val coveredBefore = coveredLines.size

                    if (isCancelled())
                        return@collect

                    generated += 1
                    when (it) {
                        is UtExecution -> {
                            logger.debug("Added execution")
                            executions += it
                            missingLines = updateCoverage(it, coveredLines, missingLines)
                        }

                        is UtError -> {
                            logger.debug("Failed evaluation. Reason: ${it.description}")
                            errors += it
                        }
                    }
                    val coveredAfter = coveredLines.size

                    if (coveredAfter == coveredBefore)
                        coverageLimit -= 1

                    if (withMinimization && missingLines?.isEmpty() == true) //&& generated % CHUNK_SIZE == 0)
                        stopFuzzing = true
                    return@collect
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
        initialArgumentTypes: List<NormalizedPythonAnnotation>,
        argInfoCollector: ArgInfoCollector,
        isCancelled: () -> Boolean
    ): Sequence<Map<String, NormalizedPythonAnnotation>> {

        val existingAnnotations = mutableMapOf<String, NormalizedPythonAnnotation>()
        initialArgumentTypes.forEachIndexed { index, classId ->
            if (classId != pythonAnyClassId)
                existingAnnotations[method.arguments[index].name] = classId
        }

        return findAnnotations(
            argInfoCollector,
            method,
            existingAnnotations,
            curModule,
            directoriesForSysPath,
            pythonPath,
            isCancelled,
            storageForMypyMessages
        )
    }
}