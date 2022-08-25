package org.utbot.python

import mu.KotlinLogging
import org.utbot.framework.minimization.minimizeExecutions
import org.utbot.framework.plugin.api.*
import org.utbot.python.code.ArgInfoCollector
import org.utbot.python.typing.AnnotationFinder.findAnnotations
import org.utbot.python.typing.MypyAnnotations
import org.utbot.python.utils.AnnotationNormalizer.annotationFromProjectToClassId

private val logger = KotlinLogging.logger {}

object PythonTestCaseGenerator {
    private var withMinimization: Boolean = true
    private lateinit var directoriesForSysPath: Set<String>
    private lateinit var curModule: String
    private lateinit var pythonPath: String
    private lateinit var fileOfMethod: String
    private lateinit var isCancelled: () -> Boolean
    private var timeoutForRun: Long = 0

    fun init(
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        pythonPath: String,
        fileOfMethod: String,
        timeoutForRun: Long,
        withMinimization: Boolean = true,
        isCancelled: () -> Boolean
    ) {
        this.directoriesForSysPath = directoriesForSysPath
        this.curModule = moduleToImport
        this.pythonPath = pythonPath
        this.fileOfMethod = fileOfMethod
        this.withMinimization = withMinimization
        this.isCancelled = isCancelled
        this.timeoutForRun = timeoutForRun
    }

    private val storageForMypyMessages: MutableList<MypyAnnotations.MypyReportLine> = mutableListOf()

    fun generate(method: PythonMethod): PythonTestSet {
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
            initialArgumentTypes[0] = NormalizedPythonAnnotation(method.containingPythonClassId!!.name)
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

        run breaking@ {
            annotationSequence.forEach { annotations ->
                if (isCancelled())
                    return@breaking

                logger.debug("Found annotations: ${
                    annotations.map { "${it.key}: ${it.value}" }.joinToString(" ")
                }")

                val engine = PythonEngine(
                    method,
                    directoriesForSysPath,
                    curModule,
                    pythonPath,
                    argInfoCollector.getConstants(),
                    annotations,
                    timeoutForRun,
                    coveredLines
                )

                engine.fuzzing().forEach {
                    if (isCancelled())
                        return@breaking
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
                    if (withMinimization && missingLines?.isEmpty() == true && generated % CHUNK_SIZE == 0)
                        return@breaking
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
    private fun updateCoverage(execution: UtExecution, coveredLines: MutableSet<Int>, missingLines: Set<Int>?): Set<Int> {
        execution.coverage?.coveredInstructions?.map { instr -> coveredLines.add(instr.lineNumber) }
        val curMissing =
            (execution.coverage as? PythonCoverage)
                ?.missedInstructions
                ?.map { x -> x.lineNumber } ?.toSet()
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